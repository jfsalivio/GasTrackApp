package com.gastrack.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.gastrack.app.core.network.TokenStorage

private val Context.sessionDataStore by preferencesDataStore("session")

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) : TokenStorage {
    private val store = context.sessionDataStore
    private val tokenKey = stringPreferencesKey("token")
    override val token: Flow<String?> = store.data.map { it[tokenKey] }
    override suspend fun save(token: String) {
        require(token.isNotBlank() && token !in setOf("null", "undefined"))
        store.edit { it[tokenKey] = token }
    }
    override suspend fun clearIfCurrent(expected: String?) {
        store.edit { if (it[tokenKey] == expected) it.remove(tokenKey) }
    }
}
