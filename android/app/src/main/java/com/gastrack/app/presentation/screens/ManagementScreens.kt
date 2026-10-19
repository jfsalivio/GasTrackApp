package com.gastrack.app.presentation.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastrack.app.domain.entities.DownloadFile
import com.gastrack.app.core.common.readLimited
import com.gastrack.app.presentation.components.*
import com.gastrack.app.presentation.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun ManagementScreen(module: String) {
    when (module) {
        "pos" -> PosScreen(hiltViewModel())
        "settings" -> SettingsScreen(hiltViewModel())
        else -> RecordsScreen(module, hiltViewModel(key = module))
    }
}
@Composable fun RecordsScreen(module: String, vm: ManagementViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<DownloadFile?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Pair<String?, String>?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var purchaseForm by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val file = pending
        if (uri != null && file != null) scope.launch {
            fileError = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(file.bytes) } ?: error("Could not open destination.") }.exceptionOrNull()?.message }
        }
        pending = null
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val imported = withContext(Dispatchers.IO) { runCatching {
                val fileName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else "import.csv" } ?: "import.csv"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readLimited(5 * 1024 * 1024) } ?: error("Could not open file.")
                require(bytes.size <= 5 * 1024 * 1024) { "Choose a CSV smaller than 5 MB." }
                fileName to bytes.toString(Charsets.UTF_8)
            } }
            imported.onSuccess { (name, text) -> vm.importCsv(module == "inventory", text, name) }.onFailure { fileError = it.message }
        }
    }
    LaunchedEffect(module) { vm.select(module) }
    LaunchedEffect(vm) { vm.downloads.collect { pending = it; save.launch(it.name) } }
    ListScreen(vm, { it.id }, header = {
        if (module in listOf("suppliers", "users", "report", "purchase-orders")) Button(onClick = { editor = null to java.util.UUID.randomUUID().toString() }) { Text(when (module) { "report" -> "Schedule report"; "purchase-orders" -> "Create purchase order"; else -> "Add ${module.removeSuffix("s")}" }) }
        fileError?.let { ErrorPanel(it) }
        if (module == "data") Row {
            Button(onClick = { vm.export("Sales Data", "CSV") }) { Text("Export sales CSV") }
            TextButton(onClick = { open.launch(arrayOf("text/*", "application/csv")) }) { Text("Import sales") }
        }
        if (module == "sales") TextButton(onClick = { open.launch(arrayOf("text/*", "application/csv")) }) { Text("Import CSV") }
    }) { record ->
        RecordCard(record.title, record.subtitle, record.details.entries.joinToString("\n") { "${it.key}: ${it.value}" }) {
            if (module in listOf("suppliers", "users")) TextButton(onClick = { editor = record.id to java.util.UUID.randomUUID().toString() }) { Text("Edit") }
            if (module in listOf("suppliers", "users", "report", "sales")) TextButton(onClick = { deleting = record.id }) { Text("Delete") }
            if (module == "report") TextButton(onClick = { vm.generateReport(record.id) }) { Text("Generate CSV") }
            if (module == "purchase-orders") {
                val action = when (record.details["Status"]) { "Pending" -> "send"; "Sent" -> "confirm"; "Approved" -> "dispatch"; else -> null }
                action?.let { TextButton(onClick = { vm.purchaseAction(record.id, it) }) { Text(it.replaceFirstChar(Char::uppercase)) } }
                if (record.details["Status"] in listOf("Approved", "Paid")) TextButton(onClick = { purchaseForm = Triple("purchase-payment", record.id, java.util.UUID.randomUUID().toString()) }) { Text("Payment") }
                if (record.details["Status"] in listOf("Paid", "Out for Delivery")) TextButton(onClick = { purchaseForm = Triple("purchase-receipt", record.id, java.util.UUID.randomUUID().toString()) }) { Text("Receive") }
            }
        }
    }
    editor?.let { (id, instance) -> EditorDialog(module, id, instance, { editor = null }, vm::refresh) }
    purchaseForm?.let { (form, id, instance) -> EditorDialog(form, id, instance, { purchaseForm = null }, vm::refresh) }
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete record?") }, text = { Text("This action affects the saved record and may be restricted by transaction history.") }, confirmButton = { TextButton(onClick = { vm.delete(id); deleting = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
@Composable private fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf("profile") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageError by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                val mime = context.contentResolver.getType(uri) ?: error("Could not read image type.")
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readLimited(3 * 1024 * 1024) } ?: error("Could not open image.")
                require(bytes.size <= 3 * 1024 * 1024) { "Choose an image smaller than 3 MB." }
                bytes to mime
            } }
            result.onSuccess { (bytes, mime) -> vm.logo(bytes, mime) }.onFailure { imageError = it.message }
        }
    }
    val groups = linkedMapOf("profile" to "Business profile", "operations" to "Operations", "tax" to "Financial", "receipt" to "Receipt", "system" to "System")
    val inputs = when (section) {
        "operations" -> listOf("businessDays" to "Business days (comma separated)", "openingTime" to "Opening time (HH:mm)", "closingTime" to "Closing time (HH:mm)", "pickupExpirationDays" to "Pickup expiration (business days)", "maxDeliveryAttempts" to "Maximum delivery attempts")
        "tax" -> listOf("taxRate" to "Tax rate (%)", "currency" to "Currency", "cancellationFeeType" to "Cancellation fee type", "cancellationFeeValue" to "Cancellation fee value")
        "receipt" -> listOf("receiptHeader" to "Receipt header", "footerMessage" to "Footer message", "printSize" to "Print size")
        "system" -> listOf("autoLogoutMinutes" to "Auto logout (minutes)", "systemTimezone" to "Time zone", "dateFormat" to "Date format", "language" to "Language", "theme" to "Theme")
        else -> listOf("companyName" to "Company name", "address" to "Address", "contactEmail" to "Email", "phone" to "Phone")
    }
    val toggles = when (section) {
        "tax" -> listOf("taxEnabled" to "Enable tax", "roundUp" to "Round up", "roundDown" to "Round down", "twoDecimalStandard" to "Two decimal standard", "allowIssueRefund" to "Allow refunds", "enableCancellationFee" to "Enable cancellation fee")
        "receipt" -> listOf("showLogoOnReceipt" to "Show logo", "showTaxBreakdown" to "Show tax breakdown")
        else -> emptyList()
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScrollableTabRow(groups.keys.indexOf(section), edgePadding = 0.dp) { groups.forEach { (id, label) -> Tab(section == id, { section = id }, text = { Text(label) }) } } }
        item { Text(groups.getValue(section), style = MaterialTheme.typography.titleLarge) }
        state.error?.let { item { ErrorPanel(it) } }
        imageError?.let { item { ErrorPanel(it) } }
        state.message?.let { item { Text(it) } }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(inputs) { (key, label) -> FormField(label, state.fields[key].orEmpty(), { vm.edit(key, it) }, enabled = !state.busy) }
        items(toggles) { (key, label) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, Modifier.padding(top = 12.dp)); Switch(state.fields[key] == "true", { vm.edit(key, it.toString()) }, enabled = !state.busy) } }
        if (section == "profile") item { TextButton(onClick = { imagePicker.launch("image/*") }, enabled = !state.busy) { Text("Choose company logo") } }
        item { Button(onClick = { vm.save(section) }, enabled = !state.busy) { Text("Save changes") } }
    }
}
@Composable private fun PosScreen(vm: PosViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        s.error?.let { item { ErrorPanel(it) } }
        s.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        if (s.loading) item { LoadingPanel() }
        item { Text("Order type", style = MaterialTheme.typography.titleMedium) }
        item { Row { listOf("Walk-in", "Pickup", "Delivery").forEach { type -> TextButton(onClick = { vm.edit("type", type) }, enabled = !s.busy) { Text(if (s.type == type) "✓ $type" else type) } } } }
        if (s.type != "Walk-in") {
            item { Text("Customer", style = MaterialTheme.typography.titleMedium) }
            items(s.options?.customers.orEmpty()) { (id, name) -> Row { RadioButton(s.fields["customerId"] == id, { vm.edit("customerId", id) }); Text(name, Modifier.padding(top = 12.dp)) } }
        }
        if (s.type == "Pickup") {
            item { FormField("Pickup date (YYYY-MM-DD)", s.fields["pickupDate"].orEmpty(), { vm.edit("pickupDate", it) }) }
            item { FormField("Pickup time (HH:mm, Philippine time)", s.fields["pickupTime"].orEmpty(), { vm.edit("pickupTime", it) }) }
        }
        if (s.type == "Delivery") {
            item { FormField("Delivery address", s.fields["address"].orEmpty(), { vm.edit("address", it) }) }
            item { FormField("Delivery instructions", s.fields["instructions"].orEmpty(), { vm.edit("instructions", it.take(255)) }) }
            item { Text("Vehicle", style = MaterialTheme.typography.titleMedium) }
            items(s.options?.vehicles.orEmpty()) { vehicle -> Row { RadioButton(s.fields["vehicle"] == vehicle, { vm.edit("vehicle", vehicle) }); Text(vehicle, Modifier.padding(top = 12.dp)) } }
            item { Text("Driver (optional)", style = MaterialTheme.typography.titleMedium) }
            items(s.options?.drivers.orEmpty()) { (id, name) -> Row { RadioButton(s.fields["riderId"] == id, { vm.edit("riderId", id) }); Text(name, Modifier.padding(top = 12.dp)) } }
        }
        item { Text("Warehouse", style = MaterialTheme.typography.titleMedium) }
        items(s.warehouses) { (id, name) -> Row { RadioButton(s.warehouse == id, { vm.edit("warehouse", id) }); Text(name, Modifier.padding(top = 12.dp)) } }
        item { Text("Products", style = MaterialTheme.typography.titleLarge) }
        items(s.products, key = { it.id }) { product -> Column { ProductImage(product.imageUrl, product.name); RecordCard(product.name, "PHP ${product.price}", "${product.stock} available") { TextButton(onClick = { vm.add(product) }, enabled = !s.busy) { Text("Add to cart") } } } }
        item { Text("Cart", style = MaterialTheme.typography.titleLarge) }
        items(s.cart, key = { "cart:${it.product.id}" }) { item -> RecordCard(item.product.name, "${item.quantity} × PHP ${item.product.price}") { TextButton(onClick = { vm.remove(item.product.id) }) { Text("Remove") } } }
        item { Text("Item subtotal: PHP ${s.cart.fold(java.math.BigDecimal.ZERO) { total, item -> total + item.product.price * item.quantity.toBigDecimal() }}") }
        item { FormField("Discount (PHP)", s.fields["discount"].orEmpty(), { vm.edit("discount", it) }) }
        item { val total = s.cart.sumOf { it.product.price * it.quantity.toBigDecimal() } - (s.fields["discount"]?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO); Text("Total due: PHP $total", style = MaterialTheme.typography.titleLarge) }
        val methods = when (s.type) { "Pickup" -> listOf("GCash", "Card", "Bank Transfer"); "Delivery" -> listOf("GCash", "Card", "Bank Transfer", "Cash on Delivery"); else -> listOf("Cash", "GCash", "Card", "Bank Transfer") }
        items(methods) { method -> Row { RadioButton(s.method == method, { vm.edit("method", method) }); Text(method, Modifier.padding(top = 12.dp)) } }
        if (s.method != "Cash on Delivery") item { FormField("Amount collected", s.collected, { vm.edit("collected", it) }) }
        if (s.method !in listOf("Cash", "Cash on Delivery")) item { FormField("Reference number", s.reference, { vm.edit("reference", it) }) }
        if (s.type == "Walk-in" && s.cart.any { it.product.category in listOf("Cylinder", "Gasul LPG") }) {
            item { Text("Empty Cylinder Returned?"); Row { TextButton(onClick = { vm.returned(true) }) { Text(if (s.returned == true) "✓ Yes" else "Yes") }; TextButton(onClick = { vm.returned(false) }) { Text(if (s.returned == false) "✓ No" else "No") } } }
            if (s.returned == true) items(s.products.filter { it.category == "Cylinder" }) { product -> FormField("${product.name} return quantity", s.returns[product.id].orEmpty(), { vm.returnQuantity(product.id, it) }) }
        }
        if (s.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { Button(onClick = vm::checkout, enabled = !s.busy && s.cart.isNotEmpty() && s.warehouse.isNotBlank()) { Text("Complete sale") } }
    }
}
