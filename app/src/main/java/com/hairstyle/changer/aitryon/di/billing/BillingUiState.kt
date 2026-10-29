package com.hairstyle.changer.aitryon.di.billing

import com.hairstyle.changer.aitryon.ui.premium.PremiumPlan

sealed class BillingUiState {
    data object Idle : BillingUiState()
    data object Loading : BillingUiState()
    data class Success(val plan: PremiumPlan) : BillingUiState()
    /** Play returned at least one active subscription during Restore. */
    data object Restored : BillingUiState()
    /** Restore tapped but no active Play subscription for this account. */
    data object NothingToRestore : BillingUiState()
    data object Cancelled : BillingUiState()
    data class Error(
        val type: Type = Type.GENERIC,
        val productId: String = "",
        val message: String = ""
    ) : BillingUiState() {
        enum class Type {
            PRODUCT_NOT_FOUND,
            NO_OFFER,
            GENERIC
        }
    }
}
