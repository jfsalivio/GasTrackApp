package com.gastrack.app

import com.gastrack.app.core.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test

private class MemoryTokens(initial: String?) : TokenStorage {
    override val token = MutableStateFlow(initial)
    override suspend fun save(token: String) { this.token.value = token }
    override suspend fun clearIfCurrent(expected: String?) { token.compareAndSet(expected, null) }
}
class AuthInterceptorTest {
    @Test fun `protected request attaches bearer and expires matching session on 401`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"Expired\"}"))
            val tokens = MemoryTokens("old-token")
            val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens)).build()
            client.newCall(Request.Builder().url(server.url("/auth/me")).build()).execute().use { assertEquals(401, it.code) }
            assertEquals("Bearer old-token", server.takeRequest().getHeader("Authorization"))
            assertNull(tokens.token.value)
        }
    }
    @Test fun `public login never sends a bearer token`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{}"))
            val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(MemoryTokens("secret"))).build()
            client.newCall(Request.Builder().url(server.url("/auth/login")).build()).execute().close()
            assertNull(server.takeRequest().getHeader("Authorization"))
        }
    }
    @Test fun `late expired response cannot clear a newer session`() {
        MockWebServer().use { server ->
            val tokens = MemoryTokens("old-token")
            server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    runBlocking { tokens.save("new-token") }
                    return MockResponse().setResponseCode(401)
                }
            }
            val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens)).build()
            client.newCall(Request.Builder().url(server.url("/inventory")).build()).execute().close()
            assertEquals("new-token", tokens.token.value)
        }
    }
    @Test fun `missing session rejects a protected request before dispatch`() {
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(MemoryTokens(null))).build()
        assertThrows(MissingSessionException::class.java) {
            client.newCall(Request.Builder().url("http://localhost:1/inventory").build()).execute()
        }
    }
}
