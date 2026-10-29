package com.hairstyle.changer.aitryon.di.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.hairstyle.changer.aitryon.ui.premium.PremiumPlan
import com.hairstyle.changer.aitryon.utils.PrefManager
import com.hairstyle.changer.aitryon.utils.PremiumPlanManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Billing (subscriptions) — talks to Play directly, no third-party SDK.
 *
 * Source of truth for "is premium" = purchases returned by Play for this account
 * ([BillingClient.queryPurchasesAsync]); the local plan records in [PremiumPlanManager]
 * are kept in sync with it.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _billingState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val billingState: StateFlow<BillingUiState> = _billingState.asStateFlow()

    private val _productsLoaded = MutableStateFlow(0)
    val productsLoaded: StateFlow<Int> = _productsLoaded.asStateFlow()

    private val _entitlementRevision = MutableStateFlow(0)
    val entitlementRevision: StateFlow<Int> = _entitlementRevision.asStateFlow()

    private val _isInitialSyncComplete = MutableStateFlow(false)
    val isInitialSyncComplete: StateFlow<Boolean> = _isInitialSyncComplete.asStateFlow()

    private val productDetailsById = mutableMapOf<String, ProductDetails>()

    private var productsLoading = false
    private var connecting = false

    /** Work to run as soon as the BillingClient is connected. */
    private val pendingWork = mutableListOf<() -> Unit>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _billingState.value = BillingUiState.Error(message = "Purchase failed")
                } else {
                    handlePurchases(purchases, fromPurchaseFlow = true)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _billingState.value = BillingUiState.Cancelled
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Already subscribed (e.g. purchased on another device / reinstall): just sync.
                querySubscriptions(fromPurchaseFlow = true)
            }
            else -> _billingState.value = mapBillingError(result, "")
        }
    }

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    /** Refresh entitlement from Play without loading products (safe to call often). */
    fun refreshEntitlements() {
        runWhenConnected { querySubscriptions(fromPurchaseFlow = false) }
    }

    fun startConnection() {
        runWhenConnected {
            querySubscriptions(fromPurchaseFlow = false)
            loadProducts(onLoaded = null)
        }
    }

    fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        val productId = PremiumPlanManager.getProductId(plan)
        _billingState.value = BillingUiState.Loading
        runWhenConnected(onUnavailable = {
            _billingState.value = BillingUiState.Error(message = "Google Play Billing is unavailable")
        }) {
            val cached = findProductDetails(productId)
            if (cached != null) {
                launchBillingFlow(activity, productId, cached)
            } else {
                loadProducts(onLoaded = {
                    val loaded = findProductDetails(productId)
                    if (loaded == null) {
                        _billingState.value = BillingUiState.Error(
                            type = BillingUiState.Error.Type.PRODUCT_NOT_FOUND,
                            productId = productId
                        )
                    } else {
                        launchBillingFlow(activity, productId, loaded)
                    }
                })
            }
        }
    }

    private fun launchBillingFlow(activity: Activity, productId: String, details: ProductDetails) {
        val offer = pickPurchaseOffer(details)
        if (offer == null) {
            _billingState.value = BillingUiState.Error(
                type = BillingUiState.Error.Type.NO_OFFER,
                productId = productId
            )
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            _billingState.value = BillingUiState.Idle
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingState.value = mapBillingError(result, productId)
        }
        // On OK the outcome arrives in purchasesUpdatedListener (state stays Loading).
    }

    /**
     * Restore = re-query Play for this account's subscriptions. Call from Restore button only.
     */
    fun restorePurchases() {
        _billingState.value = BillingUiState.Loading
        runWhenConnected(onUnavailable = {
            _billingState.value = BillingUiState.Error(message = "Restore failed")
        }) {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { result, purchases ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _billingState.value = BillingUiState.Error(
                        message = result.debugMessage.ifBlank { "Restore failed" }
                    )
                    return@queryPurchasesAsync
                }
                syncFromPurchases(purchases)
                _billingState.value = if (PremiumPlanManager.isPremiumActive()) {
                    BillingUiState.Restored
                } else {
                    BillingUiState.NothingToRestore
                }
                notifyEntitlementChanged()
            }
        }
    }

    fun getStorePrice(plan: PremiumPlan): String? = getStorePriceInfo(plan)?.formattedPrice

    fun getStorePriceInfo(plan: PremiumPlan): StorePriceInfo? {
        val productId = PremiumPlanManager.getProductId(plan)
        val details = findProductDetails(productId) ?: return null
        val phase = pickBasePricingPhase(details) ?: return null
        return StorePriceInfo(
            formattedPrice = phase.formattedPrice,
            amountMicros = phase.priceAmountMicros,
            currencyCode = phase.priceCurrencyCode
        )
    }

    data class StorePriceInfo(
        val formattedPrice: String,
        val amountMicros: Long,
        val currencyCode: String
    )

    fun resetState() {
        _billingState.value = BillingUiState.Idle
    }

    // ── DEBUG helpers (used only from PremiumViewModel when BuildConfig.DEBUG) ──

    fun emitDebugPurchaseSuccess(plan: PremiumPlan) {
        _billingState.value = BillingUiState.Success(plan)
    }

    fun emitDebugRestoreSuccess() {
        _billingState.value = BillingUiState.Restored
    }

    fun emitDebugNothingToRestore() {
        _billingState.value = BillingUiState.NothingToRestore
    }

    fun endConnection() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    // ── Connection ──────────────────────────────────────────────────────────────

    private fun runWhenConnected(onUnavailable: (() -> Unit)? = null, work: () -> Unit) {
        if (billingClient.isReady) {
            work()
            return
        }
        pendingWork.add(work)
        if (connecting) return
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                val queued = pendingWork.toList()
                pendingWork.clear()
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queued.forEach { it() }
                } else {
                    // Billing unavailable (no Play Store, old Play services, offline...).
                    _isInitialSyncComplete.value = true
                    onUnavailable?.invoke()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                // Auto service reconnection is enabled; next call reconnects if needed.
            }
        })
    }

    // ── Products ────────────────────────────────────────────────────────────────

    private fun loadProducts(onLoaded: (() -> Unit)?) {
        if (productsLoading) {
            // A load is already in flight; piggy-back the continuation on it.
            onLoaded?.let { pendingAfterProducts.add(it) }
            return
        }
        productsLoading = true
        onLoaded?.let { pendingAfterProducts.add(it) }

        val products = productIdsToQuery().map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(products).build()
        ) { result, queryResult ->
            productsLoading = false
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsById.clear()
                queryResult.productDetailsList.forEach { productDetailsById[it.productId] = it }
                _productsLoaded.value += 1
            } else if (_billingState.value is BillingUiState.Loading &&
                pendingAfterProducts.isNotEmpty()
            ) {
                _billingState.value = mapBillingError(result, "")
            }
            val continuations = pendingAfterProducts.toList()
            pendingAfterProducts.clear()
            continuations.forEach { it() }
        }
    }

    private val pendingAfterProducts = mutableListOf<() -> Unit>()

    private fun productIdsToQuery(): List<String> =
        PremiumPlan.entries.map { PremiumPlanManager.getProductId(it) }.distinct()

    private fun findProductDetails(productId: String): ProductDetails? {
        if (productId.isBlank()) return null
        return productDetailsById[productId]
            ?: productDetailsById[productId.substringBefore(':')]
    }

    /** Base plan (no promo) recurring price — what the paywall displays. */
    private fun pickBasePricingPhase(details: ProductDetails): ProductDetails.PricingPhase? {
        val offers = details.subscriptionOfferDetails.orEmpty()
        val base = offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
        return base?.pricingPhases?.pricingPhaseList?.lastOrNull()
    }

    /** Free-trial offer when the user is eligible, otherwise the plain base plan. */
    private fun pickPurchaseOffer(details: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = details.subscriptionOfferDetails.orEmpty()
        if (offers.isEmpty()) return null
        val trial = offers.firstOrNull { offer ->
            offer.offerId != null &&
                offer.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros == 0L
        }
        return trial ?: offers.firstOrNull { it.offerId == null } ?: offers.first()
    }

    // ── Purchases / entitlement ─────────────────────────────────────────────────

    private fun querySubscriptions(fromPurchaseFlow: Boolean) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases, fromPurchaseFlow)
            } else {
                // Could not reach Play: keep whatever premium state we already have.
                if (fromPurchaseFlow) {
                    _billingState.value = mapBillingError(result, "")
                }
            }
            _isInitialSyncComplete.value = true
        }
    }

    private fun handlePurchases(purchases: List<Purchase>, fromPurchaseFlow: Boolean) {
        val purchased = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }

        purchased.filterNot { it.isAcknowledged }.forEach { acknowledge(it) }

        val unlocked = syncFromPurchases(purchases)
        notifyEntitlementChanged()

        if (fromPurchaseFlow) {
            _billingState.value = when {
                unlocked != null -> BillingUiState.Success(unlocked)
                pending -> BillingUiState.Error(
                    message = "Your payment is pending. Premium will unlock once it is approved."
                )
                else -> BillingUiState.Error(message = "Purchase completed but could not be verified")
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) {
            // A failed acknowledge is retried on the next sync: queryPurchases keeps returning the
            // purchase as un-acknowledged, and Play refunds un-acknowledged ones after 3 days.
        }
    }

    /**
     * Makes local plan records match [purchases]. Returns the highest-tier unlocked plan, or
     * null when Play reports no active subscription.
     */
    private fun syncFromPurchases(purchases: List<Purchase>): PremiumPlan? {
        val now = System.currentTimeMillis()
        val activePlans = linkedSetOf<PremiumPlan>()
        var primary: PremiumPlan? = null

        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                val productId = purchase.products.firstOrNull { PremiumPlanManager.planForProductId(it) != null }
                    ?: return@forEach
                val plan = PremiumPlanManager.planForProductId(productId) ?: return@forEach
                val purchaseMs = purchase.purchaseTime.takeIf { it > 0L }
                val expiryMs = estimateExpiryMs(plan, purchaseMs, now)

                if (!PremiumPlanManager.isPlanActive(plan)) {
                    PremiumPlanManager.activatePlan(plan, purchaseMs = purchaseMs, expiryMs = expiryMs)
                } else {
                    PremiumPlanManager.syncPlanDatesFromServer(
                        serverPlanKey = planKeyFor(plan),
                        purchaseMs = purchaseMs,
                        expiryMs = expiryMs
                    )
                }
                PrefManager.setPremiumProductId(productId)
                PrefManager.setPremiumPurchaseToken(purchase.purchaseToken)
                if (PrefManager.getPurchaseId().isBlank()) {
                    // Sent to the backend as `purchase_id`; existing subscribers keep their old id.
                    PrefManager.setPurchaseId(purchase.purchaseToken)
                }
                activePlans.add(plan)
                if (primary == null || plan.ordinal < primary!!.ordinal) primary = plan
            }

        // Empty set = Play has no active subscription for this account → clear local premium.
        PremiumPlanManager.retainOnlyPlayActivePlans(activePlans)
        return primary
    }

    /**
     * Play does not expose the renewal date to the client, so derive the end of the current
     * billing period from the original purchase time + the plan length.
     */
    private fun estimateExpiryMs(plan: PremiumPlan, purchaseMs: Long?, now: Long): Long {
        val periodMs = PremiumPlanManager.planDays(plan) * DAY_MS
        val start = purchaseMs ?: return now + periodMs
        if (start > now) return start + periodMs
        val elapsedPeriods = (now - start) / periodMs
        return start + (elapsedPeriods + 1) * periodMs
    }

    private fun planKeyFor(plan: PremiumPlan): String = when (plan) {
        PremiumPlan.YEARLY -> "year"
        PremiumPlan.MONTHLY -> "month"
        PremiumPlan.WEEKLY -> "week"
    }

    private fun mapBillingError(result: BillingResult, productId: String): BillingUiState.Error {
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingUiState.Error(
                type = BillingUiState.Error.Type.PRODUCT_NOT_FOUND,
                productId = productId,
                message = result.debugMessage
            )
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> BillingUiState.Error(
                type = BillingUiState.Error.Type.NO_OFFER,
                productId = productId,
                message = result.debugMessage
            )
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR -> BillingUiState.Error(
                message = "Network error. Please check your connection and try again."
            )
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> BillingUiState.Error(
                message = "Google Play Billing is unavailable on this device"
            )
            else -> BillingUiState.Error(
                message = result.debugMessage.ifBlank { "Purchase failed" }
            )
        }
    }

    private fun notifyEntitlementChanged() {
        _entitlementRevision.value = _entitlementRevision.value + 1
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
