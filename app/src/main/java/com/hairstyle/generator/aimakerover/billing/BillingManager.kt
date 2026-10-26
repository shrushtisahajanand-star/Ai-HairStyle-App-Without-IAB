package com.hairstyle.generator.aimakerover.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.hairstyle.generator.aimakerover.ui.premium.PremiumPlan
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package as RcPackage
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @Suppress("unused")
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

    private val packageByProductId = mutableMapOf<String, RcPackage>()

    private var offeringsLoading = false

    /** Last time CustomerInfo reported an active premium SKU (avoids empty-sync races). */
    @Volatile
    private var lastPositiveRcSyncAtMs: Long = 0L

    /** Refresh Play / RevenueCat entitlement without loading offerings (safe to call often). */
    fun refreshEntitlements() {
        syncCustomerInfoSilent()
    }

    fun startConnection() {
        refreshEntitlements()
        if (offeringsLoading) return
        offeringsLoading = true
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                offeringsLoading = false
                // Log.e(TAG, "getOfferings failed: code=${error.code} msg=${error.message}")
                _isInitialSyncComplete.value = true
            },
            onSuccess = { offerings ->
                offeringsLoading = false
                cachePackagesFromOfferings(offerings)
                syncCustomerInfoSilent()
            }
        )
    }

    fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        val productId = PremiumPlanManager.getProductId(plan)
        val pkg = findPackageForProductId(productId)
        if (pkg == null) {
            _billingState.value = BillingUiState.Loading
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    // Log.e(TAG, "getOfferings (purchase) failed: ${error.message}")
                    _billingState.value = BillingUiState.Error(
                        message = error.message.ifBlank { "Unable to load products" }
                    )
                },
                onSuccess = { offerings ->
                    cachePackagesFromOfferings(offerings)
                    val refreshed = findPackageForProductId(productId)
                    if (refreshed == null) {
                        // Log.e(
                            // TAG,
                            // "PRODUCT_NOT_FOUND looking for='$productId' " +
                                // "cachedKeys=${packageByProductId.keys} " +
                                // "currentOffering=${offerings.current?.identifier} " +
                                // "allOfferings=${offerings.all.keys}"
                        // )
                        _billingState.value = BillingUiState.Error(
                            type = BillingUiState.Error.Type.PRODUCT_NOT_FOUND,
                            productId = productId
                        )
                    } else {
                        purchasePackage(activity, plan, refreshed)
                    }
                }
            )
            return
        }
        purchasePackage(activity, plan, pkg)
    }

    private fun purchasePackage(activity: Activity, plan: PremiumPlan, pkg: RcPackage) {
        _billingState.value = BillingUiState.Loading
        val params = PurchaseParams.Builder(activity, pkg).build()
        Purchases.sharedInstance.purchaseWith(
            params,
            onError = { error, userCancelled ->
                if (userCancelled) {
                    _billingState.value = BillingUiState.Cancelled
                } else {
                    _billingState.value = mapPurchaseError(error, PremiumPlanManager.getProductId(plan))
                }
            },
            onSuccess = { storeTransaction, customerInfo ->
                syncFromCustomerInfo(customerInfo, notifyUi = false)
                //Log.d(TAG, "purchasePackage: uid=${customerInfo.originalAppUserId}")
                //Log.d(
//                    TAG,
//                    "purchasePackage: activeSubs=${customerInfo.activeSubscriptions} " +
//                        "entitlements=${customerInfo.entitlements.active.keys}"
//                )
                //Log.d(
//                    TAG,
//                    "purchasePackage: allExpirationDatesByProduct=${customerInfo.allExpirationDatesByProduct} " +
//                            "entitlements=${customerInfo.latestExpirationDate}"
//                )
                val productId = storeTransaction?.productIds?.firstOrNull()
                    ?: PremiumPlanManager.getProductId(plan)
                storeTransaction?.purchaseToken?.let { PrefManager.setPremiumPurchaseToken(it) }
                PrefManager.setPremiumProductId(productId)
                val unlocked = PremiumPlanManager.planForProductId(productId) ?: plan
                val entitlement = resolveActiveEntitlement(customerInfo)
                val entitlementActive = entitlement?.isActive == true
                val subActive = customerInfo.activeSubscriptions.any { subId ->
                    PremiumPlanManager.planForProductId(subId) != null
                }
                if (PremiumPlanManager.isPlanActive(unlocked) || entitlementActive || subActive) {
                    if (!PremiumPlanManager.isPlanActive(unlocked)) {
                        PremiumPlanManager.activatePlan(
                            unlocked,
                            purchaseMs = entitlement?.latestPurchaseDate?.time
                                ?: entitlement?.originalPurchaseDate?.time,
                            expiryMs = entitlement?.expirationDate?.time
                        )
                    }
                    _billingState.value = BillingUiState.Success(unlocked)
                } else {
                    _billingState.value = BillingUiState.Error(
                        message = "Purchase completed but entitlement is not active"
                    )
                }
                notifyEntitlementChanged()
            }
        )
    }

    /**
     * Restores Play purchases via RevenueCat. Call from Restore button only.
     */
    fun restorePurchases() {
        _billingState.value = BillingUiState.Loading
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                _billingState.value = BillingUiState.Error(
                    message = error.message.ifBlank { "Restore failed" }
                )
            },
            onSuccess = { customerInfo ->
                syncFromCustomerInfo(customerInfo, notifyUi = false)
                _billingState.value = if (PremiumPlanManager.isPremiumActive()) {
                    BillingUiState.Restored
                } else {
                    BillingUiState.NothingToRestore
                }
                notifyEntitlementChanged()
            }
        )
    }

    fun getStorePrice(plan: PremiumPlan): String? = getStorePriceInfo(plan)?.formattedPrice

    fun getStorePriceInfo(plan: PremiumPlan): StorePriceInfo? {
        val productId = PremiumPlanManager.getProductId(plan)
        val price = findPackageForProductId(productId)?.product?.price ?: return null
        return StorePriceInfo(
            formattedPrice = price.formatted,
            amountMicros = price.amountMicros,
            currencyCode = price.currencyCode
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

    // â”€â”€ DEBUG helpers (used only from PremiumViewModel when BuildConfig.DEBUG) â”€â”€

    fun emitDebugPurchaseSuccess(plan: PremiumPlan) {
        _billingState.value = BillingUiState.Success(plan)
    }

    fun emitDebugRestoreSuccess() {
        _billingState.value = BillingUiState.Restored
    }

    fun emitDebugNothingToRestore() {
        _billingState.value = BillingUiState.NothingToRestore
    }

    /** No BillingClient connection to tear down; kept for API compatibility. */
    fun endConnection() {
        // no-op
    }

    private fun cachePackagesFromOfferings(offerings: com.revenuecat.purchases.Offerings) {
        packageByProductId.clear()
        val current = offerings.current
        if (current == null) {
            // Log.e(
                // TAG,
                // "No CURRENT offering set in RevenueCat. allOfferings=${offerings.all.keys}. " +
                    // "Dashboard â†’ Offerings â†’ mark one as Current."
            // )
        }
        val packages = current?.availablePackages
            ?: offerings.all.values.firstOrNull()?.availablePackages.orEmpty()

        if (packages.isEmpty()) {
            // Log.e(TAG, "Offering has 0 packages. Add weekly/monthly/yearly packages in RC dashboard.")
        }

        packages.forEach { pkg ->
            val storeId = pkg.product.id
            // Log.d(
                // TAG,
                // "offering package: pkg=${pkg.identifier} storeProductId=$storeId " +
                    // "type=${pkg.packageType}"
            // )
            // Exact id (may be "premium_week" or "premium_week:basePlan")
            packageByProductId[storeId] = pkg
            // Also index by Play subscription product id (before ':')
            val baseProductId = storeId.substringBefore(':')
            if (baseProductId.isNotBlank() && baseProductId != storeId) {
                packageByProductId.putIfAbsent(baseProductId, pkg)
            }
        }
        // Log.d(
            // TAG,
            // "cached product keys=${packageByProductId.keys} " +
                // "expected=${BillingProductIds.allProductIds()}"
        // )
        _productsLoaded.value += 1
    }

    /**
     * Match app product id (`premium_week`) to RC package even when store id is
     * `premium_week:base-plan-id`.
     */
    private fun findPackageForProductId(productId: String): RcPackage? {
        if (productId.isBlank()) return null
        packageByProductId[productId]?.let { return it }
        return packageByProductId.entries.firstOrNull { (key, _) ->
            key.substringBefore(':') == productId
        }?.value
    }

    private fun syncCustomerInfoSilent() {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                // Log.w(TAG, "getCustomerInfo failed: ${error.message}")
                _isInitialSyncComplete.value = true
            },
            onSuccess = { customerInfo ->
                syncFromCustomerInfo(customerInfo, notifyUi = false)
                notifyEntitlementChanged()
                _isInitialSyncComplete.value = true
            }
        )
    }

    /**
     * Source of truth = RevenueCat entitlement [RevenueCatConfig.ENTITLEMENT_ID],
     * with fallbacks so a dashboard Identifier mismatch cannot wipe a valid
     * Play subscription for [BillingProductIds].
     */
    private fun syncFromCustomerInfo(customerInfo: CustomerInfo, notifyUi: Boolean) {
        val beforePremium = PremiumPlanManager.isPremiumActive()
        val entitlement = resolveActiveEntitlement(customerInfo)
        val activePlans = linkedSetOf<PremiumPlan>()
        var lastActivated: PremiumPlan? = null

        customerInfo.originalAppUserId.takeIf { it.isNotBlank() }?.let {
            PrefManager.setRcOriginalAppUserId(it)
        }

        val purchaseMs = entitlement?.latestPurchaseDate?.time
            ?: entitlement?.originalPurchaseDate?.time
        val expiryMs = entitlement?.expirationDate?.time

        val productIds = LinkedHashSet<String>().apply {
            addAll(customerInfo.activeSubscriptions)
            entitlement?.productIdentifier?.takeIf { it.isNotBlank() }?.let { add(it) }
        }

        productIds.forEach { productId ->
            val plan = PremiumPlanManager.planForProductId(productId) ?: return@forEach
            if (!PremiumPlanManager.isPlanActive(plan)) {
                PremiumPlanManager.activatePlan(plan, purchaseMs = purchaseMs, expiryMs = expiryMs)
            } else if (purchaseMs != null || expiryMs != null) {
                // Refresh Started/Expires for Active Subscription UI from RC.
                PremiumPlanManager.updatePrimaryPlanDates(purchaseMs, expiryMs)
            }
            PrefManager.setPremiumProductId(productId)
            activePlans.add(plan)
            lastActivated = plan
        }

        // Named entitlement active but SKU not in our map — still unlock.
        if (activePlans.isEmpty() && entitlement?.isActive == true) {
            val fallbackId = entitlement.productIdentifier
            val plan = PremiumPlanManager.planForProductId(fallbackId)
                ?: PremiumPlan.YEARLY
            if (!PremiumPlanManager.isPlanActive(plan)) {
                PremiumPlanManager.activatePlan(plan, purchaseMs = purchaseMs, expiryMs = expiryMs)
            }
            PrefManager.setPremiumProductId(fallbackId)
            activePlans.add(plan)
            lastActivated = plan
        }

        if (activePlans.isEmpty()) {
            val configuredEntitlement =
                customerInfo.entitlements[RevenueCatConfig.ENTITLEMENT_ID]
            val definitivelyExpired = customerInfo.activeSubscriptions.isEmpty() &&
                (
                    customerInfo.entitlements.active.isEmpty() ||
                        configuredEntitlement?.isActive == false
                    )
            if (definitivelyExpired) {
                PremiumPlanManager.retainOnlyPlayActivePlans(emptySet())
            } else {
                // Transient empty CustomerInfo (race after a positive sync in same session).
                val sincePositive = System.currentTimeMillis() - lastPositiveRcSyncAtMs
                if (beforePremium && sincePositive in 0 until EMPTY_SYNC_GRACE_MS) {
                    //Log.w(
//                        TAG,
//                        "RC sync: ignoring empty CustomerInfo within ${EMPTY_SYNC_GRACE_MS}ms " +
//                            "of positive sync (race)"
//                    )
                    return
                }
                PremiumPlanManager.retainOnlyPlayActivePlans(emptySet())
            }
        } else {
            lastPositiveRcSyncAtMs = System.currentTimeMillis()
            PremiumPlanManager.retainOnlyPlayActivePlans(activePlans)
            if (purchaseMs != null || expiryMs != null) {
                PremiumPlanManager.updatePrimaryPlanDates(
                    purchaseMs = purchaseMs,
                    expiryMs = expiryMs
                )
            }
            // Past RC expirationDate used to prune local plans while entitlement stayed active.
            activePlans.forEach { plan ->
                if (!PremiumPlanManager.isPlanActive(plan)) {
                    PremiumPlanManager.activatePlan(
                        plan,
                        purchaseMs = purchaseMs,
                        expiryMs = expiryMs
                    )
                }
            }
        }

        val afterPremium = PremiumPlanManager.isPremiumActive()
        //Log.d(
//            TAG,
//            "RC sync: entitlement=${entitlement?.identifier} " +
//                "activeSubs=${customerInfo.activeSubscriptions} " +
//                "rcExpiryMs=$expiryMs " +
//                "activePlans=$activePlans before=$beforePremium after=$afterPremium " +
//                "allEntitlements=${customerInfo.entitlements.active.keys}"
//        )

        if (notifyUi && lastActivated != null && afterPremium) {
            _billingState.value = BillingUiState.Success(lastActivated!!)
        }
    }

    /**
     * Prefer configured entitlement id; otherwise any active entitlement whose product
     * maps to our Play SKUs (covers Identifier vs Display Name mistakes in dashboard).
     */
    private fun resolveActiveEntitlement(
        customerInfo: CustomerInfo
    ): com.revenuecat.purchases.EntitlementInfo? {
        val configured = customerInfo.entitlements[RevenueCatConfig.ENTITLEMENT_ID]
        if (configured?.isActive == true) return configured

        return customerInfo.entitlements.active.values.firstOrNull { info ->
            PremiumPlanManager.planForProductId(info.productIdentifier) != null
        }
    }

    private fun mapPurchaseError(error: PurchasesError, productId: String): BillingUiState.Error {
        return when (error.code) {
            PurchasesErrorCode.ProductNotAvailableForPurchaseError -> BillingUiState.Error(
                type = BillingUiState.Error.Type.PRODUCT_NOT_FOUND,
                productId = productId,
                message = error.message
            )
            PurchasesErrorCode.PurchaseInvalidError -> BillingUiState.Error(
                type = BillingUiState.Error.Type.NO_OFFER,
                productId = productId,
                message = error.message
            )
            else -> BillingUiState.Error(
                message = error.message.ifBlank { "Purchase failed" }
            )
        }
    }

    private fun notifyEntitlementChanged() {
        _entitlementRevision.value = _entitlementRevision.value + 1
    }

    companion object {
        private const val TAG = "BillingManager"
        /** Ignore empty RC payloads shortly after a positive sync (multi startConnection race). */
        private const val EMPTY_SYNC_GRACE_MS = 60_000L
    }
}
