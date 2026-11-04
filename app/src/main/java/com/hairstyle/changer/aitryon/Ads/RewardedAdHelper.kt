package com.hairstyle.changer.aitryon.Ads

import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.changer.aitryon.ui.progress.SomethingWentWrongDialog

/**
 * Helper for Rewarded Ads (Generate / Save).
 * Always onLoad when [ADSMainClass.getRewardAdsShow] is true;
 * otherwise skips ads and grants the action for free users.
 */
object RewardedAdHelper {

    fun showForReward(
        fragment: Fragment,
        onRewardEarned: () -> Unit,
        onFailedOrSkipped: () -> Unit = {}
    ) {
        val activity = fragment.activity
        if (activity == null || activity.isFinishing || !fragment.isAdded) {
            onFailedOrSkipped()
            return
        }
        showForReward(
            activity = activity,
            isAlive = { fragment.isAdded },
            onRewardEarned = onRewardEarned,
            onFailedOrSkipped = onFailedOrSkipped,
            showFailedDialog = { SomethingWentWrongDialog.show(fragment, onFailedOrSkipped) }
        )
    }

    fun showForReward(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailedOrSkipped: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFailedOrSkipped()
            return
        }
        showForReward(
            activity = activity,
            isAlive = { !activity.isFinishing && !activity.isDestroyed },
            onRewardEarned = onRewardEarned,
            onFailedOrSkipped = onFailedOrSkipped,
            showFailedDialog = { SomethingWentWrongDialog.show(activity, onFailedOrSkipped) }
        )
    }

    /**
     * SAVE buttons: with `save_photo_show_ads` on, a reward ad is shown first and the image is
     * saved once it is watched. If the ad can't be shown (load fail, not ready within
     * [SAVE_AD_LOAD_TIMEOUT_MS], consent, show fail) the user is NOT blocked - the image is
     * saved without it. Closing the ad early does not save.
     */
    fun showForSave(
        activity: Activity,
        onSave: () -> Unit,
        onClosedWithoutReward: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        val isAlive = { !activity.isFinishing && !activity.isDestroyed }
        ADSRewardDisplay.loadAndShow(
            activity,
            ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID),
            object : ADSRewardDisplay.Listener {
                override fun onRewardEarned() {
                    if (isAlive()) onSave()
                }

                override fun onAdFailed() {
                    if (isAlive()) onSave()
                }

                override fun onClosedWithoutReward() {
                    if (isAlive()) onClosedWithoutReward()
                }
            },
            false,
            SAVE_AD_LOAD_TIMEOUT_MS
        )
    }

    private const val SAVE_AD_LOAD_TIMEOUT_MS = 8000L

    private fun showForReward(
        activity: Activity,
        isAlive: () -> Boolean,
        onRewardEarned: () -> Unit,
        onFailedOrSkipped: () -> Unit,
        showFailedDialog: () -> Unit
    ) {
        ADSRewardDisplay.loadAndShow(
            activity,
            ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID),
            object : ADSRewardDisplay.Listener {
                override fun onRewardEarned() {
                    if (!isAlive()) return
                    onRewardEarned()
                }

                override fun onAdFailed() {
                    if (!isAlive()) return
                    showFailedDialog()
                }

                override fun onClosedWithoutReward() {
                    if (!isAlive()) return
                    onFailedOrSkipped()
                }
            }
        )
    }
}
