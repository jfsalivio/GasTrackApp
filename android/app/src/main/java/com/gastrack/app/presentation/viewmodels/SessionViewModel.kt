package com.gastrack.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastrack.app.core.common.Resource
import com.gastrack.app.core.network.ConnectivityMonitor
import com.gastrack.app.domain.entities.SessionUser
import com.gastrack.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SessionState(val loading: Boolean = true, val user: SessionUser? = null, val error: String? = null)
@HiltViewModel class SessionViewModel @Inject constructor(private val auth: AuthRepository, connectivity: ConnectivityMonitor) : ViewModel() {
    private val mutable = MutableStateFlow(SessionState())
    private var explicitLogout = false
    val state = mutable.asStateFlow()
    val online = connectivity.online.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    init {
        viewModelScope.launch {
            auth.hasSession.collectLatest { present ->
                if (!present) {
                    mutable.value = SessionState(loading = false, error = if (mutable.value.user != null && !explicitLogout) "Your session expired. Please sign in again." else null)
                    explicitLogout = false
                }
                else refresh()
            }
        }
    }
    fun refreshSession() { viewModelScope.launch { refresh() } }
    private suspend fun refresh() {
        if (!auth.hasSession.first()) return
        when (val result = auth.currentUser()) {
            is Resource.Success -> mutable.value = SessionState(false, result.data)
            is Resource.Error -> mutable.value = mutable.value.copy(loading = false, error = result.message)
            Resource.Loading -> Unit
        }
    }
    fun logout() { viewModelScope.launch {
        explicitLogout = true
        when (val result = auth.logout()) {
            is Resource.Error -> if (result.status != 401) { explicitLogout = false; mutable.update { it.copy(error = result.message) } }
            else -> Unit
        }
    } }
}
