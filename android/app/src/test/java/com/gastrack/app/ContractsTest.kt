package com.gastrack.app

import com.gastrack.app.data.models.*
import com.gastrack.app.domain.usecase.ValidateRegistration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ContractsTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    @Test fun `decimal strings and JSON numbers preserve financial precision`() {
        val text = json.decodeFromString<Decimal>("\"9999999999.99\"")
        val number = json.decodeFromString<Decimal>("9999999999.99")
        assertEquals(text.toBigDecimal(), number.toBigDecimal())
        assertEquals("9999999999.99", json.encodeToString(text))
    }
    @Test fun `customer type uses the exact backend field`() {
        val body = json.parseToJsonElement(json.encodeToString(CustomerRequest("Commercial", "09123456789", "Pasig", "Ana", "Reyes", "Gas Co"))).jsonObject
        assertEquals("Commercial", body.getValue("customerType").jsonPrimitive.content)
        assertFalse(body.containsKey("type"))
    }
    @Test fun `actor response maps PascalCase IDs and account permissions`() {
        val actor = json.decodeFromString<ActorDto>("""{"UserID":"USR-001","FirstName":"Ana","LastName":"Reyes","Email":"ana@example.com","RoleName":"Drivers","Status":"Active","ModuleAccess":{"orders":true,"pos":false}}""")
        assertTrue(actor.toDomain().isDriver)
        assertEquals(setOf("orders"), actor.toDomain().modules)
    }
    @Test fun `ML recommendation accepts decimal confidence and string IDs`() {
        val dto = json.decodeFromString<RestockDto>("""{"restockId":"R-001","productId":"P-ABC12345","productName":"Gasul 11 kg","unit":"kg","costPrice":"900.00","supplierId":"SUP-001","supplierName":"Supplier","currentStock":5,"reorderLevel":10,"suggestedQty":25,"confidence":"87.3","status":"Pending","forecastDate":"2026-10-19","priority":"Critical"}""")
        assertEquals("87.3", dto.toDomain().confidence.toString())
        assertEquals(25, dto.toDomain().quantity)
    }
    @Test fun `registration enforces matching passwords and required fields`() {
        val errors = ValidateRegistration()(mapOf("password" to "12345678", "confirm" to "different"))
        assertTrue("confirm" in errors)
        assertTrue("company" in errors)
        assertFalse("password" in errors)
    }
}
