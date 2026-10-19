package com.gastrack.app.data.repository

import com.gastrack.app.core.network.networkCall
import com.gastrack.app.data.api.GasTrackApi
import com.gastrack.app.data.models.*
import com.gastrack.app.domain.entities.*
import com.gastrack.app.domain.repository.FulfillmentRepository
import kotlinx.serialization.json.*
import javax.inject.Inject

class FulfillmentRepositoryImpl @Inject constructor(private val api: GasTrackApi, private val json: Json) : FulfillmentRepository {
    override suspend fun returnOptions(id: String, delivery: Boolean) = networkCall(json) {
        val data = if (delivery) api.deliveryReturns(id) else api.pickupClaimOptions(id)
        fun options(key: String) = data.getValue(key).jsonArray.map { it.jsonObject.let { row -> row.getValue("id").jsonPrimitive.content to row.getValue("name").jsonPrimitive.content } }
        ReturnOptions(options("warehouses"), options("products"))
    }
    override suspend fun complete(command: Completion) = networkCall(json) {
        require(!command.returned || (!command.warehouse.isNullOrBlank() && command.cylinders.isNotEmpty())) { "Select a warehouse and at least one returned cylinder." }
        require(command.cylinders.values.all { it > 0 }) { "Cylinder quantities must be positive whole numbers." }
        val body = CompletionRequest(command.requestId, command.returned, if (command.returned) command.cylinders.map { ReturnedCylinder(it.key, it.value) } else emptyList(), if (command.returned) command.warehouse else null, command.collected?.let { Decimal(it.toPlainString()) })
        if (command.delivery) api.completeDelivery(command.id, body) else api.pickupClaim(command.id, body)
        if (command.delivery) "Delivery completed." else "Pickup claimed."
    }
}
