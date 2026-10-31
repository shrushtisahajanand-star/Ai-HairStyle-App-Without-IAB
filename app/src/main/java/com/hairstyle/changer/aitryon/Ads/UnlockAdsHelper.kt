package com.hairstyle.changer.aitryon.Ads

import android.app.Activity
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.progress.SomethingWentWrongDialog
import com.hairstyle.changer.aitryon.utils.PrefManager
import com.hairstyle.changer.aitryon.utils.UnlockConfig
import com.hairstyle.changer.aitryon.utils.UnlockConfig.AdKind

/**
 * Ad shown to use a locked style / hair color. Every unlock shows ONE ad, taken from the
 * Firebase `unlock_items.ads_sequence` in turn and wrapping around, e.g. `[["reward",2],["inter",1]]`:
 * 1st unlock → reward, 2nd → reward, 3rd → inter, 4th → reward, 5th → reward, 6th → inter …
 * `[["reward",1]]` → always reward. Nothing is unlocked for good — every locked click shows its ad.
 *
 * The position advances only when the ad was really watched (not on fail / cancel) and is kept
 * across app restarts.
 *
 * Switched on only by `unlock_ads_show = true`; it does NOT depend on `reward_ads_show` /
 * `inter_ads_show`, and uses its own ad units (`reward_ad_id` / `inter_ad_id`) and its own load
 * type (`ads_load_type`: PreLoad = cache the next ad in advance, otherwise load on demand).
 *
 * Failures: offline → No Internet dialog; ad load/show failed → Something Went Wrong dialog.
 */
object UnlockAdsHelper {

    /** `true` → locks and unlock ads are active (`unlock_ads_show`); `ads_blocked` still wins. */
    fun isEnabled(): Boolean = !ADSMainClass.getAds_Free() && UnlockConfig.unlockAdsShow()

    private fun sequence(): List<AdKind> =
        if (isEnabled()) UnlockConfig.adSequence() else emptyList()

    fun isLocked(group: UnlockConfig.Group, position: Int): Boolean =
        isEnabled() && position >= UnlockConfig.freeCount(group)

    /** Unlock reward ad unit (`unlock_items.reward_ad_id`); blank → the normal reward unit. */
    fun rewardAdId(): String =
        ADSMainClass.getStringValue(ADSMainClass.UNLOCK_REWARD_AD_ID)
            .ifBlank { ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID) }

    /** Unlock interstitial unit (`unlock_items.inter_ad_id`); blank → the normal forward inter unit. */
    fun interAdId(): String =
        ADSMainClass.getStringValue(ADSMainClass.UNLOCK_INTER_AD_ID)
            .ifBlank { InterstitialNavHelper.forwardAdId() }

    /** Index in [steps] of the ad the next unlock will show. */
    private fun upcomingIndex(steps: List<AdKind>): Int =
        Math.floorMod(PrefManager.getUnlockSequencePos(), steps.size)

    fun runSequence(
        activity: Activity,
        onUnlocked: () -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val steps = sequence()
        if (steps.isEmpty()) {
            onUnlocked()
            return
        }

        fun isAlive() = !activity.isFinishing && !activity.isDestroyed

        if (isAlive() && (activity as? BaseActivity)?.showNoInternetIfOffline() == true) {
            onCancelled()
            return
        }

        val index = upcomingIndex(steps)

        fun fail() {
            if (!isAlive()) return
            // Offline → No Internet; otherwise the ad itself failed.
            if ((activity as? BaseActivity)?.showNoInternetIfOffline() == true) {
                onCancelled()
            } else {
                SomethingWentWrongDialog.show(activity, R.string.unlock_ad_failed_message) { onCancelled() }
            }
        }

        fun done() {
            if (!isAlive()) return
            PrefManager.setUnlockSequencePos(index + 1)
            onUnlocked()
            preload(activity)
        }

        when (steps[index]) {
            AdKind.REWARD -> ADSRewardDisplay.loadAndShow(
                activity,
                rewardAdId(),
                object : ADSRewardDisplay.Listener {
                    override fun onRewardEarned() = done()
                    override fun onAdFailed() = fail()
                    override fun onClosedWithoutReward() {
                        if (isAlive()) onCancelled()
                    }
                },
                true
            )
            AdKind.INTER -> ADSInterDisplayClick.showForUnlock(
                activity,
                interAdId(),
                object : ADSInterDisplayClick.UnlockListener {
                    override fun onClosed() = done()
                    override fun onFailed() = fail()
                }
            )
        }
    }

    /** Warm the ad the next unlock will show (PreLoad mode only; no-op in Load mode). */
    fun preload(activity: Activity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return
        if (!UnlockConfig.isPreload()) return
        val steps = sequence()
        if (steps.isEmpty()) return
        when (steps[upcomingIndex(steps)]) {
            AdKind.REWARD -> ADSRewardDisplay.preload(activity, rewardAdId(), true)
            AdKind.INTER -> ADSInterDisplayClick.preloadForUnlock(activity, interAdId())
        }
    }
}
