package com.gastrack.app.domain.repository

import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.*

interface FulfillmentRepository {
    suspend fun returnOptions(id: String, delivery: Boolean): Resource<ReturnOptions>
    suspend fun complete(command: Completion): Resource<String>
}
