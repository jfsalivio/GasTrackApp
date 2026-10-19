package com.gastrack.app.presentation.viewmodels

import androidx.lifecycle.*
import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.*
import com.gastrack.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class ManagementViewModel @Inject constructor(private val repository: ManagementRepository) : ListViewModel<ManagementRecord>() {
    private var module: String = "sales"
    private val downloadsChannel = Channel<DownloadFile>(Channel.BUFFERED)
    val downloads = downloadsChannel.receiveAsFlow()
    fun select(module: String) { if (this.module != module || state.value.loading) { this.module = module; refresh() } }
    override suspend fun fetch(query: String) = repository.records(module, query)
    fun purchaseAction(id: String, action: String) = action { repository.purchaseAction(id, action) }
    fun delete(id: String) = action { repository.deleteRecord(module, id) }
    fun export(type: String, format: String) = action {
        when (val result = repository.export(type, format)) {
            is Resource.Success -> { downloadsChannel.send(result.data); Resource.Success("Export ready to save.") }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
    }
    fun generateReport(id: String) = action {
        when (val result = repository.generateReport(id)) {
            is Resource.Success -> { downloadsChannel.send(result.data); Resource.Success("Report ready to save.") }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
    }
    fun importCsv(inventory: Boolean, text: String, name: String) = action { repository.importCsv(inventory, text, name) }
}
@HiltViewModel class SettingsViewModel @Inject constructor(private val repository: ManagementRepository) : ViewModel() {
    private val mutable = MutableStateFlow(FormState(busy = true))
    val state = mutable.asStateFlow()
    init { viewModelScope.launch {
        when (val result = repository.settings()) {
            is Resource.Success -> mutable.value = FormState(fields = result.data)
            is Resource.Error -> mutable.value = FormState(error = result.message)
            Resource.Loading -> Unit
        }
    } }
    fun edit(key: String, value: String) { mutable.update { it.copy(fields = it.fields + (key to value), message = null) } }
    fun save(section: String) { if (state.value.busy) return; viewModelScope.launch {
        mutable.update { it.copy(busy = true, error = null) }
        when (val result = repository.saveSettings(section, state.value.fields)) {
            is Resource.Success -> mutable.update { it.copy(busy = false, message = result.data) }
            is Resource.Error -> mutable.update { it.copy(busy = false, error = result.message) }
            Resource.Loading -> Unit
        }
    } }
    fun logo(bytes: ByteArray, mimeType: String) { viewModelScope.launch {
        mutable.update { it.copy(busy = true, error = null) }
        when (val result = repository.saveLogo(bytes, mimeType)) {
            is Resource.Success -> mutable.update { it.copy(busy = false, message = result.data) }
            is Resource.Error -> mutable.update { it.copy(busy = false, error = result.message) }
            Resource.Loading -> Unit
        }
    } }
}
data class PosState(val loading: Boolean = true, val products: List<Product> = emptyList(), val cart: List<CartItem> = emptyList(), val warehouses: List<Pair<String, String>> = emptyList(), val warehouse: String = "", val method: String = "Cash", val collected: String = "", val reference: String = "", val busy: Boolean = false, val error: String? = null, val message: String? = null, val type: String = "Walk-in", val fields: Map<String, String> = emptyMap(), val options: CheckoutOptions? = null, val returned: Boolean? = null, val returns: Map<String, String> = emptyMap())
@HiltViewModel class PosViewModel @Inject constructor(private val operations: OperationsRepository, private val management: ManagementRepository) : ViewModel() {
    private val mutable = MutableStateFlow(PosState())
    val state = mutable.asStateFlow()
    init { refresh() }
    fun refresh() { viewModelScope.launch {
        mutable.update { it.copy(loading = true) }
        val products = operations.products(warehouseId = state.value.warehouse.ifBlank { null })
        val warehouses = management.warehouses()
        mutable.update { it.copy(loading = false, products = (products as? Resource.Success)?.data?.filter { product -> product.status == "Active" } ?: it.products, warehouses = (warehouses as? Resource.Success)?.data ?: it.warehouses, error = (products as? Resource.Error)?.message ?: (warehouses as? Resource.Error)?.message) }
    } }
    fun add(product: Product) { mutable.update { s ->
        val item = s.cart.find { it.product.id == product.id }
        val count = (item?.quantity ?: 0) + 1
        if (count.toBigDecimal() > product.stock) s.copy(error = "Not enough stock.")
        else s.copy(cart = s.cart.filterNot { it.product.id == product.id } + CartItem(product, count), error = null, message = null)
    } }
    fun remove(id: String) { mutable.update { it.copy(cart = it.cart.filterNot { item -> item.product.id == id }) } }
    fun edit(key: String, value: String) {
        mutable.update { when (key) {
            "warehouse" -> it.copy(warehouse = value)
            "method" -> it.copy(method = value)
            "collected" -> it.copy(collected = value)
            "reference" -> it.copy(reference = value.take(50))
            "type" -> it.copy(type = value, method = if (value == "Walk-in") "Cash" else "GCash", returned = null, returns = emptyMap())
            else -> it.copy(fields = it.fields + (key to value))
        } }
        if (key == "warehouse") { mutable.update { it.copy(cart = emptyList()) }; refresh() }
        if (key == "type" && value != "Walk-in" && state.value.options == null) viewModelScope.launch {
            when (val options = management.checkoutOptions()) {
                is Resource.Success -> mutable.update { it.copy(options = options.data) }
                is Resource.Error -> mutable.update { it.copy(error = options.message) }
                Resource.Loading -> Unit
            }
        }
    }
    fun returned(value: Boolean) { mutable.update { it.copy(returned = value) } }
    fun returnQuantity(id: String, value: String) { mutable.update { it.copy(returns = it.returns + (id to value.filter(Char::isDigit))) } }
    fun checkout() {
        val s = state.value
        val collected = if (s.method == "Cash on Delivery") java.math.BigDecimal.ZERO else s.collected.toBigDecimalOrNull()
        if (collected == null || collected.signum() < 0) { mutable.update { it.copy(error = "Enter a valid amount collected.") }; return }
        if (s.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val returns = if (s.type == "Walk-in" && s.returned == true) s.returns.mapNotNull { (id, qty) -> qty.toIntOrNull()?.takeIf { it > 0 }?.let { id to it } }.toMap() else emptyMap()
            val discount = s.fields["discount"].orEmpty().ifBlank { "0" }.toBigDecimalOrNull()
            if (discount == null) { mutable.update { it.copy(busy = false, error = "Enter a valid discount.") }; return@launch }
            when (val result = management.checkout(Checkout(s.cart, s.warehouse, s.method, collected, s.reference.ifBlank { null }, discount, s.type, s.fields["customerId"], s.fields["pickupDate"], s.fields["pickupTime"], s.fields["address"], s.fields["instructions"], s.fields["riderId"], s.fields["vehicle"], returns))) {
                is Resource.Success -> { mutable.update { it.copy(busy = false, cart = emptyList(), message = result.data) }; refresh() }
                is Resource.Error -> mutable.update { it.copy(busy = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
