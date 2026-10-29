package com.hairstyle.changer.aitryon.di.billing

import com.hairstyle.changer.aitryon.ui.premium.PremiumPlan

/**
 * Google Play subscription product IDs (must match Play Console).
 * Defaults used when Remote Config does not override `product_id`.
 */
object BillingProductIds {

    const val YEARLY = "premium_year"
    const val MONTHLY = "premium_month"
    const val WEEKLY = "premium_week"

    fun productIdFor(plan: PremiumPlan): String = when (plan) {
        PremiumPlan.YEARLY -> YEARLY
        PremiumPlan.MONTHLY -> MONTHLY
        PremiumPlan.WEEKLY -> WEEKLY
    }

    fun planFor(productId: String): PremiumPlan? = when (productId) {
        YEARLY -> PremiumPlan.YEARLY
        MONTHLY -> PremiumPlan.MONTHLY
        WEEKLY -> PremiumPlan.WEEKLY
        else -> null
    }

    fun allProductIds(): List<String> = listOf(YEARLY, MONTHLY, WEEKLY)
}
