package com.hairstyle.changer.aitryon.utils

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.hairstyle.changer.aitryon.remoteconfig.InAppUpdateConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Play In-App Updates gated by Firebase `update_app`.
 *
 * - [InAppUpdateConfig.show] must be true
 * - Immediate â†’ force update (no daily limit); cancel does not unlock promo
 * - Flexible â†’ at most [InAppUpdateConfig.dailyDialogShowCount] dialogs per calendar day
 */
object InAppUpdateHelper {

    private const val TAG = "InAppUpdateHelper"

    private var sessionHandled = false
    private var waitingForPlayUi = false
    private var requestInFlight = false

    fun onUpdateFlowResult(
        resultCode: Int,
        config: InAppUpdateConfig,
        onFinished: () -> Unit
    ) {
        waitingForPlayUi = false
        requestInFlight = false
        // Log.d(TAG, "Update flow resultCode=$resultCode immediate=${config.isImmediate}")

        if (config.show && config.isImmediate && resultCode != Activity.RESULT_OK) {
            // Force update: allow another attempt on next Home resume; skip promo.
            sessionHandled = false
            return
        }

        sessionHandled = true
        onFinished()
    }

    /**
     * Checks config + Play Store. Invokes [onFinished] when promo may show
     * (update off / unavailable / daily limit / Flexible flow ended).
     * Does not invoke [onFinished] while Play UI is showing, or after Immediate cancel.
     */
    fun checkAndMaybeStart(
        fragment: Fragment,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        config: InAppUpdateConfig,
        onFinished: () -> Unit
    ) {
        if (!fragment.isAdded || fragment.isHidden) return
        if (waitingForPlayUi || requestInFlight) return
        if (sessionHandled) {
            onFinished()
            return
        }

        if (!config.show) {
            sessionHandled = true
            onFinished()
            return
        }

        val activity = fragment.activity ?: run {
            sessionHandled = true
            onFinished()
            return
        }

        requestInFlight = true
        val appUpdateManager = AppUpdateManagerFactory.create(activity.applicationContext)
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                requestInFlight = false
                if (!fragment.isAdded || fragment.isHidden) {
                    sessionHandled = true
                    onFinished()
                    return@addOnSuccessListener
                }

                // Resume Immediate if Play left download/install mid-flight.
                if (info.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    config.isImmediate
                ) {
                    startFlow(
                        fragment = fragment,
                        appUpdateManager = appUpdateManager,
                        info = info,
                        updateLauncher = updateLauncher,
                        updateType = AppUpdateType.IMMEDIATE,
                        recordFlexibleShow = false,
                        onFinished = onFinished
                    )
                    return@addOnSuccessListener
                }

                val updateType =
                    if (config.isImmediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(updateType)

                if (!available) {
                    // Log.d(TAG, "No update available (availability=${info.updateAvailability()})")
                    sessionHandled = true
                    onFinished()
                    return@addOnSuccessListener
                }

                if (config.isFlexible && !canShowFlexibleDialogToday(config.dailyDialogShowCount)) {
                    // Log.d(TAG, "Flexible daily dialog limit reached")
                    sessionHandled = true
                    onFinished()
                    return@addOnSuccessListener
                }

                startFlow(
                    fragment = fragment,
                    appUpdateManager = appUpdateManager,
                    info = info,
                    updateLauncher = updateLauncher,
                    updateType = updateType,
                    recordFlexibleShow = config.isFlexible,
                    onFinished = onFinished
                )
            }
            .addOnFailureListener { e ->
                requestInFlight = false
                // Log.e(TAG, "appUpdateInfo failed", e)
                sessionHandled = true
                onFinished()
            }
    }

    private fun startFlow(
        fragment: Fragment,
        appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager,
        info: com.google.android.play.core.appupdate.AppUpdateInfo,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        updateType: Int,
        recordFlexibleShow: Boolean,
        onFinished: () -> Unit
    ) {
        runCatching {
            waitingForPlayUi = true
            if (recordFlexibleShow) recordFlexibleDialogShown()
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
            // Log.d(
                // TAG,
                // "Started ${if (updateType == AppUpdateType.IMMEDIATE) "Immediate" else "Flexible"} update"
            // )
        }.onFailure { e ->
            // Log.e(TAG, "Failed to start update flow", e)
            waitingForPlayUi = false
            sessionHandled = true
            onFinished()
        }
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun canShowFlexibleDialogToday(maxPerDay: Int): Boolean {
        if (maxPerDay <= 0) return false
        val today = todayKey()
        val count = if (PrefManager.getInAppUpdateDialogDay() == today) {
            PrefManager.getInAppUpdateDialogCount()
        } else {
            0
        }
        return count < maxPerDay
    }

    private fun recordFlexibleDialogShown() {
        val today = todayKey()
        val count = if (PrefManager.getInAppUpdateDialogDay() == today) {
            PrefManager.getInAppUpdateDialogCount()
        } else {
            0
        }
        PrefManager.setInAppUpdateDialogDay(today)
        PrefManager.setInAppUpdateDialogCount(count + 1)
    }
}
