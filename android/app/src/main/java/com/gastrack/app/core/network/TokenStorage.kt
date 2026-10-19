package com.gastrack.app.core.network

import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    val token: Flow<String?>
    suspend fun save(token: String)
    suspend fun clearIfCurrent(expected: String?)
}
