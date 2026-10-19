package com.gastrack.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.gastrack.app.data.repository.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/** Uses the bundled React frontend to keep Android and web workflows together. */
open class WebAppActivity : ComponentActivity() {
    @Inject lateinit var sessions: SessionStore
    private lateinit var browser: WebView
    private var upload: ValueCallback<Array<Uri>>? = null
    private var exportBytes: ByteArray? = null
    private var printBrowser: WebView? = null
    private val origin = "https://appassets.androidplatform.net"
    private var migrationToken: String? = null
    private val chooseFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        upload?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        upload = null
    }
    private val saveFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val bytes = exportBytes
        exportBytes = null
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null && bytes != null) lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { runCatching {
                requireNotNull(contentResolver.openOutputStream(uri)).use { it.write(bytes) }
            }.isSuccess }
            Toast.makeText(this@WebAppActivity, if (success) "File saved" else "Could not save file", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val frame = FrameLayout(this)
        ViewCompat.setOnApplyWindowInsetsListener(frame) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        browser = WebView(this)
        frame.addView(browser, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
        browser.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = if (BuildConfig.DEBUG) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        browser.addJavascriptInterface(PlatformBridge(), "GasTrackAndroid")
        browser.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.url.scheme != "https" || request.url.host != "appassets.androidplatform.net") return null
                val path = request.url.path.orEmpty().trimStart('/').ifEmpty { "index.html" }
                if (path.split('/').any { it == ".." }) return missingAsset()
                return try {
                    if (path.startsWith("backend/uploads/")) {
                        // Public images use a same-origin URL: WebView otherwise
                        // upgrades HTTP image URLs to HTTPS on a debug backend.
                        val connection = URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.removePrefix("backend/")).openConnection() as HttpURLConnection
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000
                        connection.instanceFollowRedirects = false
                        if (connection.responseCode != 200 || !connection.contentType.orEmpty().startsWith("image/")) {
                            connection.disconnect()
                            missingAsset()
                        } else WebResourceResponse(connection.contentType.substringBefore(';'), null, object : FilterInputStream(connection.inputStream) {
                            override fun close() { try { super.close() } finally { connection.disconnect() } }
                        })
                    } else if (path == "index.html") {
                        val configuration = "<script>window.__GASTRACK_API_BASE_URL__=${JSONObject.quote(BuildConfig.API_BASE_URL.trimEnd('/'))};" +
                            (migrationToken?.let { "if(!localStorage.getItem('token'))localStorage.setItem('token',${JSONObject.quote(it)});" } ?: "") + "</script>"
                        val html = assets.open("web/index.html").bufferedReader().use { it.readText() }.replace("<head>", "<head>$configuration")
                        WebResourceResponse("text/html", "UTF-8", 200, "OK", mapOf(
                            "Content-Security-Policy" to "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: http: https:; connect-src 'self' ${BuildConfig.API_BASE_URL.trimEnd('/')}; font-src 'self' data:; frame-src 'self' blob: data:; object-src blob: data:; base-uri 'self'; form-action 'none'",
                            "Cache-Control" to "no-store"
                        ), ByteArrayInputStream(html.toByteArray()))
                    } else {
                        val mime = when (path.substringAfterLast('.').lowercase()) {
                            "js", "mjs" -> "text/javascript"
                            "css" -> "text/css"
                            "svg" -> "image/svg+xml"
                            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substringAfterLast('.')) ?: "application/octet-stream"
                        }
                        WebResourceResponse(mime, null, assets.open("web/$path"))
                    }
                } catch (_: Exception) { missingAsset() }
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.scheme == "https" && request.url.host == "appassets.androidplatform.net") return false
                if (request.isForMainFrame && request.url.scheme in listOf("https", "http", "mailto", "tel"))
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                return true
            }
            override fun onPageFinished(view: WebView, url: String) {
                migrationToken?.let { previous -> migrationToken = null; lifecycleScope.launch { sessions.clearIfCurrent(previous) } }
            }
        }
        browser.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, parameters: FileChooserParams): Boolean {
                upload?.onReceiveValue(null)
                upload = callback
                runCatching { chooseFile.launch(parameters.createIntent()) }.onFailure { upload?.onReceiveValue(null); upload = null }
                return true
            }
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                AlertDialog.Builder(this@WebAppActivity).setMessage(message).setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setOnCancelListener { result.cancel() }.show()
                return true
            }
            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                AlertDialog.Builder(this@WebAppActivity).setMessage(message).setPositiveButton("Continue") { _, _ -> result.confirm() }
                    .setNegativeButton("Cancel") { _, _ -> result.cancel() }.setOnCancelListener { result.cancel() }.show()
                return true
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                browser.evaluateJavascript("document.querySelector('.mobile-menu-open') !== null") { open ->
                    if (open == "true") browser.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))", null)
                    else if (browser.canGoBack()) browser.goBack()
                    else { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
                }
            }
        })
        lifecycleScope.launch {
            migrationToken = sessions.token.first()
            val reset = intent?.data?.takeIf { it.scheme == "gastrack" && it.host == "reset-password" }?.getQueryParameter("token")
            browser.loadUrl("$origin/index.html" + (reset?.let { "#/reset-password?token=${Uri.encode(it)}" } ?: ""))
        }
    }

    private fun missingAsset() = WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), ByteArrayInputStream("Asset not found".toByteArray()))

    private inner class PlatformBridge {
        @JavascriptInterface fun saveFile(name: String, mime: String, base64: String) {
            if (base64.length > 32 * 1024 * 1024) return
            val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return
            runOnUiThread {
                if (!browser.url.orEmpty().startsWith("$origin/")) return@runOnUiThread
                if (exportBytes != null) { Toast.makeText(this@WebAppActivity, "Finish saving the current file first", Toast.LENGTH_SHORT).show(); return@runOnUiThread }
                exportBytes = bytes
                val fileName = name.substringAfterLast('/').substringAfterLast('\\').take(128).ifBlank { "gastrack-download" }
                saveFile.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(mime.takeIf { it.matches(Regex("[\\w.+-]+/[\\w.+-]+")) } ?: "application/octet-stream")
                    .putExtra(Intent.EXTRA_TITLE, fileName))
            }
        }
        @JavascriptInterface fun printReceipt(html: String) {
            if (html.length > 4 * 1024 * 1024) return
            runOnUiThread {
                if (!browser.url.orEmpty().startsWith("$origin/")) return@runOnUiThread
                printBrowser?.destroy()
                printBrowser = WebView(this@WebAppActivity).apply {
                    settings.mixedContentMode = if (BuildConfig.DEBUG) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.allowFileAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            (getSystemService(PRINT_SERVICE) as PrintManager).print("GasTrack receipt", view.createPrintDocumentAdapter("GasTrack receipt"), PrintAttributes.Builder().build())
                        }
                    }
                    loadDataWithBaseURL("$origin/", html, "text/html", "UTF-8", null)
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.takeIf { it.scheme == "gastrack" && it.host == "reset-password" }?.getQueryParameter("token")?.let {
            browser.loadUrl("$origin/index.html#/reset-password?token=${Uri.encode(it)}")
        }
    }
    override fun onDestroy() {
        upload?.onReceiveValue(null)
        browser.removeJavascriptInterface("GasTrackAndroid")
        browser.destroy()
        printBrowser?.destroy()
        super.onDestroy()
    }
}
