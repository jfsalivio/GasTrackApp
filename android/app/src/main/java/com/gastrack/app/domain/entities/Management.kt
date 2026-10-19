package com.gastrack.app.domain.entities

data class ManagementRecord(val id: String, val title: String, val subtitle: String, val details: Map<String, String> = emptyMap())
data class DownloadFile(val name: String, val mimeType: String, val bytes: ByteArray)
data class CartItem(val product: Product, val quantity: Int)
data class Checkout(val items: List<CartItem>, val warehouseId: String, val paymentMethod: String, val collected: java.math.BigDecimal, val reference: String?, val discount: java.math.BigDecimal = java.math.BigDecimal.ZERO, val type: String = "Walk-in", val customerId: String? = null, val pickupDate: String? = null, val pickupTime: String? = null, val deliveryAddress: String? = null, val instructions: String? = null, val riderId: String? = null, val vehicleType: String? = null, val returns: Map<String, Int> = emptyMap())
data class CheckoutOptions(val customers: List<Pair<String, String>>, val drivers: List<Pair<String, String>>, val vehicles: List<String>)
