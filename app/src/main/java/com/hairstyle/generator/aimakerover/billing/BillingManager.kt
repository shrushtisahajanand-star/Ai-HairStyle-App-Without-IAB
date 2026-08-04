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

    private val packageByProductId = mutableMapOf<String, RcPackage>()

    private var offeringsLoading = false

    fun startConnection() {
        if (offeringsLoading) return
        offeringsLoading = true
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                offeringsLoading = false
                // Log.e(TAG, "getOfferings failed: code=${error.code} msg=${error.message}")
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
                Log.d(TAG, "purchasePackage: " + customerInfo.originalAppUserId)
                Log.d(TAG, "purchasePackage: " + customerInfo)
                Log.d(TAG, "purchasePackage: " + customerInfo)
                val productId = storeTransaction?.productIds?.firstOrNull()
                    ?: PremiumPlanManager.getProductId(plan)
                storeTransaction?.purchaseToken?.let { PrefManager.setPremiumPurchaseToken(it) }
                PrefManager.setPremiumProductId(productId)
                val unlocked = PremiumPlanManager.planForProductId(productId) ?: plan
                if (PremiumPlanManager.isPlanActive(unlocked) ||
                    customerInfo.entitlements[RevenueCatConfig.ENTITLEMENT_ID]?.isActive == true
                ) {
                    if (!PremiumPlanManager.isPlanActive(unlocked)) {
                        PremiumPlanManager.activatePlan(unlocked)
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
            },
            onSuccess = { customerInfo ->
                syncFromCustomerInfo(customerInfo, notifyUi = false)
                notifyEntitlementChanged()
            }
        )
    }

    /**
     * Source of truth = RevenueCat entitlement [RevenueCatConfig.ENTITLEMENT_ID].
     * Maps active product identifier(s) onto local [PremiumPlanManager] plans.
     */
    private fun syncFromCustomerInfo(customerInfo: CustomerInfo, notifyUi: Boolean) {
        val beforePremium = PremiumPlanManager.isPremiumActive()
        val entitlement = customerInfo.entitlements[RevenueCatConfig.ENTITLEMENT_ID]
        val activePlans = linkedSetOf<PremiumPlan>()
        var lastActivated: PremiumPlan? = null

        customerInfo.originalAppUserId.takeIf { it.isNotBlank() }?.let {
            PrefManager.setRcOriginalAppUserId(it)
        }

        if (entitlement?.isActive == true) {
            val productIds = LinkedHashSet<String>().apply {
                addAll(customerInfo.activeSubscriptions)
                entitlement.productIdentifier.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            productIds.forEach { productId ->
                val plan = PremiumPlanManager.planForProductId(productId) ?: return@forEach
                if (!PremiumPlanManager.isPlanActive(plan)) {
                    PremiumPlanManager.activatePlan(plan)
                }
                PrefManager.setPremiumProductId(productId)
                activePlans.add(plan)
                lastActivated = plan
            }
            // Entitlement active but product id unknown to our SKUs â€” still unlock a plan
            if (activePlans.isEmpty()) {
                val fallbackId = entitlement.productIdentifier
                val plan = PremiumPlanManager.planForProductId(fallbackId)
                    ?: PremiumPlan.YEARLY
                PremiumPlanManager.activatePlan(plan)
                PrefManager.setPremiumProductId(fallbackId)
                activePlans.add(plan)
                lastActivated = plan
            }

            // Prefer RevenueCat purchase / expiry when available (API + local record).
            val purchaseMs = entitlement.latestPurchaseDate?.time
                ?: entitlement.originalPurchaseDate?.time
            val expiryMs = entitlement.expirationDate?.time
            if (purchaseMs != null || expiryMs != null) {
                PremiumPlanManager.updatePrimaryPlanDates(
                    purchaseMs = purchaseMs,
                    expiryMs = expiryMs
                )
            }
        }

        PremiumPlanManager.retainOnlyPlayActivePlans(activePlans)
        val afterPremium = PremiumPlanManager.isPremiumActive()
        // Log.d(
            // TAG,
            // "RC sync: activePlans=$activePlans beforePremium=$beforePremium afterPremium=$afterPremium"
        // )

        if (notifyUi && lastActivated != null && afterPremium) {
            _billingState.value = BillingUiState.Success(lastActivated!!)
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
    }
}
