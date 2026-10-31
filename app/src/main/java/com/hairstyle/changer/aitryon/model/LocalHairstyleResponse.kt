package com.hairstyle.changer.aitryon.model

import com.google.gson.annotations.SerializedName

data class LocalHairstyleResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("image") val image: String? = null,
    /** Server-side reason when [success] is false (key name varies by endpoint). */
    @SerializedName(value = "message", alternate = ["error", "msg", "detail"])
    val message: String? = null,
    @SerializedName("code") val code: String? = null,
    /** Present on successful generate: `{ daily_limit, used, remaining, resets_at }`. */
    @SerializedName("quota") val quota: GenerateQuota? = null,
    /** Present on HTTP 429 (daily limit exceeded). */
    @SerializedName("remaining") val remaining: Int? = null,
    @SerializedName("resets_at") val resetsAt: String? = null,
    @SerializedName("exceeded") val exceeded: Boolean? = null
) {
    fun failureReason(): String = message?.takeIf { it.isNotBlank() } ?: "no message from server"

    /** Server rejected the request because the device used up its daily free limit. */
    fun isDailyLimitReached(): Boolean =
        !success && (
            code == "DAILY_QUOTA_EXCEEDED" ||
                exceeded == true ||
                message?.contains("limit", ignoreCase = true) == true
            )

    fun effectiveRemaining(): Int? = quota?.remaining ?: remaining

    fun effectiveResetsAt(): String? = quota?.resetsAt ?: resetsAt
}

data class GenerateQuota(
    @SerializedName("daily_limit") val dailyLimit: Int? = null,
    @SerializedName("used") val used: Int? = null,
    @SerializedName("remaining") val remaining: Int? = null,
    @SerializedName("resets_at") val resetsAt: String? = null
)
