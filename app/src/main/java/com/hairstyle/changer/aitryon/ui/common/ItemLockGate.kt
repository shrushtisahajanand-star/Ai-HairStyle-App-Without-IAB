package com.hairstyle.changer.aitryon.ui.common

import android.app.Activity
import com.hairstyle.changer.aitryon.Ads.UnlockAdsHelper
import com.hairstyle.changer.aitryon.utils.UnlockConfig
import com.hairstyle.changer.aitryon.utils.UnlockSession

/**
 * Decides which list items are locked and runs "dialog → ad → continue" for them.
 * The top [UnlockConfig.freeCount] items are free; the rest need an ad. An item whose ad was
 * watched stays unlocked until the app is closed ([UnlockSession]).
 */
class ItemLockGate(
    private val group: UnlockConfig.Group,
    private val activityProvider: () -> Activity?
) {

    fun isLocked(position: Int, id: Int, title: String): Boolean =
        position >= 0 &&
            UnlockAdsHelper.isLocked(group, position) &&
            !UnlockSession.isUnlocked(group, id, title)

    fun run(position: Int, id: Int, title: String, onUnlocked: () -> Unit) {
        if (!isLocked(position, id, title)) {
            onUnlocked()
            return
        }
        val activity = activityProvider() ?: return
        UnlockItemDialog.show(
            activity = activity,
            onWatchAds = {
                UnlockAdsHelper.runSequence(activity, onUnlocked = {
                    UnlockSession.markUnlocked(group, id, title)
                    onUnlocked()
                })
            }
        )
    }

    /** Call when a list screen becomes visible so the next ad is ready. */
    fun preload() {
        UnlockAdsHelper.preload(activityProvider())
    }
}
