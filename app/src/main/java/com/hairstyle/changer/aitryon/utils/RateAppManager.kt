package com.hairstyle.changer.aitryon.utils

/**
 * Rate-app prompt: every 4th app open (4, 8, 12…) and after image save.
 *
 * Play Store does **not** expose whether the user already left a review.
 * We persist [KEY_USER_RATED] when they submit (In-App Review / Play Store) and
 * never show this dialog again after that.
 */
object RateAppManager {

    private const val APP_OPEN_INTERVAL = 4
    private const val KEY_LAST_SHOWN_LAUNCH = "rate_app_last_shown_launch_count"
    private const val KEY_USER_RATED = "rate_app_user_already_rated"

    private var appOpenPromptHandledThisSession = false
    private var dialogVisible = false

    /** @deprecated Kept for Splash call site; eligibility uses launch count + rated flag. */
    fun onLaunchCounted(launchCount: Int) = Unit

    /** True once user submitted rating (Play / In-App Review). Dialog must not show again. */
    fun hasUserRated(): Boolean = PrefManager.getBoolean(KEY_USER_RATED, false)

    fun markUserRated() {
        PrefManager.putBoolean(KEY_USER_RATED, true)
        appOpenPromptHandledThisSession = true
    }

    fun shouldShowAppOpenPrompt(): Boolean {
        if (hasUserRated()) return false
        if (appOpenPromptHandledThisSession || dialogVisible) return false
        val count = AppVisitManager.getLaunchCount()
        if (count < APP_OPEN_INTERVAL || count % APP_OPEN_INTERVAL != 0) return false
        if (PrefManager.getInt(KEY_LAST_SHOWN_LAUNCH, -1) == count) return false
        return true
    }

    fun shouldShowAfterSave(): Boolean {
        return !hasUserRated() && !dialogVisible
    }

    fun markAppOpenPromptHandled() {
        appOpenPromptHandledThisSession = true
        PrefManager.putInt(KEY_LAST_SHOWN_LAUNCH, AppVisitManager.getLaunchCount())
    }

    fun canShowDialog(): Boolean = !dialogVisible && !hasUserRated()

    fun markDialogVisible(visible: Boolean) {
        dialogVisible = visible
    }
}
