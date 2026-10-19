package com.gastrack.app.domain.repository

import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.*

interface ManagementRepository {
    suspend fun records(module: String, query: String = ""): Resource<List<ManagementRecord>>
    suspend fun settings(): Resource<Map<String, String>>
    suspend fun saveProfile(fields: Map<String, String>): Resource<String>
    suspend fun saveSettings(section: String, fields: Map<String, String>): Resource<String>
    suspend fun saveLogo(bytes: ByteArray, mimeType: String): Resource<String>
    suspend fun warehouses(): Resource<List<Pair<String, String>>>
    suspend fun checkoutOptions(): Resource<CheckoutOptions>
    suspend fun checkout(command: Checkout): Resource<String>
    suspend fun saveCustomer(id: String?, fields: Map<String, String>): Resource<String>
    suspend fun adjustStock(id: String, quantity: Int, remarks: String): Resource<String>
    suspend fun generateReport(id: String): Resource<DownloadFile>
    suspend fun export(dataType: String, format: String): Resource<DownloadFile>
    suspend fun importCsv(inventory: Boolean, text: String, name: String): Resource<String>
    suspend fun purchaseAction(id: String, action: String): Resource<String>
    suspend fun editor(module: String, id: String?): Resource<EditorDefinition>
    suspend fun saveRecord(module: String, id: String?, fields: Map<String, String>): Resource<String>
    suspend fun deleteRecord(module: String, id: String): Resource<String>
}
