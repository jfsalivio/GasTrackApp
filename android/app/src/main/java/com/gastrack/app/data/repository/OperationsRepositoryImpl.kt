package com.gastrack.app.data.repository

import com.gastrack.app.core.network.networkCall
import com.gastrack.app.data.api.GasTrackApi
import com.gastrack.app.data.models.*
import com.gastrack.app.domain.repository.OperationsRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class OperationsRepositoryImpl @Inject constructor(private val api: GasTrackApi, private val json: Json) : OperationsRepository {
    override suspend fun dashboard() = networkCall(json) { api.dashboard().toDomain() }
    override suspend fun products(search: String?, warehouseId: String?) = networkCall(json) { api.products(search, warehouseId = warehouseId).map { it.toDomain() } }
    override suspend fun inventory(search: String?) = networkCall(json) { api.inventory(search = search).map { it.toDomain() } }
    override suspend fun customers(search: String?) = networkCall(json) { api.customers(search).map { it.toDomain() } }
    override suspend fun orders(search: String?) = networkCall(json) { api.orders(search).map { it.toDomain() } }
    override suspend fun pickups() = networkCall(json) { api.pickups().map { it.toDomain() } }
    override suspend fun deliveries() = networkCall(json) { api.deliveries().map { it.toDomain() } }
    override suspend fun recommendations() = networkCall(json) { api.recommendations().map { it.toDomain() } }
    override suspend fun generateRecommendations() = networkCall(json) {
        api.generateRecommendations(); api.recommendations().map { it.toDomain() }
    }
    override suspend fun rescoreRecommendations() = networkCall(json) {
        api.rescoreRecommendations(); api.recommendations().map { it.toDomain() }
    }
    override suspend fun dispatchDelivery(id: String, requestId: String) = networkCall(json) { api.deliveryAction(id, "dispatch", DeliveryActionRequest(requestId)); "Delivery dispatched." }
    override suspend fun failDelivery(id: String, reason: String, requestId: String) = networkCall(json) { require(reason.isNotBlank() && reason.length <= 2000) { "Enter failure details (up to 2000 characters)." }; api.deliveryAction(id, "fail", DeliveryActionRequest(requestId, "Other (Specify in Notes)", reason)); "Attempt recorded." }
    override suspend fun updatePickup(id: String, status: String) = networkCall(json) { api.pickupStatus(id, PickupStatusRequest(status)); "Pickup updated." }
    override suspend fun customizeRecommendation(id: String, quantity: Int) = networkCall(json) { require(quantity > 0) { "Quantity must be positive." }; api.updateRecommendation(id, RestockUpdateRequest(quantity)).message }
}
