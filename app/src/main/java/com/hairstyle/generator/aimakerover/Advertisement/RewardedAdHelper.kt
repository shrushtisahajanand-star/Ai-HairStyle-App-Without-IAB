package com.hairstyle.generator.aimakerover.Advertisement

import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakerover.ui.progress.SomethingWentWrongDialog

/**
 * Fragment helper for Rewarded Ads (Generate / Save).
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

        ADSRewardDisplay.loadAndShow(
            activity,
            ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID),
            object : ADSRewardDisplay.Listener {
                override fun onRewardEarned() {
                    if (!fragment.isAdded) return
                    onRewardEarned()
                }

                override fun onAdFailed() {
                    if (!fragment.isAdded) return
                    SomethingWentWrongDialog.show(fragment) {
                        onFailedOrSkipped()
                    }
                }

                override fun onClosedWithoutReward() {
                    if (!fragment.isAdded) return
                    onFailedOrSkipped()
                }
            }
        )
    }
}
