package com.hairstyle.generator.aimakerover.data.model

import com.google.gson.annotations.SerializedName

/** One concurrently active premium subscription (week / month / year can stack). */
data class ActivePlanRecord(
    @SerializedName("plan_key") val planKey: String,
    @SerializedName("started_at") val startedAt: Long,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("period_started_at") val periodStartedAt: Long = startedAt,
    @SerializedName("period_images_used") val periodImagesUsed: Int = 0,
    @SerializedName("current_day_index") val currentDayIndex: Int = 0
)
