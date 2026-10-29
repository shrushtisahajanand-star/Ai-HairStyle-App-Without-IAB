package com.hairstyle.changer.aitryon.model

import com.google.gson.annotations.SerializedName

data class LocalHairstyleResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("image") val image: String? = null,
    /** Server-side reason when [success] is false (key name varies by endpoint). */
    @SerializedName(value = "message", alternate = ["error", "msg", "detail"])
    val message: String? = null
) {
    fun failureReason(): String = message?.takeIf { it.isNotBlank() } ?: "no message from server"
}
