package com.gastrack.app.domain.entities

import java.math.BigDecimal

data class SessionUser(val id: String, val firstName: String, val lastName: String, val email: String, val role: String, val modules: Set<String>) {
    val isDriver get() = role.lowercase().trim() in setOf("driver", "drivers")
}
data class Product(val id: String, val name: String, val category: String, val price: BigDecimal, val stock: BigDecimal, val imageUrl: String?, val status: String)
data class Stock(val id: String, val productId: String, val name: String, val warehouseId: String, val warehouse: String, val quantity: BigDecimal, val reorderLevel: BigDecimal, val status: String)
data class Customer(val id: String, val name: String, val type: String, val phone: String?, val address: String?)
data class Order(val id: String, val number: String, val customer: String?, val type: String, val status: String, val total: BigDecimal, val pickupStatus: String?)
data class Delivery(val id: String, val orderNumber: String, val customer: String?, val status: String, val address: String?, val amountToCollect: BigDecimal, val attempts: Int, val maxAttempts: Int, val canSettle: Boolean, val cashOnDelivery: Boolean)
data class Recommendation(val id: String, val productId: String, val name: String, val supplier: String, val stock: BigDecimal, val quantity: Int, val confidence: BigDecimal?, val priority: String)
data class Dashboard(val sales: BigDecimal, val salesChange: String, val transactions: Int, val stockAttention: Int, val bestSeller: String, val trend: List<Pair<String, BigDecimal>>, val activity: List<String>)
