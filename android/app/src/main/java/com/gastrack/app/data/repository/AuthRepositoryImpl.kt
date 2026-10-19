package com.gastrack.app.data.repository

import com.gastrack.app.core.common.Resource
import com.gastrack.app.core.network.networkCall
import com.gastrack.app.data.api.GasTrackApi
import com.gastrack.app.data.models.*
import com.gastrack.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class AuthRepositoryImpl @Inject constructor(private val api: GasTrackApi, private val store: SessionStore, private val json: Json) : AuthRepository {
    override val hasSession = store.token.map { !it.isNullOrBlank() }.distinctUntilChanged()
    override suspend fun login(email: String, password: String) = networkCall(json) {
        val response = api.login(LoginRequest(email.trim(), password))
        store.save(response.token); response.user.toDomain()
    }
    override suspend fun register(company: String, branch: String, address: String, first: String, last: String, email: String, password: String, confirm: String) = networkCall(json) {
        val response = api.register(RegisterRequest(company, branch, address, first, last, email, password, confirm))
        store.save(response.token); response.user.toDomain()
    }
    override suspend fun currentUser() = networkCall(json) {
        val token = store.token.first()
        val actor = api.me().toDomain()
        if (token != store.token.first()) throw com.gastrack.app.core.network.MissingSessionException()
        actor
    }
    override suspend fun forgot(email: String) = networkCall(json) { api.forgot(EmailRequest(email.trim())).message }
    override suspend fun validateReset(token: String) = networkCall(json) { api.validateReset(token).valid }
    override suspend fun reset(token: String, password: String) = networkCall(json) { api.reset(ResetPasswordRequest(token, password)).message }
    override suspend fun logout(): Resource<String> {
        val token = store.token.first()
        val result = networkCall(json) { api.logout().message }
        if (result is Resource.Success || result is Resource.Error && result.status == 401) store.clearIfCurrent(token)
        return result
    }
}
