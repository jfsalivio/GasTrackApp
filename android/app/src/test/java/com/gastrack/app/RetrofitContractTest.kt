package com.gastrack.app

import com.gastrack.app.data.api.GasTrackApi
import com.gastrack.app.data.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RetrofitContractTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    private fun api(server: MockWebServer) = Retrofit.Builder().baseUrl(server.url("/"))
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .validateEagerly(true).build().create(GasTrackApi::class.java)
    @Test fun `every Retrofit declaration can be parsed eagerly`() {
        MockWebServer().use { server -> assertNotNull(api(server)) }
    }
    @Test fun `inventory filters preserve query names and omit missing parameters`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("[]"))
            api(server).inventory(warehouseId = "WH-001", search = "Gasul 11")
            val request = server.takeRequest()
            assertEquals("/inventory", request.requestUrl!!.encodedPath)
            assertEquals("WH-001", request.requestUrl!!.queryParameter("warehouseId"))
            assertEquals("Gasul 11", request.requestUrl!!.queryParameter("search"))
            assertNull(request.requestUrl!!.queryParameter("status"))
        }
    }
    @Test fun `delivery completion sends numeric money and stable request ID`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{\"status\":\"Delivered\"}"))
            api(server).completeDelivery("D-001", CompletionRequest("request-12345678", false, emptyList(), amountCollected = Decimal("1200.50")))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/deliveries/D-001/complete", request.path)
            val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("request-12345678", body.getValue("requestId").jsonPrimitive.content)
            assertFalse(body.getValue("amountCollected").jsonPrimitive.isString)
            assertFalse(body.containsKey("warehouseId"))
        }
    }
}
