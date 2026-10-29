package com.hairstyle.changer.aitryon.remoteconfig

import com.google.gson.annotations.SerializedName

/**
 * Firebase Remote Config `update_app` block.
 *
 * ```json
 * "update_app": {
 *   "in_app_update_show": false,
 *   "in_app_update_type": "Flexible",
 *   "in_appp_dailog_daily_show_count": 2
 * }
 * ```
 */
data class InAppUpdateConfig(
    @SerializedName("in_app_update_show")
    val show: Boolean = false,
    @SerializedName("in_app_update_type")
    val type: String = TYPE_FLEXIBLE,
    /** Typo kept to match Firebase key exactly. Used only for Flexible. */
    @SerializedName("in_appp_dailog_daily_show_count")
    val dailyDialogShowCount: Int = DEFAULT_DAILY_COUNT
) {
    val isImmediate: Boolean
        get() = type.equals(TYPE_IMMEDIATE, ignoreCase = true)

    val isFlexible: Boolean
        get() = !isImmediate

    companion object {
        const val TYPE_FLEXIBLE = "Flexible"
        const val TYPE_IMMEDIATE = "Immediate"
        const val DEFAULT_DAILY_COUNT = 2

        val DISABLED = InAppUpdateConfig(show = false)
    }
}
