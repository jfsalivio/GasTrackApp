package com.gastrack.app.core.network

import com.gastrack.app.data.repository.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.IOException
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val sessions: TokenStorage) : Interceptor {
    private val publicPaths = setOf("health", "auth/login", "auth/register", "auth/forgot-password", "auth/reset-password", "auth/reset-password/validate")
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val protected = request.url.encodedPath.trim('/') !in publicPaths
        // Interceptors run on OkHttp workers, never the Compose/main thread.
        val token = if (protected) runBlocking { sessions.token.first() } else null
        if (protected && (token.isNullOrBlank() || token in setOf("null", "undefined"))) {
            throw MissingSessionException()
        }
        val sent = request.newBuilder().apply { if (protected) header("Authorization", "Bearer ${token!!.trim()}") }.build()
        val response = chain.proceed(sent)
        if (protected && response.code == 401) runBlocking { sessions.clearIfCurrent(token) }
        return response
    }
}
class MissingSessionException : IOException("Your session is missing or expired. Please sign in again.")
