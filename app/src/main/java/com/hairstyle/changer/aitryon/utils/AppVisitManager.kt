package com.hairstyle.changer.aitryon.utils

object AppVisitManager {

    private const val KEY_LAUNCH_COUNT = "app_launch_count"

    private var promoShownThisSession = false

    fun incrementLaunchCount() {
        val count = getLaunchCount() + 1
        PrefManager.putInt(KEY_LAUNCH_COUNT, count)
        RateAppManager.onLaunchCounted(count)
    }

    fun getLaunchCount(): Int = PrefManager.getInt(KEY_LAUNCH_COUNT, 0)

    /** Show promo from 2nd app open onward, once per app session. */
    fun shouldShowPromoOnAppOpen(): Boolean {
        return getLaunchCount() >= 2 && !promoShownThisSession
    }

    fun markPromoShownThisSession() {
        promoShownThisSession = true
    }
}
