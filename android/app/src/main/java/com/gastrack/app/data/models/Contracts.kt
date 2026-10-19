@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.gastrack.app.data.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/** MySQL DECIMAL values arrive as JSON strings or numbers. Keep precision on the wire. */
@Serializable(with = DecimalSerializer::class)
@JvmInline value class Decimal(val value: String) {
    fun toBigDecimal() = value.toBigDecimal()
    override fun toString() = value
}
object DecimalSerializer : KSerializer<Decimal> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Decimal", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Decimal {
        val text = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content
        require(text.toBigDecimalOrNull() != null) { "Invalid decimal" }
        return Decimal(text)
    }
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Decimal) {
        (encoder as JsonEncoder).encodeJsonElement(JsonUnquotedLiteral(value.toBigDecimal().toPlainString()))
    }
}
@Serializable data class ApiErrorDto(val error: String)
@Serializable data class MessageDto(val message: String)
@Serializable data class MutationDto(val message: String? = null, val id: String? = null, val productId: String? = null, val restockId: String? = null, val status: String? = null)
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RegisterRequest(val companyName: String, val branchName: String, val completeAddress: String, val firstName: String, val lastName: String, val email: String, val password: String, val confirmPassword: String)
@Serializable data class EmailRequest(val email: String)
@Serializable data class ResetPasswordRequest(val token: String, val newPassword: String)
@Serializable data class TokenValidationDto(val valid: Boolean)
@Serializable data class UserDto(val id: String, val firstName: String, val lastName: String, val email: String, val role: String, val companyId: String? = null, val moduleAccess: Map<String, Boolean>)
@Serializable data class LoginDto(val token: String, val user: UserDto)
@Serializable data class ActorDto(
    @SerialName("UserID") val id: String,
    @SerialName("FirstName") val firstName: String,
    @SerialName("LastName") val lastName: String,
    @SerialName("Email") val email: String,
    @SerialName("RoleName") val role: String,
    @SerialName("CompanyID") val companyId: String? = null,
    @SerialName("ModuleAccess") val moduleAccess: Map<String, Boolean>,
)
@Serializable data class LookupDto(val id: String, val name: String, val location: String? = null, val address: String? = null, val status: String? = null, val branchId: String? = null)
@Serializable data class NameRequest(val name: String)
@Serializable data class ProductDto(val productId: String, val name: String, val category: String, val supplier: String, val brand: String? = null, val unit: String, val unitValue: String? = null, val unitPrice: Decimal, val costPrice: Decimal, val reorderLevel: Decimal, val imageUrl: String? = null, val arModelUrl: String? = null, val status: String, val stock: Decimal)
@Serializable data class ProductRequest(val productName: String, val categoryId: String, val brandId: String, val supplierId: String, val unit: String, val unitValue: String, val unitPrice: Decimal, val costPrice: Decimal, val reorderLevel: Int, val imageUrl: String? = null, val arModelUrl: String? = null, val status: String = "Active")
@Serializable data class InventoryDto(val inventoryId: String, val productId: String, val productName: String, val warehouseId: String, val warehouse: String, val currentStock: Decimal, val reorderLimit: Decimal, val lastUpdated: String? = null, val status: String)
@Serializable data class StockItem(val productId: String, val quantity: Int)
@Serializable data class StockMovementRequest(val warehouseId: String, val items: List<StockItem>, val referenceNo: String? = null, val reason: String? = null, val remarks: String? = null)
@Serializable data class TransferRequest(val fromWarehouseId: String, val toWarehouseId: String, val items: List<StockItem>, val status: String, val remarks: String? = null)
@Serializable data class StockAdjustmentRequest(val warehouseId: String, val items: List<StockAdjustmentItem>, val remarks: String? = null)
@Serializable data class StockAdjustmentItem(val productId: String, val newQuantity: Int)
@Serializable data class InventoryUpdateRequest(val newQuantity: Int, val remarks: String? = null)
@Serializable data class CsvImportRequest(val csvText: String, val fileName: String, val adjustInventory: Boolean? = null)
@Serializable data class InventoryTransactionDto(val transactionId: String, val productId: String, val productName: String, val warehouse: String, val type: String, val quantity: Decimal, val reason: String? = null, val reference: String? = null, val date: String, val user: String)
@Serializable data class CustomerDto(val id: String, val name: String, val firstName: String? = null, val lastName: String? = null, val companyName: String? = null, val type: String, val phone: String? = null, val address: String? = null, val status: String, val created: String? = null, val updated: String? = null)
@Serializable data class CustomerRequest(@SerialName("customerType") val type: String, val phone: String, val address: String, val firstName: String? = null, val lastName: String? = null, val companyName: String? = null)
@Serializable data class SaleItem(val productId: String, val qty: Int, val unitPrice: Decimal)
@Serializable data class ReturnedCylinder(val productId: String, val qty: Int)
@Serializable data class PickupDetails(val pickupDate: String, val pickupTime: String, val firstName: String? = null, val lastName: String? = null, val companyName: String? = null, val phone: String? = null, val address: String? = null, val customerType: String? = null)
@Serializable data class DeliveryDetails(val customerId: String, val address: String, val instructions: String? = null, val riderId: String? = null, val vehicleType: String)
@Serializable data class CheckoutRequest(val customerType: String, val items: List<SaleItem>, val paymentMethod: String, val amountCollected: Decimal, val warehouseId: String, val discount: Decimal = Decimal("0"), val customerId: String? = null, val remarks: String? = null, val referenceNo: String? = null, val returnedCylinders: List<ReturnedCylinder> = emptyList(), val pickupDetails: PickupDetails? = null, val deliveryDetails: DeliveryDetails? = null)
@Serializable data class CheckoutDto(val saleId: String, val saleNo: String, val orderId: String? = null, val orderNo: String? = null, val totalAmount: Decimal, val cashierName: String, val emptyCylinderReference: String? = null, val subtotal: Decimal, val vat: Decimal, val taxRate: Decimal, val discount: Decimal, val changeDue: Decimal? = null)
@Serializable data class PaymentRequest(val paymentType: String, val paymentMethod: String, val amountPaid: Decimal, val saleId: String? = null, val purchaseOrderId: String? = null, val referenceNo: String? = null, val remarks: String? = null)
@Serializable data class OrderPaymentRequest(val paymentMethod: String, val amountPaid: Decimal)
@Serializable data class PickupStatusRequest(val status: String)
@Serializable data class DeliveryAssignmentRequest(val deliveredByUserId: String?, val remarks: String)
@Serializable data class CompletionRequest(val requestId: String, val emptyCylinderReturned: Boolean, val returnedCylinders: List<ReturnedCylinder>, val warehouseId: String? = null, val amountCollected: Decimal? = null)
@Serializable data class DeliveryActionRequest(val requestId: String, val reason: String? = null, val notes: String? = null)
@Serializable data class SettlementAmounts(val eligibleAmount: Decimal, val cancellationFee: Decimal, val refundAmount: Decimal)
@Serializable data class SettlementRequest(val requestId: String, val refundMethod: String, val accountName: String, val accountNumber: String, val payoutReferenceNo: String, val quote: SettlementAmounts)
@Serializable data class OrderDto(val orderId: String, val id: String, val type: String, val status: String, val date: String, val totalAmount: Decimal, val customerId: String? = null, val customerName: String? = null, val customerPhone: String? = null, val customerAddress: String? = null, val customerType: String? = null, val pickupStatus: String? = null, val pickupScheduledAt: String? = null, val settlementPending: Int? = null, val remarks: String? = null, val saleId: String? = null, val saleNo: String? = null, val paymentId: String? = null, val referenceNo: String? = null, val paymentMethod: String? = null, val amountPaid: Decimal? = null, val paymentStatus: String? = null, val deliveryId: String? = null, val drNo: String? = null, val deliveryStatus: String? = null, val deliveryNotes: String? = null, val deliveredAt: String? = null, val deliveryFee: Decimal? = null, val deliveryAddress: String? = null, val deliveryRiderId: String? = null, val deliveryRiderName: String? = null, val vehicleType: String? = null, val vehicleRateSnapshot: JsonElement? = null, val canSettle: Boolean = false, val items: List<JsonObject> = emptyList())
@Serializable data class DeliveryDto(val deliveryId: String, val drNo: String, val orderId: String, val id: String, val type: String, val status: String, val date: String, val totalAmount: Decimal, val customerName: String? = null, val deliveryStatus: String, val attempts: Int, val maxAttempts: Int, val nextAttemptAt: String? = null, val deliveryAddress: String? = null, val deliveryRiderId: String? = null, val deliveryRiderName: String? = null, val paymentStatus: String, val paymentMethod: String? = null, val amountToCollect: Decimal, val restockedAt: String? = null, val settledAt: String? = null, val canSettle: Boolean, val vehicleType: String? = null, val vehicleRateSnapshot: JsonElement? = null)
@Serializable data class RestockDto(val restockId: String, val productId: String, val productName: String, val unit: String, val costPrice: Decimal, val supplierId: String, val supplierName: String, val currentStock: Decimal, val reorderLevel: Decimal, val suggestedQty: Decimal, val confidence: Decimal? = null, val status: String, val forecastDate: String, val priority: String)
@Serializable data class RestockUpdateRequest(val recommendedQuantity: Int? = null, val status: String? = null)
@Serializable data class RestockGenerationDto(val created: Int, val ids: List<String>, val refreshed: Int)
@Serializable data class RestockRescoreDto(val refreshed: Int, val message: String)
@Serializable data class PurchaseItem(val productId: String, val qty: Int, val unitCost: Decimal)
@Serializable data class PurchaseOrderRequest(val supplierId: String, val warehouseId: String, val items: List<PurchaseItem>, val paymentMethod: String? = null, val referenceNo: String? = null, val restockIds: List<String> = emptyList(), val requestKey: String)
@Serializable data class PurchaseReceiptRequest(val requestKey: String, val items: List<ReturnedCylinder>, val warehouseId: String? = null)
@Serializable data class PurchasePaymentRequest(val paymentMethod: String, val referenceNo: String, val amountPaid: Decimal)
@Serializable data class SupplierRequest(val fullName: String, val defaultLeadTime: Int, val status: String, val contactPerson: String, val email: String, val phone: String, val address: String, val supplierTypeId: Int)
@Serializable data class ChartPointDto(val day: String, val value: Decimal)
@Serializable data class ActivityDto(val id: String, val module: String, val action: String, val text: String? = null, val description: String? = null, val date: JsonPrimitive? = null)
@Serializable data class LowStockDto(val id: String, val name: String, val stock: Decimal, val status: String, val suggest: Decimal? = null, val reorderLevel: Decimal? = null)
@Serializable data class DashboardDto(val salesPerformance: Decimal, val salesPerformanceChangeLabel: String, val transactions: Int, val transactionsChangeLabel: String, val stockAttention: Int, val bestSeller: String, val salesTrend: List<ChartPointDto>, val restockSuggestions: List<LowStockDto>, val activityLog: List<ActivityDto>)
@Serializable data class ExportRequest(val dataType: String, val dateRange: String? = null, val dateFrom: String? = null, val dateTo: String? = null, val format: String, val year: Int? = null, val brandId: String? = null)
@Serializable data class ExportDto(val fileName: String, val mimeType: String, val fileBase64: String)
@Serializable data class ImportLogRequest(val dataType: String, val fileName: String, val format: String)
@Serializable data class ReportRequest(val reportName: String, val reportType: String, val periodLabel: String, val periodStart: String, val periodEnd: String, val dueDate: String)
@Serializable data class CsvFileDto(val fileName: String, val csv: String)
@Serializable data class CompanyProfileRequest(val companyName: String, val address: String, val contactEmail: String, val phone: String)
@Serializable data class LogoRequest(val logoDataUrl: String)
@Serializable data class OperationsRequest(val businessDays: List<String>, val openingTime: String, val closingTime: String, val pickupExpirationDays: Int, val maxDeliveryAttempts: Int)
@Serializable data class TaxRequest(val taxRate: Decimal, val taxEnabled: Boolean, val currency: String, val roundUp: Boolean, val roundDown: Boolean, val twoDecimalStandard: Boolean, val allowIssueRefund: Boolean, val enableCancellationFee: Boolean, val cancellationFeeType: String, val cancellationFeeValue: Decimal)
@Serializable data class ReceiptRequest(val receiptHeader: String, val showLogoOnReceipt: Boolean, val showTaxBreakdown: Boolean, val footerMessage: String, val printSize: String)
@Serializable data class SystemRequest(val autoLogoutMinutes: Int, val systemTimezone: String, val dateFormat: String, val language: String, val theme: String)
@Serializable data class UserRequest(val usernameEmail: String, val password: String, val role: String, val branch: String? = null, val status: String, val firstName: String, val lastName: String, val branchId: String? = null, val modules: Map<String, Boolean>? = null)
@Serializable data class UserSettingsRequest(val name: String, val address: String? = null, val contact: String? = null, val location: String? = null, val branchId: String? = null)
