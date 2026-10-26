package com.hairstyle.generator.aimakerover.data.model

import com.google.gson.annotations.SerializedName

/**
 * POST /api/check-free-limit
 *
 * ```json
 * {
 *   "success": true,
 *   "data": {
 *     "image_generation": true,
 *     "subscription_expired": false,
 *     "subscription": { ... }
 *   }
 * }
 * ```
 */
data class CheckFreeLimitResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("expired") val expired: Boolean? = null,
    @SerializedName("free_limit") val freeLimit: Int? = null,
    @SerializedName("used") val used: Int? = null,
    @SerializedName("image_generation") val imageGeneration: Boolean? = null,
    @SerializedName("subscription_expired") val subscriptionExpired: Boolean? = null,
    @SerializedName("data") val data: CheckFreeLimitData? = null
) {
    fun effectiveImageGeneration(): Boolean? =
        imageGeneration ?: data?.imageGeneration ?: data?.subscription?.imageGeneration

    fun effectiveSubscriptionExpired(): Boolean =
        subscriptionExpired == true ||
            data?.subscriptionExpired == true ||
            data?.subscription?.expired == true

    fun subscription(): FreeLimitSubscription? = data?.subscription
}

data class CheckFreeLimitData(
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("expired") val expired: Boolean? = null,
    @SerializedName("free_limit") val freeLimit: Int? = null,
    @SerializedName("used") val used: Int? = null,
    @SerializedName("remaining") val remaining: Int? = null,
    @SerializedName("total_available_images") val totalAvailableImages: Int? = null,
    @SerializedName("image_generation") val imageGeneration: Boolean? = null,
    @SerializedName("subscription_expired") val subscriptionExpired: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("purchase_id") val purchaseId: String? = null,
    @SerializedName("subscription") val subscription: FreeLimitSubscription? = null
)

data class FreeLimitSubscription(
    @SerializedName("plan_type") val planType: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("total_quota") val totalQuota: Int? = null,
    @SerializedName("remaining_quota") val remainingQuota: Int? = null,
    @SerializedName("today_allocated") val todayAllocated: Int? = null,
    @SerializedName("today_used") val todayUsed: Int? = null,
    @SerializedName("today_remaining") val todayRemaining: Int? = null,
    @SerializedName("total_days") val totalDays: Int? = null,
    @SerializedName("current_day") val currentDay: Int? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("expiry_date") val expiryDate: String? = null,
    @SerializedName("expired") val expired: Boolean? = null,
    @SerializedName("image_generation") val imageGeneration: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("renewal_required") val renewalRequired: Boolean? = null
)
