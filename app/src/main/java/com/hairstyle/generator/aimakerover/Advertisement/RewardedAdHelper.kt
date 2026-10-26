package com.hairstyle.generator.aimakerover.Advertisement

import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakerover.ui.progress.SomethingWentWrongDialog

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
