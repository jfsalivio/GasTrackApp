package com.gastrack.app.presentation.viewmodels

import androidx.lifecycle.*
import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.*
import com.gastrack.app.domain.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListState<T>(val loading: Boolean = true, val items: List<T> = emptyList(), val query: String = "", val error: String? = null, val busy: Boolean = false)
abstract class ListViewModel<T> : ViewModel() {
    protected val mutable = MutableStateFlow(ListState<T>())
    val state = mutable.asStateFlow()
    private val channel = Channel<String>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var loadJob: Job? = null
    protected abstract suspend fun fetch(query: String): Resource<List<T>>
    fun editQuery(query: String) { mutable.update { it.copy(query = query) } }
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            when (val result = fetch(state.value.query)) {
                is Resource.Success -> mutable.update { it.copy(loading = false, items = result.data) }
                is Resource.Error -> mutable.update { it.copy(loading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
    protected fun action(block: suspend () -> Resource<String>) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch { try {
            when (val result = block()) {
                is Resource.Success -> { channel.send(result.data); refresh() }
                is Resource.Error -> channel.send(result.message)
                Resource.Loading -> Unit
            }
        } finally { mutable.update { it.copy(busy = false) } } }
    }
}
@HiltViewModel class ProductsViewModel @Inject constructor(private val repository: OperationsRepository) : ListViewModel<Product>() {
    override suspend fun fetch(query: String) = repository.products(query.ifBlank { null })
    init { refresh() }
}
@HiltViewModel class InventoryViewModel @Inject constructor(private val repository: OperationsRepository, private val management: com.gastrack.app.domain.repository.ManagementRepository) : ListViewModel<Stock>() {
    override suspend fun fetch(query: String) = repository.inventory(query.ifBlank { null })
    fun adjust(id: String, quantity: Int, remarks: String) = action { management.adjustStock(id, quantity, remarks) }
    init { refresh() }
}
@HiltViewModel class CustomersViewModel @Inject constructor(private val repository: OperationsRepository, private val management: com.gastrack.app.domain.repository.ManagementRepository) : ListViewModel<Customer>() {
    override suspend fun fetch(query: String) = repository.customers(query.ifBlank { null })
    fun create(fields: Map<String, String>) = action { management.saveCustomer(null, fields) }
    init { refresh() }
}
@HiltViewModel class OrdersViewModel @Inject constructor(private val repository: OperationsRepository) : ListViewModel<Order>() {
    override suspend fun fetch(query: String) = repository.orders(query.ifBlank { null })
    init { refresh() }
}
@HiltViewModel class PickupsViewModel @Inject constructor(private val repository: OperationsRepository) : ListViewModel<Order>() {
    override suspend fun fetch(query: String) = repository.pickups()
    fun changeStatus(id: String, status: String) = action { repository.updatePickup(id, status) }
    init { refresh() }
}
@HiltViewModel class DeliveriesViewModel @Inject constructor(private val repository: OperationsRepository) : ListViewModel<Delivery>() {
    override suspend fun fetch(query: String) = repository.deliveries()
    // Request IDs remain attached to a logical attempt across retries.
    private val requestIds = mutableMapOf<String, String>()
    fun dispatch(id: String) = action { repository.dispatchDelivery(id, requestIds.getOrPut("dispatch:$id") { java.util.UUID.randomUUID().toString() }) }
    fun fail(id: String, reason: String) = action { repository.failDelivery(id, reason, requestIds.getOrPut("fail:$id:${state.value.items.find { it.id == id }?.attempts}") { java.util.UUID.randomUUID().toString() }) }
    init { refresh() }
}
@HiltViewModel class RestockingViewModel @Inject constructor(private val repository: OperationsRepository) : ListViewModel<Recommendation>() {
    override suspend fun fetch(query: String) = repository.recommendations()
    fun generate() = infer { repository.generateRecommendations() }
    fun rescore() = infer { repository.rescoreRecommendations() }
    fun customize(id: String, quantity: Int) = action { repository.customizeRecommendation(id, quantity) }
    private fun infer(block: suspend () -> Resource<List<Recommendation>>) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch { try {
            when (val result = block()) {
                is Resource.Success -> mutable.update { it.copy(items = result.data, loading = false) }
                is Resource.Error -> mutable.update { it.copy(error = result.message) }
                Resource.Loading -> Unit
            }
        } finally { mutable.update { it.copy(busy = false) } } }
    }
    init { refresh() }
}
data class DashboardState(val loading: Boolean = true, val data: Dashboard? = null, val error: String? = null)
@HiltViewModel class DashboardViewModel @Inject constructor(private val repository: OperationsRepository) : ViewModel() {
    private val mutable = MutableStateFlow(DashboardState())
    val state = mutable.asStateFlow()
    fun refresh() { viewModelScope.launch {
        mutable.update { it.copy(loading = true, error = null) }
        when (val result = repository.dashboard()) {
            is Resource.Success -> mutable.value = DashboardState(false, result.data)
            is Resource.Error -> mutable.update { it.copy(loading = false, error = result.message) }
            Resource.Loading -> Unit
        }
    } }
    init { refresh() }
}
