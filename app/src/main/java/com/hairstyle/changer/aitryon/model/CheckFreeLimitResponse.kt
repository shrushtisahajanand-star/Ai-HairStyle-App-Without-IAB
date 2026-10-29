package com.hairstyle.changer.aitryon.model

import com.google.gson.annotations.SerializedName

/**
 * POST /api/check-free-limit
 *
 * ```json
 * {
 *   "success": true,
 *   "data": {
 *     "device_id": "..",
 *     "free_limit": 3,
 *     "used": 0,
 *     "remaining": 3,
 *     "image_generation": true,
 *     "daily_limit": 3,
 *     "resets_at": "2026-10-30T00:00:00.963Z",
 *     "message": "You have 3 generations remaining today."
 *   }
 * }
 * ```
 */
data class CheckFreeLimitResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: CheckFreeLimitData? = null
) {
    fun effectiveImageGeneration(): Boolean? = data?.imageGeneration
}

data class CheckFreeLimitData(
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("free_limit") val freeLimit: Int? = null,
    @SerializedName("used") val used: Int? = null,
    @SerializedName("remaining") val remaining: Int? = null,
    @SerializedName("image_generation") val imageGeneration: Boolean? = null,
    @SerializedName("daily_limit") val dailyLimit: Int? = null,
    @SerializedName("resets_at") val resetsAt: String? = null,
    @SerializedName("message") val message: String? = null
)
