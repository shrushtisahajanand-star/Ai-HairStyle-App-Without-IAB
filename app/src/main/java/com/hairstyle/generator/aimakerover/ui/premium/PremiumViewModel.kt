package com.hairstyle.generator.aimakerover.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
// import com.hairstyle.generator.aimakeover.BuildConfig
// import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.billing.BillingManager
import com.hairstyle.generator.aimakerover.billing.BillingUiState
// import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val billingState: StateFlow<BillingUiState> = billingManager.billingState
    val productsLoaded: StateFlow<Int> = billingManager.productsLoaded

    init {
        billingManager.startConnection()
    }

    fun purchase(activity: Activity, plan: PremiumPlan) {
        // DEBUG IAB test (local premium activate — uncomment to test without Play Billing):
        // if (BuildConfig.DEBUG) {
        //     PremiumPlanManager.activatePlan(plan)
        //     ADSMainClass.setAds_Free(true)
        //     billingManager.emitDebugPurchaseSuccess(plan)
        //     return
        // }
        billingManager.launchPurchase(activity, plan)
    }

    fun restorePurchases() {
        // DEBUG IAB test (local restore — uncomment to test without Play Billing):
        // if (BuildConfig.DEBUG) {
        //     if (PremiumPlanManager.isPremiumActive()) {
        //         ADSMainClass.setAds_Free(true)
        //         billingManager.emitDebugRestoreSuccess()
        //     } else {
        //         billingManager.emitDebugNothingToRestore()
        //     }
        //     return
        // }
        billingManager.restorePurchases()
    }

    fun getStorePrice(plan: PremiumPlan): String? = billingManager.getStorePrice(plan)

    fun getStorePriceInfo(plan: PremiumPlan): BillingManager.StorePriceInfo? =
        billingManager.getStorePriceInfo(plan)

    fun resetBillingState() {
        billingManager.resetState()
    }

    override fun onCleared() {
        billingManager.resetState()
        super.onCleared()
    }
}
