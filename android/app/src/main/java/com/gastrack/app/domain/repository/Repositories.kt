package com.gastrack.app.domain.repository

import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val hasSession: Flow<Boolean>
    suspend fun login(email: String, password: String): Resource<SessionUser>
    suspend fun register(company: String, branch: String, address: String, first: String, last: String, email: String, password: String, confirm: String): Resource<SessionUser>
    suspend fun currentUser(): Resource<SessionUser>
    suspend fun forgot(email: String): Resource<String>
    suspend fun validateReset(token: String): Resource<Boolean>
    suspend fun reset(token: String, password: String): Resource<String>
    suspend fun logout(): Resource<String>
}
interface OperationsRepository {
    suspend fun dashboard(): Resource<Dashboard>
    suspend fun products(search: String? = null, warehouseId: String? = null): Resource<List<Product>>
    suspend fun inventory(search: String? = null): Resource<List<Stock>>
    suspend fun customers(search: String? = null): Resource<List<Customer>>
    suspend fun orders(search: String? = null): Resource<List<Order>>
    suspend fun pickups(): Resource<List<Order>>
    suspend fun deliveries(): Resource<List<Delivery>>
    suspend fun recommendations(): Resource<List<Recommendation>>
    suspend fun generateRecommendations(): Resource<List<Recommendation>>
    suspend fun rescoreRecommendations(): Resource<List<Recommendation>>
    suspend fun dispatchDelivery(id: String, requestId: String): Resource<String>
    suspend fun failDelivery(id: String, reason: String, requestId: String): Resource<String>
    suspend fun updatePickup(id: String, status: String): Resource<String>
    suspend fun customizeRecommendation(id: String, quantity: Int): Resource<String>
}
