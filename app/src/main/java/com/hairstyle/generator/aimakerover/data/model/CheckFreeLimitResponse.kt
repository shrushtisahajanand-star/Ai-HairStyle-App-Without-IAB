package com.hairstyle.generator.aimakerover.data.model

import com.google.gson.annotations.SerializedName

/**
 * POST /api/check-free-limit
 *
 * Example:
 * ```json
 * {
 *   "success": true,
 *   "expired": false,
 *   "free_limit": 1,
 *   "used": 0
 * }
 * ```
 */
data class CheckFreeLimitResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("expired") val expired: Boolean? = null,
    @SerializedName("free_limit") val freeLimit: Int? = null,
    @SerializedName("used") val used: Int? = null,
    @SerializedName("data") val data: CheckFreeLimitData? = null
) {
    private val effectiveExpired: Boolean? get() = expired ?: data?.expired
    private val effectiveFreeLimit: Int? get() = freeLimit ?: data?.freeLimit
    private val effectiveUsed: Int? get() = used ?: data?.used

    /**
     * Prefer server [expired] when present.
     * Otherwise treat limit as reached when used >= free_limit.
     */
    fun isLimitReached(): Boolean {
        effectiveExpired?.let { return it }
        val limit = effectiveFreeLimit ?: return false
        if (limit <= 0) return true
        return (effectiveUsed ?: 0) >= limit
    }
}

data class CheckFreeLimitData(
    @SerializedName("expired") val expired: Boolean? = null,
    @SerializedName("free_limit") val freeLimit: Int? = null,
    @SerializedName("used") val used: Int? = null
)
