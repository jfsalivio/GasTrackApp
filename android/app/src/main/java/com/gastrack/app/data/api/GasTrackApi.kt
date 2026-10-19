package com.gastrack.app.data.api

import com.gastrack.app.data.models.*
import kotlinx.serialization.json.*
import okhttp3.ResponseBody
import retrofit2.http.*

interface GasTrackApi {
    @GET("health") suspend fun health(): JsonObject
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): LoginDto
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): LoginDto
    @POST("auth/forgot-password") suspend fun forgot(@Body body: EmailRequest): MessageDto
    @GET("auth/reset-password/validate") suspend fun validateReset(@Query("token") token: String): TokenValidationDto
    @POST("auth/reset-password") suspend fun reset(@Body body: ResetPasswordRequest): MessageDto
    @GET("auth/me") suspend fun me(): ActorDto
    @POST("auth/logout") suspend fun logout(): MessageDto
    @GET("dashboard/summary") suspend fun dashboard(): DashboardDto
    @GET("dashboard/low-stock") suspend fun lowStock(): List<LowStockDto>
    @GET("dashboard/top-products") suspend fun topProducts(@Query("days") days: Int = 30, @Query("limit") limit: Int = 10): List<JsonObject>
    @GET("dashboard/activity-log") suspend fun dashboardActivity(@Query("limit") limit: Int = 30): List<ActivityDto>
    @GET("products") suspend fun products(@Query("search") search: String? = null, @Query("category") category: String? = null, @Query("status") status: String? = null, @Query("warehouseId") warehouseId: String? = null): List<ProductDto>
    @GET("products/{id}") suspend fun product(@Path("id") id: String): JsonObject
    @POST("products") suspend fun createProduct(@Body body: ProductRequest): MutationDto
    @PUT("products/{id}") suspend fun updateProduct(@Path("id") id: String, @Body body: ProductRequest): MessageDto
    @DELETE("products/{id}") suspend fun deleteProduct(@Path("id") id: String): MessageDto
    @GET("brands") suspend fun brands(): List<LookupDto>
    @POST("brands") suspend fun createBrand(@Body body: NameRequest): LookupDto
    @PUT("brands/{id}") suspend fun updateBrand(@Path("id") id: String, @Body body: NameRequest): LookupDto
    @DELETE("brands/{id}") suspend fun deleteBrand(@Path("id") id: String): MessageDto
    @GET("categories") suspend fun categories(): List<LookupDto>
    @POST("categories") suspend fun createCategory(@Body body: NameRequest): LookupDto
    @PUT("categories/{id}") suspend fun updateCategory(@Path("id") id: String, @Body body: NameRequest): LookupDto
    @DELETE("categories/{id}") suspend fun deleteCategory(@Path("id") id: String): MessageDto
    @GET("inventory") suspend fun inventory(@Query("warehouseId") warehouseId: String? = null, @Query("status") status: String? = null, @Query("search") search: String? = null): List<InventoryDto>
    @GET("inventory/{id}") suspend fun inventoryItem(@Path("id") id: String): JsonObject
    @GET("inventory/transactions") suspend fun transactions(): List<InventoryTransactionDto>
    @GET("inventory/transfers") suspend fun transfers(): List<JsonObject>
    @POST("inventory/stock-in") suspend fun stockIn(@Body body: StockMovementRequest): MessageDto
    @POST("inventory/stock-out") suspend fun stockOut(@Body body: StockMovementRequest): MessageDto
    @POST("inventory/transfer") suspend fun transfer(@Body body: TransferRequest): MutationDto
    @POST("inventory/adjust") suspend fun adjust(@Body body: StockAdjustmentRequest): MessageDto
    @POST("inventory/import") suspend fun importInventory(@Body body: CsvImportRequest): JsonObject
    @PUT("inventory/{id}") suspend fun updateInventory(@Path("id") id: String, @Body body: InventoryUpdateRequest): MessageDto
    @DELETE("inventory/{id}") suspend fun deleteInventory(@Path("id") id: String, @Query("force") force: Boolean = false): MessageDto
    @GET("warehouses") suspend fun warehouses(): List<LookupDto>
    @GET("customers") suspend fun customers(@Query("search") search: String? = null, @Query("customerType") type: String? = null): List<CustomerDto>
    @GET("customers/{id}") suspend fun customer(@Path("id") id: String): CustomerDto
    @POST("customers") suspend fun createCustomer(@Body body: CustomerRequest): MutationDto
    @PUT("customers/{id}") suspend fun updateCustomer(@Path("id") id: String, @Body body: CustomerRequest): MessageDto
    @DELETE("customers/{id}") suspend fun deleteCustomer(@Path("id") id: String): MessageDto
    @GET("orders") suspend fun orders(@Query("search") search: String? = null, @Query("status") status: String? = null, @Query("type") type: String? = null): List<OrderDto>
    @GET("orders/{id}") suspend fun order(@Path("id") id: String): OrderDto
    @POST("orders") suspend fun createOrder(@Body body: JsonObject): JsonObject
    @PUT("orders/{id}") suspend fun updateOrder(@Path("id") id: String, @Body body: JsonObject): MessageDto
    @DELETE("orders/{id}") suspend fun deleteOrder(@Path("id") id: String): MessageDto
    @GET("orders/pickup") suspend fun pickups(): List<OrderDto>
    @PUT("orders/{id}/pickup-status") suspend fun pickupStatus(@Path("id") id: String, @Body body: PickupStatusRequest): JsonObject
    @GET("orders/{id}/pickup-claim-options") suspend fun pickupClaimOptions(@Path("id") id: String): JsonObject
    @POST("orders/{id}/pickup-claim") suspend fun pickupClaim(@Path("id") id: String, @Body body: CompletionRequest): JsonObject
    @GET("orders/{id}/pickup-settlement") suspend fun pickupSettlement(@Path("id") id: String): JsonObject
    @POST("orders/{id}/pickup-settlement") suspend fun settlePickup(@Path("id") id: String, @Body body: SettlementRequest): JsonObject
    @GET("orders/customer/{customerId}/history") suspend fun customerHistory(@Path("customerId") id: String): List<JsonObject>
    @POST("orders/{id}/refund") suspend fun refund(@Path("id") id: String): JsonObject
    @GET("orders/{id}/refund") suspend fun refundDetails(@Path("id") id: String): JsonObject
    @PUT("orders/{id}/payment") suspend fun orderPayment(@Path("id") id: String, @Body body: OrderPaymentRequest): MessageDto
    @PUT("orders/{id}/delivery") suspend fun legacyOrderDelivery(@Path("id") id: String, @Body body: JsonObject): MessageDto
    @GET("sales") suspend fun sales(@Query("search") search: String? = null, @Query("cashier") cashier: String? = null, @Query("status") status: String? = null): List<SaleDto>
    @GET("sales/{id}") suspend fun sale(@Path("id") id: String): JsonObject
    @POST("sales") suspend fun checkout(@Body body: CheckoutRequest): CheckoutDto
    @POST("sales/import") suspend fun importSales(@Body body: CsvImportRequest): JsonObject
    @DELETE("sales/{id}") suspend fun deleteSale(@Path("id") id: String): MessageDto
    @GET("payments") suspend fun payments(@Query("type") type: String? = null): List<JsonObject>
    @POST("payments") suspend fun payment(@Body body: PaymentRequest): MutationDto
    @GET("deliveries") suspend fun deliveries(): List<DeliveryDto>
    @GET("deliveries/drivers") suspend fun drivers(): List<LookupDto>
    @GET("deliveries/{id}/return-options") suspend fun deliveryReturns(@Path("id") id: String): JsonObject
    @GET("deliveries/{id}/settlement") suspend fun deliverySettlement(@Path("id") id: String): JsonObject
    @PUT("deliveries/{id}") suspend fun assignDelivery(@Path("id") id: String, @Body body: DeliveryAssignmentRequest): MessageDto
    @POST("deliveries/{id}/{action}") suspend fun deliveryAction(@Path("id") id: String, @Path("action") action: String, @Body body: DeliveryActionRequest): JsonObject
    @POST("deliveries/{id}/complete") suspend fun completeDelivery(@Path("id") id: String, @Body body: CompletionRequest): JsonObject
    @POST("deliveries/{id}/settle") suspend fun settleDelivery(@Path("id") id: String, @Body body: SettlementRequest): JsonObject
    @GET("restocking") suspend fun recommendations(@Query("priority") priority: String? = null): List<RestockDto>
    @POST("restocking/generate") suspend fun generateRecommendations(): RestockGenerationDto
    @POST("restocking/rescore") suspend fun rescoreRecommendations(): RestockRescoreDto
    @POST("restocking/{productId}/quick") suspend fun quickRecommendation(@Path("productId") id: String): MutationDto
    @PUT("restocking/{id}") suspend fun updateRecommendation(@Path("id") id: String, @Body body: RestockUpdateRequest): MessageDto
    @DELETE("restocking/{id}") suspend fun deleteRecommendation(@Path("id") id: String): MessageDto
    @GET("purchase-orders/options") suspend fun purchaseOptions(): JsonObject
    @GET("purchase-orders") suspend fun purchaseOrders(): List<PurchaseOrderDto>
    @GET("purchase-orders/{id}") suspend fun purchaseOrder(@Path("id") id: String): JsonObject
    @POST("purchase-orders") suspend fun createPurchaseOrder(@Body body: PurchaseOrderRequest): JsonObject
    @POST("purchase-orders/document-preview") suspend fun previewPurchaseOrder(@Body body: JsonObject): ResponseBody
    @GET("purchase-orders/{id}/document") suspend fun purchaseDocument(@Path("id") id: String): ResponseBody
    @GET("purchase-orders/{id}/payment-document") suspend fun purchasePaymentDocument(@Path("id") id: String): ResponseBody
    @PUT("purchase-orders/{id}/{action}") suspend fun purchaseAction(@Path("id") id: String, @Path("action") action: String): JsonObject
    @PUT("purchase-orders/{id}/send") suspend fun sendPurchase(@Path("id") id: String): MessageDto
    @PUT("purchase-orders/{id}/send-payment") suspend fun sendPurchasePayment(@Path("id") id: String): MessageDto
    @PUT("purchase-orders/{id}/cancel") suspend fun cancelPurchase(@Path("id") id: String): MessageDto
    @PUT("purchase-orders/{id}/confirm") suspend fun confirmPurchase(@Path("id") id: String): JsonObject
    @PUT("purchase-orders/{id}/reject") suspend fun rejectPurchase(@Path("id") id: String): JsonObject
    @PUT("purchase-orders/{id}/dispatch") suspend fun dispatchPurchase(@Path("id") id: String): JsonObject
    @PUT("purchase-orders/{id}/payment") suspend fun purchasePayment(@Path("id") id: String, @Body body: PurchasePaymentRequest): MessageDto
    @PUT("purchase-orders/{id}/receive") suspend fun receivePurchase(@Path("id") id: String, @Body body: PurchaseReceiptRequest): JsonObject
    @GET("suppliers") suspend fun suppliers(@Query("search") search: String? = null, @Query("status") status: String? = null): List<SupplierDto>
    @GET("suppliers/{id}") suspend fun supplier(@Path("id") id: String): SupplierDto
    @POST("suppliers") suspend fun createSupplier(@Body body: SupplierRequest): JsonObject
    @PUT("suppliers/{id}") suspend fun updateSupplier(@Path("id") id: String, @Body body: SupplierRequest): MessageDto
    @DELETE("suppliers/{id}") suspend fun deleteSupplier(@Path("id") id: String): MessageDto
    @GET("suppliers/{id}/products") suspend fun supplierProducts(@Path("id") id: String): List<JsonObject>
    @GET("suppliers/{id}/purchase-orders") suspend fun supplierPurchases(@Path("id") id: String): List<JsonObject>
    @GET("suppliers/{id}/deliveries") suspend fun supplierDeliveries(@Path("id") id: String): List<JsonObject>
    @GET("suppliers/types") suspend fun supplierTypes(): List<JsonObject>
    @POST("suppliers/types") suspend fun createSupplierType(@Body body: NameRequest): JsonObject
    @PUT("suppliers/types/{typeId}") suspend fun updateSupplierType(@Path("typeId") id: String, @Body body: NameRequest): JsonObject
    @DELETE("suppliers/types/{typeId}") suspend fun deleteSupplierType(@Path("typeId") id: String): MessageDto
    @GET("reports") suspend fun reports(): List<ReportDto>
    @GET("reports/summary") suspend fun reportSummary(): ReportSummaryDto
    @POST("reports") suspend fun createReport(@Body body: ReportRequest): JsonObject
    @POST("reports/{id}/generate") suspend fun generateReport(@Path("id") id: String): CsvFileDto
    @PUT("reports/{id}/submit") suspend fun submitReport(@Path("id") id: String): MessageDto
    @DELETE("reports/{id}") suspend fun deleteReport(@Path("id") id: String): MessageDto
    @GET("data/logs") suspend fun dataLogs(@Query("activityType") type: String? = null): List<DataLogDto>
    @POST("data/export") suspend fun export(@Body body: ExportRequest): ExportDto
    @POST("data/import") suspend fun logImport(@Body body: ImportLogRequest): MutationDto
    @GET("users") suspend fun users(@Query("search") search: String? = null, @Query("role") role: String? = null, @Query("status") status: String? = null, @Query("branch") branch: String? = null): List<ManagedUserDto>
    @GET("users/{id}") suspend fun user(@Path("id") id: String): ManagedUserDto
    @POST("users") suspend fun createUser(@Body body: UserRequest): JsonObject
    @PUT("users/{id}") suspend fun updateUser(@Path("id") id: String, @Body body: JsonObject): MessageDto
    @DELETE("users/{id}") suspend fun deleteUser(@Path("id") id: String): MessageDto
    @GET("users/activity-log") suspend fun userActivity(@QueryMap filters: Map<String, String> = emptyMap()): ActivityPageDto
    @GET("roles") suspend fun roles(): List<LookupDto>
    @GET("user-settings/{kind}") suspend fun userSettings(@Path("kind") kind: String): List<LookupDto>
    @POST("user-settings/{kind}") suspend fun createUserSetting(@Path("kind") kind: String, @Body body: UserSettingsRequest): JsonObject
    @PUT("user-settings/{kind}/{id}") suspend fun updateUserSetting(@Path("kind") kind: String, @Path("id") id: String, @Body body: UserSettingsRequest): JsonObject
    @DELETE("user-settings/{kind}/{id}") suspend fun deleteUserSetting(@Path("kind") kind: String, @Path("id") id: String): JsonObject
    @GET("settings") suspend fun settings(): SettingsDto
    @PUT("settings/profile") suspend fun profile(@Body body: CompanyProfileRequest): MessageDto
    @PUT("settings/logo") suspend fun logo(@Body body: LogoRequest): MessageDto
    @PUT("settings/operations") suspend fun operations(@Body body: OperationsRequest): MessageDto
    @PUT("settings/tax") suspend fun tax(@Body body: TaxRequest): MessageDto
    @PUT("settings/receipt") suspend fun receipt(@Body body: ReceiptRequest): MessageDto
    @PUT("settings/system") suspend fun system(@Body body: SystemRequest): MessageDto
    @PUT("settings/vehicle-rates") suspend fun vehicleRates(@Body body: VehicleRatesRequest): JsonObject
}
