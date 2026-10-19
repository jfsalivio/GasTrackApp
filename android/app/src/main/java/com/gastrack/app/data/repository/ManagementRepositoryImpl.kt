package com.gastrack.app.data.repository

import android.util.Base64
import com.gastrack.app.core.network.networkCall
import com.gastrack.app.data.api.GasTrackApi
import com.gastrack.app.data.models.*
import com.gastrack.app.domain.entities.*
import com.gastrack.app.domain.repository.ManagementRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import javax.inject.Inject

class ManagementRepositoryImpl @Inject constructor(private val api: GasTrackApi, private val json: Json) : ManagementRepository {
    override suspend fun records(module: String, query: String) = networkCall(json) {
        when (module) {
            "inventory-transactions" -> api.transactions().map { ManagementRecord(it.transactionId, it.productName, "${it.type} · ${it.quantity}", mapOf("Warehouse" to it.warehouse, "Reason" to it.reason.orEmpty(), "Reference" to it.reference.orEmpty(), "Date" to it.date)) }
            "inventory-transfers" -> api.transfers().map { raw -> ManagementRecord(raw.getValue("transferId").jsonPrimitive.content, "${raw.getValue("fromWarehouse").jsonPrimitive.content} → ${raw.getValue("toWarehouse").jsonPrimitive.content}", raw.getValue("status").jsonPrimitive.content) }
            "sales" -> api.sales(query.ifBlank { null }).map { ManagementRecord(it.id, it.saleNo, "${it.customerName.orEmpty()} · ${it.status.orEmpty()}", mapOf("Total" to "PHP ${it.amount}", "Cashier" to it.cashierName, "Date" to it.datetime)) }
            "suppliers" -> api.suppliers(query.ifBlank { null }).map { ManagementRecord(it.id, it.name, "${it.status} · ${it.supplierType.orEmpty()}", mapOf("Email" to it.email.orEmpty(), "Phone" to it.phone.orEmpty(), "Lead time" to "${it.leadTime} days")) }
            "report" -> api.reports().map { ManagementRecord(it.id.toString(), it.name, "${it.status} · ${it.period}", mapOf("Type" to it.type, "Due" to it.dueDate)) }
            "users" -> api.users(query.ifBlank { null }).map { ManagementRecord(it.id, it.name, "${it.role} · ${it.status}", mapOf("Email" to it.email, "Branch" to it.branchName.orEmpty())) }
            "data" -> api.dataLogs().map { ManagementRecord(it.id, it.file ?: it.type, "${it.format} · ${it.status}", mapOf("Date" to it.date, "Type" to it.type)) }
            "purchase-orders" -> api.purchaseOrders().map { ManagementRecord(it.id, it.poNo, "${it.supplier} · ${it.status}", mapOf("Status" to it.status, "Total" to "PHP ${it.totalAmount}", "Items" to it.itemCount.toString())) }
            else -> throw IllegalArgumentException("Unknown module")
        }
    }
    override suspend fun settings() = networkCall(json) {
        val settings = api.settings()
        json.encodeToJsonElement(SettingsDto.serializer(), settings).jsonObject.mapValues { (_, value) ->
            (value as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
        } + ("businessDays" to settings.businessDays.joinToString(","))
    }
    override suspend fun saveProfile(fields: Map<String, String>) = networkCall(json) {
        api.profile(CompanyProfileRequest(fields["companyName"].orEmpty(), fields["address"].orEmpty(), fields["contactEmail"].orEmpty(), fields["phone"].orEmpty())).message
    }
    override suspend fun saveSettings(section: String, fields: Map<String, String>) = networkCall(json) {
        fun text(key: String) = fields[key].orEmpty()
        fun bool(key: String) = text(key) == "true"
        fun integer(key: String) = text(key).toIntOrNull() ?: throw IllegalArgumentException("Enter a whole number for $key.")
        fun decimal(key: String) = Decimal(text(key).also { require(it.toBigDecimalOrNull() != null) { "Enter a valid number for $key." } })
        when (section) {
            "profile" -> api.profile(CompanyProfileRequest(text("companyName"), text("address"), text("contactEmail"), text("phone")))
            "operations" -> api.operations(OperationsRequest(text("businessDays").split(',').filter(String::isNotBlank), text("openingTime"), text("closingTime"), integer("pickupExpirationDays"), integer("maxDeliveryAttempts")))
            "tax" -> api.tax(TaxRequest(decimal("taxRate"), bool("taxEnabled"), text("currency"), bool("roundUp"), bool("roundDown"), bool("twoDecimalStandard"), bool("allowIssueRefund"), bool("enableCancellationFee"), text("cancellationFeeType"), decimal("cancellationFeeValue")))
            "receipt" -> api.receipt(ReceiptRequest(text("receiptHeader"), bool("showLogoOnReceipt"), bool("showTaxBreakdown"), text("footerMessage"), text("printSize")))
            "system" -> api.system(SystemRequest(integer("autoLogoutMinutes"), text("systemTimezone"), text("dateFormat"), text("language"), text("theme")))
            else -> throw IllegalArgumentException("Unknown settings section.")
        }.message
    }
    override suspend fun saveLogo(bytes: ByteArray, mimeType: String) = networkCall(json) {
        require(mimeType.startsWith("image/") && bytes.size <= 3 * 1024 * 1024) { "Choose an image smaller than 3 MB." }
        api.logo(LogoRequest("data:$mimeType;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP))).message
    }
    override suspend fun warehouses() = networkCall(json) { api.warehouses().map { it.id to it.name } }
    override suspend fun checkoutOptions() = networkCall(json) {
        CheckoutOptions(api.customers().map { it.id to it.name }, api.drivers().map { it.id to it.name }, api.settings().vehicleRates.map { it.name })
    }
    override suspend fun checkout(command: Checkout) = networkCall(json) {
        require(command.items.isNotEmpty()) { "Add at least one product." }
        require(command.warehouseId.isNotBlank()) { "Select a warehouse." }
        val allowed = when (command.type) { "Walk-in" -> listOf("Cash", "GCash", "Card", "Bank Transfer"); "Pickup" -> listOf("GCash", "Card", "Bank Transfer"); "Delivery" -> listOf("GCash", "Card", "Bank Transfer", "Cash on Delivery"); else -> emptyList() }
        require(command.paymentMethod in allowed) { "Choose a valid payment method for the order type." }
        require(command.items.all { it.quantity > 0 })
        require(command.paymentMethod in listOf("Cash", "Cash on Delivery") || !command.reference.isNullOrBlank()) { "Enter a payment reference." }
        require(command.reference == null || command.reference.length <= 50) { "Reference must be at most 50 characters." }
        require(command.type == "Walk-in" || !command.customerId.isNullOrBlank()) { "Select a customer." }
        val pickup = if (command.type == "Pickup") {
            val date = runCatching { java.time.LocalDate.parse(command.pickupDate) }.getOrElse { throw IllegalArgumentException("Enter a valid pickup date (YYYY-MM-DD).") }
            val time = runCatching { java.time.LocalTime.parse(command.pickupTime) }.getOrElse { throw IllegalArgumentException("Enter a valid pickup time (HH:mm).") }
            require(date.atTime(time).atZone(java.time.ZoneId.of("Asia/Manila")).toInstant().isAfter(java.time.Instant.now())) { "Pickup schedule must be in the future." }
            PickupDetails(date.toString(), time.toString())
        } else null
        val delivery = if (command.type == "Delivery") {
            require(!command.deliveryAddress.isNullOrBlank() && command.deliveryAddress.length <= 255) { "Enter a delivery address (up to 255 characters)." }
            require(!command.vehicleType.isNullOrBlank()) { "Select a delivery vehicle." }
            DeliveryDetails(command.customerId!!, command.deliveryAddress, command.instructions, command.riderId, command.vehicleType)
        } else null
        val total = command.items.sumOf { it.product.price * it.quantity.toBigDecimal() } - command.discount
        require(total.signum() > 0 && command.discount.signum() >= 0) { "Enter a valid discount." }
        require(command.paymentMethod == "Cash on Delivery" || command.collected >= total) { "Amount collected must cover the total due." }
        val response = api.checkout(CheckoutRequest(command.type, command.items.map { SaleItem(it.product.id, it.quantity, Decimal(it.product.price.toPlainString())) }, command.paymentMethod, Decimal(command.collected.toPlainString()), command.warehouseId, Decimal(command.discount.toPlainString()), customerId = command.customerId, referenceNo = command.reference, pickupDetails = pickup, deliveryDetails = delivery, returnedCylinders = command.returns.map { ReturnedCylinder(it.key, it.value) }))
        "Sale ${response.saleNo} completed · PHP ${response.totalAmount}"
    }
    override suspend fun saveCustomer(id: String?, fields: Map<String, String>) = networkCall(json) {
        val body = CustomerRequest(fields["type"] ?: "Residential", fields["phone"].orEmpty(), fields["address"].orEmpty(), fields["firstName"], fields["lastName"], fields["companyName"])
        if (id == null) { api.createCustomer(body); "Customer created." } else api.updateCustomer(id, body).message
    }
    override suspend fun adjustStock(id: String, quantity: Int, remarks: String) = networkCall(json) {
        require(quantity >= 0) { "Quantity cannot be negative." }
        api.updateInventory(id, InventoryUpdateRequest(quantity, remarks)).message
    }
    override suspend fun generateReport(id: String) = networkCall(json) { api.generateReport(id).let { DownloadFile(it.fileName, "text/csv", it.csv.toByteArray(Charsets.UTF_8)) } }
    override suspend fun export(dataType: String, format: String) = networkCall(json) { api.export(ExportRequest(dataType, "Today", format = format)).let { DownloadFile(it.fileName, it.mimeType, Base64.decode(it.fileBase64, Base64.DEFAULT)) } }
    override suspend fun importCsv(inventory: Boolean, text: String, name: String) = networkCall(json) {
        require(text.isNotBlank()) { "The CSV file is empty." }
        if (inventory) api.importInventory(CsvImportRequest(text, name)) else api.importSales(CsvImportRequest(text, name, false))
        "Import completed."
    }
    override suspend fun purchaseAction(id: String, action: String) = networkCall(json) {
        require(action in listOf("send", "confirm", "reject", "dispatch", "send-payment"))
        api.purchaseAction(id, action); "Purchase order updated."
    }
    override suspend fun editor(module: String, id: String?) = networkCall(json) {
        when (module) {
            "stock-in", "stock-out", "transfer" -> {
                val warehouses = api.warehouses().map { it.id to it.name }
                val products = api.products()
                val destination = if (module == "transfer") listOf(EditorField("fromWarehouseId", "From warehouse", choices = warehouses), EditorField("toWarehouseId", "To warehouse", choices = warehouses)) else listOf(EditorField("warehouseId", "Warehouse", choices = warehouses))
                val fields = destination + listOf(EditorField("referenceNo", "Reference number", false), EditorField("reason", "Reason", false), EditorField("remarks", "Remarks", false)) + products.map { EditorField("quantity:${it.productId}", "${it.name} quantity", false) }
                EditorDefinition(when (module) { "stock-in" -> "Record stock in"; "stock-out" -> "Record stock out"; else -> "Transfer stock" }, fields, emptyMap())
            }
            "purchase-orders" -> {
                val options = api.purchaseOptions()
                fun choices(key: String) = options.getValue(key).jsonArray.map { it.jsonObject.let { item -> item.getValue("id").jsonPrimitive.content to item.getValue("name").jsonPrimitive.content } }
                val products = options.getValue("products").jsonArray.map { it.jsonObject }
                val fields = listOf(EditorField("supplierId", "Supplier", choices = choices("suppliers")), EditorField("warehouseId", "Destination warehouse", choices = choices("warehouses"))) + products.map { product ->
                    EditorField("qty:${product.getValue("supplierId").jsonPrimitive.content}:${product.getValue("productId").jsonPrimitive.content}", "${product.getValue("productName").jsonPrimitive.content} quantity", false)
                }
                EditorDefinition("Create purchase order", fields, products.associate { "cost:${it.getValue("productId").jsonPrimitive.content}" to it.getValue("unitCost").jsonPrimitive.content } + ("requestKey" to java.util.UUID.randomUUID().toString()))
            }
            "purchase-payment" -> EditorDefinition("Record purchase payment", listOf(EditorField("paymentMethod", "Payment method", choices = listOf("GCash", "Card", "Bank Transfer").map { it to it }), EditorField("referenceNo", "Reference number"), EditorField("amountPaid", "Amount paid")), emptyMap())
            "purchase-receipt" -> {
                require(id != null)
                val items = api.purchaseOrder(id).getValue("items").jsonArray.map { it.jsonObject }
                EditorDefinition("Receive purchase order", items.map { EditorField("qty:${it.getValue("productId").jsonPrimitive.content}", "${it.getValue("productName").jsonPrimitive.content} delivered quantity") }, items.associate { "qty:${it.getValue("productId").jsonPrimitive.content}" to (it.getValue("qty").jsonPrimitive.content.toInt() - it.getValue("receivedQty").jsonPrimitive.content.toInt()).toString() } + ("requestKey" to java.util.UUID.randomUUID().toString()))
            }
            "products" -> {
                val fields = listOf(EditorField("productName", "Product name"), EditorField("brandId", "Brand", choices = api.brands().map { it.id to it.name }), EditorField("categoryId", "Category", choices = api.categories().map { it.id to it.name }), EditorField("supplierId", "Supplier", choices = api.suppliers().map { it.id to it.name }), EditorField("unit", "Unit", choices = listOf("kg", "m", "pc").map { it to it }), EditorField("unitValue", "Unit value"), EditorField("unitPrice", "Unit price"), EditorField("costPrice", "Cost price"), EditorField("reorderLevel", "Reorder level"), EditorField("imageUrl", "Image URL", false), EditorField("arModelUrl", "AR model URL", false), EditorField("status", "Status", choices = listOf("Active", "Inactive").map { it to it }))
                val raw = if (id != null) api.product(id) else null
                val aliases = mapOf("productName" to "ProductName", "brandId" to "BrandID", "categoryId" to "CategoryID", "supplierId" to "SupplierID", "unit" to "Unit", "unitValue" to "UnitValue", "unitPrice" to "UnitPrice", "costPrice" to "CostPrice", "reorderLevel" to "ReorderLevel", "imageUrl" to "ImageURL", "arModelUrl" to "ARModelURL", "status" to "Status")
                EditorDefinition("Product", fields, if (raw == null) mapOf("status" to "Active", "unit" to "kg", "unitPrice" to "0", "costPrice" to "0", "reorderLevel" to "0") else aliases.mapValues { (_, key) -> raw[key]?.jsonPrimitive?.contentOrNull.orEmpty() })
            }
            "suppliers" -> {
                val types = api.supplierTypes().map { it.getValue("id").jsonPrimitive.content to it.getValue("name").jsonPrimitive.content }
                val fields = listOf(EditorField("fullName", "Supplier name"), EditorField("supplierTypeId", "Supplier type", choices = types), EditorField("defaultLeadTime", "Lead time (days)"), EditorField("contactPerson", "Contact person"), EditorField("email", "Email"), EditorField("phone", "Phone"), EditorField("address", "Address"), EditorField("status", "Status", choices = listOf("Active", "Inactive").map { it to it }))
                val supplier = if (id != null) api.supplier(id) else null
                EditorDefinition("Supplier", fields, supplier?.let { mapOf("fullName" to it.name, "supplierTypeId" to it.supplierTypeId.toString(), "defaultLeadTime" to it.leadTime.toString(), "contactPerson" to it.contactPerson.orEmpty(), "email" to it.email.orEmpty(), "phone" to it.phone.orEmpty(), "address" to it.address.orEmpty(), "status" to it.status) } ?: mapOf("status" to "Active", "defaultLeadTime" to "3"))
            }
            "report" -> EditorDefinition("Schedule report", listOf(EditorField("reportName", "Report name"), EditorField("reportType", "Report type", choices = listOf("Sales Summary", "Inventory Audit", "Restocking Logs").map { it to it }), EditorField("periodLabel", "Period label"), EditorField("periodStart", "Period start (YYYY-MM-DD)"), EditorField("periodEnd", "Period end (YYYY-MM-DD)"), EditorField("dueDate", "Due date (YYYY-MM-DD)")), emptyMap())
            "users" -> {
                val fields = listOf(EditorField("firstName", "First name"), EditorField("lastName", "Last name"), EditorField("usernameEmail", "Email", required = id == null), EditorField("password", "Password", required = id == null, secret = true), EditorField("role", "Role", choices = api.roles().map { it.name to it.name }), EditorField("branchId", "Branch", false, choices = api.userSettings("branches").map { it.id to it.name }), EditorField("branch", "Warehouse", false, choices = api.warehouses().map { it.name to it.name }), EditorField("status", "Status", choices = listOf("Active", "Inactive").map { it to it }))
                val user = if (id != null) api.user(id) else null
                val accessFields = if (api.me().role.lowercase() in listOf("admin", "administrator")) listOf("dashboard", "pos", "inventory", "products", "sales", "restocking", "orders", "suppliers", "report", "data", "users", "settings").map { EditorField("access:$it", "$it access", choices = listOf("true" to "Enabled", "false" to "Disabled")) } else emptyList()
                val accessValues = accessFields.associate { it.key to (user?.moduleAccess?.get(it.key.removePrefix("access:")) ?: false).toString() }
                EditorDefinition("User account", fields + accessFields, (user?.let { mapOf("firstName" to it.firstName, "lastName" to it.lastName, "usernameEmail" to it.email, "role" to it.role, "branchId" to it.branchId.orEmpty(), "branch" to it.branch.orEmpty(), "status" to it.status) } ?: mapOf("status" to "Active")) + accessValues)
            }
            else -> throw IllegalArgumentException("Editor unavailable for this module.")
        }
    }
    override suspend fun saveRecord(module: String, id: String?, fields: Map<String, String>) = networkCall(json) {
        fun text(key: String) = fields[key].orEmpty().trim()
        fun integer(key: String) = text(key).toIntOrNull() ?: throw IllegalArgumentException("Enter a whole number for $key.")
        fun decimal(key: String) = Decimal(text(key).also { require(it.toBigDecimalOrNull()?.signum()?.let { sign -> sign >= 0 } == true) { "Enter a non-negative number for $key." } })
        when (module) {
            "stock-in", "stock-out", "transfer" -> {
                val items = fields.filterKeys { it.startsWith("quantity:") }.mapNotNull { (key, value) ->
                    val qty = value.toIntOrNull() ?: if (value.isBlank()) 0 else throw IllegalArgumentException("Enter whole-number quantities.")
                    require(qty >= 0) { "Quantity cannot be negative." }
                    if (qty == 0) null else StockItem(key.removePrefix("quantity:"), qty)
                }
                require(items.isNotEmpty()) { "Enter at least one positive product quantity." }
                if (module == "transfer") {
                    require(text("fromWarehouseId") != text("toWarehouseId")) { "Select different warehouses." }
                    api.transfer(TransferRequest(text("fromWarehouseId"), text("toWarehouseId"), items, "Completed", text("remarks").ifBlank { null }))
                } else {
                    val body = StockMovementRequest(text("warehouseId"), items, text("referenceNo").ifBlank { null }, text("reason").ifBlank { null }, text("remarks").ifBlank { null })
                    if (module == "stock-in") api.stockIn(body) else api.stockOut(body)
                }
            }
            "purchase-orders" -> {
                val supplier = text("supplierId")
                val items = fields.filterKeys { it.startsWith("qty:$supplier:") }.mapNotNull { (key, value) ->
                    val quantity = value.toIntOrNull() ?: if (value.isBlank()) 0 else throw IllegalArgumentException("Enter whole-number quantities.")
                    require(quantity >= 0) { "Quantity cannot be negative." }
                    val product = key.substringAfterLast(':')
                    if (quantity == 0) null else PurchaseItem(product, quantity, Decimal(text("cost:$product")))
                }
                require(items.isNotEmpty()) { "Enter at least one product quantity." }
                val created = api.createPurchaseOrder(PurchaseOrderRequest(supplier, text("warehouseId"), items, requestKey = text("requestKey")))
                val emailError = created["emailError"]?.jsonPrimitive?.contentOrNull
                if (emailError != null) return@networkCall "Purchase order saved as Pending. Sending email failed; use Send to retry."
            }
            "purchase-payment" -> { require(id != null); api.purchasePayment(id, PurchasePaymentRequest(text("paymentMethod"), text("referenceNo"), decimal("amountPaid"))) }
            "purchase-receipt" -> {
                require(id != null)
                val items = fields.filterKeys { it.startsWith("qty:") }.map { (key, value) -> ReturnedCylinder(key.removePrefix("qty:"), value.toIntOrNull()?.takeIf { it >= 0 } ?: throw IllegalArgumentException("Enter non-negative whole-number quantities.")) }
                api.receivePurchase(id, PurchaseReceiptRequest(text("requestKey"), items))
            }
            "products" -> { val body = ProductRequest(text("productName"), text("categoryId"), text("brandId"), text("supplierId"), text("unit"), text("unitValue"), decimal("unitPrice"), decimal("costPrice"), integer("reorderLevel"), text("imageUrl").ifBlank { null }, text("arModelUrl").ifBlank { null }, text("status")); if (id == null) api.createProduct(body) else api.updateProduct(id, body) }
            "suppliers" -> { val body = SupplierRequest(text("fullName"), integer("defaultLeadTime"), text("status"), text("contactPerson"), text("email"), text("phone"), text("address"), integer("supplierTypeId")); if (id == null) api.createSupplier(body) else api.updateSupplier(id, body) }
            "report" -> api.createReport(ReportRequest(text("reportName"), text("reportType"), text("periodLabel"), text("periodStart"), text("periodEnd"), text("dueDate")))
            "users" -> {
                val modules = fields.filterKeys { it.startsWith("access:") }.mapKeys { it.key.removePrefix("access:") }.mapValues { it.value == "true" }.takeIf { it.isNotEmpty() }
                if (id == null) api.createUser(UserRequest(text("usernameEmail"), text("password"), text("role"), text("branch").ifBlank { null }, text("status"), text("firstName"), text("lastName"), text("branchId").ifBlank { null }, modules))
                else api.updateUser(id, buildJsonObject {
                    for (key in listOf("firstName", "lastName", "role", "branch", "branchId", "status")) put(key, text(key))
                    if (text("password").isNotBlank()) put("password", text("password"))
                    modules?.let { access -> put("modules", buildJsonObject { access.forEach { (key, value) -> put(key, value) } }) }
                })
            }
            else -> throw IllegalArgumentException("Unknown module.")
        }
        "Saved successfully."
    }
    override suspend fun deleteRecord(module: String, id: String) = networkCall(json) {
        when (module) {
            "products" -> api.deleteProduct(id)
            "suppliers" -> api.deleteSupplier(id)
            "users" -> api.deleteUser(id)
            "report" -> api.deleteReport(id)
            "sales" -> api.deleteSale(id)
            else -> throw IllegalArgumentException("Deletion unavailable.")
        }.message
    }
}
