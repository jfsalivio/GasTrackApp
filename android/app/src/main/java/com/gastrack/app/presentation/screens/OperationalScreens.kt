package com.gastrack.app.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastrack.app.presentation.components.*
import com.gastrack.app.presentation.viewmodels.*

@Composable fun <T> ListScreen(vm: ListViewModel<T>, key: (T) -> String, header: @Composable () -> Unit = {}, row: @Composable (T) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.events.collect { snackbar.showSnackbar(it) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        header()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.query, vm::editQuery, label = { Text("Search") }, modifier = Modifier.weight(1f), singleLine = true)
            TextButton(onClick = vm::refresh, enabled = !state.loading) { Text("Refresh") }
        }
        state.error?.let { ErrorPanel(it, vm::refresh) }
        if (state.loading) LoadingPanel()
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!state.loading && state.items.isEmpty() && state.error == null) Text("No records found.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(state.items, key = key) { row(it) }
        }
        SnackbarHost(snackbar)
    }
}
@Composable fun DashboardScreen(vm: DashboardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = vm::refresh) { Text("Refresh dashboard") } }
        state.error?.let { item { ErrorPanel(it, vm::refresh) } }
        if (state.loading) item { LoadingPanel() }
        state.data?.let { data ->
            item { RecordCard("Sales performance", "PHP ${data.sales.setScale(2)}", data.salesChange) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { RecordCard("Transactions", data.transactions.toString()) }
                Box(Modifier.weight(1f)) { RecordCard("Stock attention", data.stockAttention.toString()) }
            } }
            item { RecordCard("Best seller", data.bestSeller) }
            item {
                Card { Column(Modifier.padding(16.dp)) {
                    Text("Sales trend", style = MaterialTheme.typography.titleMedium)
                    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                        val maximum = data.trend.maxOfOrNull { it.second.toFloat() }?.coerceAtLeast(1f) ?: 1f
                        val points = data.trend.mapIndexed { index, point -> Offset(index * size.width / (data.trend.size - 1).coerceAtLeast(1), size.height * (1 - point.second.toFloat() / maximum)) }
                        points.zipWithNext().forEach { (start, end) -> drawLine(Color(0xFF1D6BF3), start, end, 4f) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { data.trend.forEach { Text(it.first, style = MaterialTheme.typography.labelSmall) } }
                } }
            }
            item { Text("Recent activity", style = MaterialTheme.typography.titleMedium) }
            items(data.activity) { Text(it, Modifier.padding(vertical = 8.dp)) }
        }
    }
}
@Composable fun InventoryScreen(vm: InventoryViewModel) {
    var id by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var movement by remember { mutableStateOf<Pair<String, String>?>(null) }
    ListScreen(vm, { it.id }, header = { Row { listOf("stock-in" to "Stock in", "stock-out" to "Stock out", "transfer" to "Transfer").forEach { (kind, label) -> TextButton(onClick = { movement = kind to java.util.UUID.randomUUID().toString() }) { Text(label) } } } }) { stock -> RecordCard(stock.name, "${stock.quantity} units · ${stock.warehouse}", "${stock.status} · Reorder level ${stock.reorderLevel}") {
        TextButton(onClick = { id = stock.id; quantity = stock.quantity.toString(); remarks = "" }) { Text("Adjust stock") }
    } }
    movement?.let { (kind, instance) -> EditorDialog(kind, null, instance, { movement = null }, vm::refresh) }
    id?.let { selected -> AlertDialog(onDismissRequest = { id = null }, title = { Text("Stock adjustment") }, text = { Column {
        FormField("New quantity", quantity, { quantity = it.filter(Char::isDigit) }); FormField("Remarks", remarks, { remarks = it })
    } }, confirmButton = { TextButton(onClick = { vm.adjust(selected, quantity.toInt(), remarks); id = null }, enabled = (quantity.toIntOrNull() ?: -1) >= 0) { Text("Save") } }, dismissButton = { TextButton(onClick = { id = null }) { Text("Cancel") } }) }
}
@Composable fun ProductsScreen(vm: ProductsViewModel) {
    var editor by remember { mutableStateOf<Pair<String?, String>?>(null) }
    ListScreen(vm, { it.id }, header = { Button(onClick = { editor = null to java.util.UUID.randomUUID().toString() }) { Text("Add product") } }) { product -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProductImage(product.imageUrl, product.name)
        RecordCard(product.name, "PHP ${product.price} · ${product.category}", "${product.stock} in stock") {
        TextButton(onClick = { editor = product.id to java.util.UUID.randomUUID().toString() }) { Text("Edit") }
    } } }
    editor?.let { (id, instance) -> EditorDialog("products", id, instance, { editor = null }, vm::refresh) }
}
@Composable fun CustomersScreen(vm: CustomersViewModel) {
    var open by remember { mutableStateOf(false) }
    var fields by remember { mutableStateOf(mapOf("type" to "Residential")) }
    ListScreen(vm, { it.id }, header = { Button(onClick = { open = true }) { Text("Add customer") } }) { RecordCard(it.name, "${it.type} · ${it.phone.orEmpty()}", it.address.orEmpty()) }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Add customer") }, text = { Column {
        Row { listOf("Residential", "Commercial").forEach { type -> TextButton(onClick = { fields = fields + ("type" to type) }) { Text(if (fields["type"] == type) "✓ $type" else type) } } }
        listOf("firstName" to "First name", "lastName" to "Last name", "phone" to "Phone", "address" to "Address").forEach { (key, label) -> FormField(label, fields[key].orEmpty(), { fields = fields + (key to it) }) }
        if (fields["type"] == "Commercial") FormField("Company name", fields["companyName"].orEmpty(), { fields = fields + ("companyName" to it) })
    } }, confirmButton = { TextButton(onClick = { vm.create(fields); open = false }, enabled = listOf("firstName", "lastName", "phone", "address").all { !fields[it].isNullOrBlank() } && (fields["type"] != "Commercial" || !fields["companyName"].isNullOrBlank())) { Text("Save") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } })
}
@Composable fun OrdersScreen(vm: OrdersViewModel) = ListScreen(vm, { it.id }) { RecordCard(it.number, "${it.customer.orEmpty()} · ${it.type}", "${it.status} · PHP ${it.total}") }
@Composable fun PickupsScreen(vm: PickupsViewModel) {
    var completionId by remember { mutableStateOf<String?>(null) }
    ListScreen(vm, { it.id }) { order ->
        RecordCard(order.number, order.customer.orEmpty(), "${order.pickupStatus ?: order.status} · PHP ${order.total}") {
            if (order.pickupStatus == "Preparing") TextButton(onClick = { vm.changeStatus(order.id, "Ready for Pickup") }) { Text("Mark ready") }
            if (order.pickupStatus == "Ready for Pickup") TextButton(onClick = { completionId = order.id }) { Text("Complete claim") }
        }
    }
    completionId?.let { CompletionDialog(it, false, false, { completionId = null }, vm::refresh) }
}
@Composable fun DeliveriesScreen(vm: DeliveriesViewModel, isDriver: Boolean) {
    var failureId by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var completing by remember { mutableStateOf<com.gastrack.app.domain.entities.Delivery?>(null) }
    ListScreen(vm, { it.id }) { delivery ->
        RecordCard(delivery.orderNumber, "${delivery.customer.orEmpty()} · ${delivery.status}", "${delivery.address.orEmpty()}\nCollect PHP ${delivery.amountToCollect} · Attempt ${delivery.attempts}/${delivery.maxAttempts}") {
            if (!isDriver && delivery.status in setOf("Preparing", "Failed Attempt")) TextButton(onClick = { vm.dispatch(delivery.id) }) { Text("Dispatch") }
            if (delivery.status in setOf("Out for Delivery", "Failed Attempt")) TextButton(onClick = { failureId = delivery.id; reason = "" }) { Text("Failed attempt") }
            if (delivery.status in setOf("Out for Delivery", "Failed Attempt")) TextButton(onClick = { completing = delivery }) { Text("Complete") }
        }
    }
    failureId?.let { id -> AlertDialog(onDismissRequest = { failureId = null }, title = { Text("Record failed attempt") }, text = { FormField("Reason", reason, { reason = it }) }, confirmButton = { TextButton(onClick = { vm.fail(id, reason); failureId = null }, enabled = reason.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = { failureId = null }) { Text("Cancel") } }) }
    completing?.let { CompletionDialog(it.id, true, it.cashOnDelivery, { completing = null }, vm::refresh) }
}
@Composable fun RestockingScreen(vm: RestockingViewModel) {
    var editId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    val state by vm.state.collectAsStateWithLifecycle()
    ListScreen(vm, { it.id }, header = {
        Row { Button(onClick = vm::generate, enabled = !state.busy) { Text("Generate") }; TextButton(onClick = vm::rescore, enabled = !state.busy) { Text("Refresh scores") } }
        if (state.busy) Text("Analyzing stock and sales history…")
    }) { recommendation ->
        RecordCard(recommendation.name, "${recommendation.priority} · ${recommendation.supplier}", "Stock ${recommendation.stock} · Suggested ${recommendation.quantity}") {
            TextButton(onClick = { editId = recommendation.id; quantity = recommendation.quantity.toString() }) { Text("Customize") }
        }
        recommendation.confidence?.let { confidence ->
            Text("Restock confidence: $confidence%", style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(progress = { (confidence.toFloat() / 100).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        } ?: Text("Confidence unavailable")
    }
    editId?.let { id -> AlertDialog(onDismissRequest = { editId = null }, title = { Text("Customize quantity") }, text = { FormField("Quantity", quantity, { quantity = it.filter(Char::isDigit) }) }, confirmButton = { TextButton(onClick = { vm.customize(id, quantity.toInt()); editId = null }, enabled = (quantity.toIntOrNull() ?: 0) > 0) { Text("Save") } }, dismissButton = { TextButton(onClick = { editId = null }) { Text("Cancel") } }) }
}
