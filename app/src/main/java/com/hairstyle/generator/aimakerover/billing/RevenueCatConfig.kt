package com.hairstyle.generator.aimakerover.billing

/**
 * RevenueCat dashboard identifiers.
 *
 * [ENTITLEMENT_ID] must match the Entitlement **Identifier** (not Display Name)
 * in RevenueCat → Product catalog → Entitlements.
 * Attach Play products: [BillingProductIds.YEARLY], [BillingProductIds.MONTHLY],
 * [BillingProductIds.WEEKLY].
 *
 * If Identifier ≠ this constant, [BillingManager] still unlocks via active
 * `premium_year` / `premium_month` / `premium_week` subscriptions as a fallback.
 */
object RevenueCatConfig {
    // Prefer the short Identifier from RC (often "premium"). Spaces = usually Display Name.
    const val ENTITLEMENT_ID = "Ai Hair Style Pro"
}
