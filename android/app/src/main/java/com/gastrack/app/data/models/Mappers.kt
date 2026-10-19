package com.gastrack.app.data.models

import com.gastrack.app.domain.entities.*

fun UserDto.toDomain() = SessionUser(id, firstName, lastName, email, role, moduleAccess.filterValues { it }.keys)
fun ActorDto.toDomain() = SessionUser(id, firstName, lastName, email, role, moduleAccess.filterValues { it }.keys)
fun ProductDto.toDomain() = Product(productId, name, category, unitPrice.toBigDecimal(), stock.toBigDecimal(), productImage(imageUrl, name), status)
fun InventoryDto.toDomain() = Stock(inventoryId, productId, productName, warehouseId, warehouse, currentStock.toBigDecimal(), reorderLimit.toBigDecimal(), status)
fun CustomerDto.toDomain() = Customer(id, name, type, phone, address)
fun OrderDto.toDomain() = Order(orderId, id, customerName, type, status, totalAmount.toBigDecimal(), pickupStatus)
fun DeliveryDto.toDomain() = Delivery(deliveryId, id, customerName, deliveryStatus, deliveryAddress, amountToCollect.toBigDecimal(), attempts, maxAttempts, canSettle, paymentMethod == "Cash on Delivery")
fun RestockDto.toDomain() = Recommendation(restockId, productId, productName, supplierName, currentStock.toBigDecimal(), suggestedQty.toBigDecimal().intValueExact(), confidence?.toBigDecimal(), priority)
fun DashboardDto.toDomain() = Dashboard(salesPerformance.toBigDecimal(), salesPerformanceChangeLabel, transactions, stockAttention, bestSeller, salesTrend.map { it.day to it.value.toBigDecimal() }, activityLog.map { it.text ?: it.description ?: it.action })
