package com.gastrack.app.presentation.viewmodels

import androidx.lifecycle.*
import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.*
import com.gastrack.app.domain.repository.FulfillmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompletionState(val loading: Boolean = true, val busy: Boolean = false, val returned: Boolean? = null, val warehouse: String = "", val quantities: Map<String, String> = emptyMap(), val collected: String = "", val options: ReturnOptions? = null, val error: String? = null)
@HiltViewModel class CompletionViewModel @Inject constructor(private val repository: FulfillmentRepository, saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(CompletionState())
    val state = mutable.asStateFlow()
    private val channel = Channel<String>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private val requestId = saved.get<String>("requestId") ?: java.util.UUID.randomUUID().toString().also { saved["requestId"] = it }
    fun load(id: String, delivery: Boolean) { if (state.value.options != null) return; viewModelScope.launch {
        when (val result = repository.returnOptions(id, delivery)) {
            is Resource.Success -> mutable.update { it.copy(loading = false, options = result.data, warehouse = result.data.warehouses.singleOrNull()?.first.orEmpty()) }
            is Resource.Error -> mutable.update { it.copy(loading = false, error = result.message) }
            Resource.Loading -> Unit
        }
    } }
    fun returned(value: Boolean) { mutable.update { it.copy(returned = value) } }
    fun warehouse(value: String) { mutable.update { it.copy(warehouse = value) } }
    fun quantity(id: String, value: String) { mutable.update { it.copy(quantities = it.quantities + (id to value.filter(Char::isDigit))) } }
    fun collected(value: String) { mutable.update { it.copy(collected = value) } }
    fun complete(id: String, delivery: Boolean, cod: Boolean) {
        val s = state.value
        if (s.busy) return
        if (s.returned == null) { mutable.update { it.copy(error = "Answer Empty Cylinder Returned? Yes or No.") }; return }
        if (cod && s.collected.toBigDecimalOrNull() == null) { mutable.update { it.copy(error = "Enter amount collected.") }; return }
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.complete(Completion(id, delivery, requestId, s.returned, s.warehouse, s.quantities.mapNotNull { (id, value) -> value.toIntOrNull()?.takeIf { it > 0 }?.let { id to it } }.toMap(), if (cod) s.collected.toBigDecimalOrNull() else null))) {
                is Resource.Success -> channel.send(result.data)
                is Resource.Error -> mutable.update { it.copy(error = result.message) }
                Resource.Loading -> Unit
            }
            mutable.update { it.copy(busy = false) }
        }
    }
}
