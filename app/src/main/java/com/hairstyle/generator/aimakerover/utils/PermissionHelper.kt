package com.hairstyle.generator.aimakerover.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppManage

object PermissionHelper {

    enum class PermissionType {
        CAMERA,
        STORAGE,
        NOTIFICATION
    }

    private const val KEY_CAMERA_DENIED = "permission_camera_denied_once"
    private const val KEY_STORAGE_DENIED = "permission_storage_denied_once"
    private const val KEY_NOTIFICATION_DENIED = "permission_notification_denied_once"
    private const val KEY_DENIED_FLAGS_RESET = "permission_denied_flags_reset_v3"

    /**
     * Older builds marked a permission denied even when the system dialog was only
     * dismissed by tapping outside, which wrongly showed the settings dialog afterwards.
     * Clear those flags once so the system dialog is asked again.
     */
    fun resetStaleDeniedFlags() {
        if (!PrefManager.isInitialized()) return
        if (PrefManager.getBoolean(KEY_DENIED_FLAGS_RESET, false)) return
        PermissionType.entries.forEach { clearDeniedFlag(it) }
        PrefManager.putBoolean(KEY_DENIED_FLAGS_RESET, true)
    }

    fun cameraPermissions(): Array<String> = arrayOf(Manifest.permission.CAMERA)

    fun storagePermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else -> {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /** Android 13+ only; below Tiramisu notifications do not need runtime permission. */
    fun notificationPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        }
        else -> emptyArray()
    }

    fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasNotificationPermission(context: Context): Boolean {
        val permissions = notificationPermissions()
        if (permissions.isEmpty()) return true
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun permissionsFor(type: PermissionType): Array<String> = when (type) {
        PermissionType.CAMERA -> cameraPermissions()
        PermissionType.STORAGE -> storagePermissions()
        PermissionType.NOTIFICATION -> notificationPermissions()
    }

    fun hasPermissions(fragment: Fragment, permissions: Array<String>): Boolean =
        permissions.isEmpty() || permissions.all {
            ContextCompat.checkSelfPermission(fragment.requireContext(), it) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun isPermanentlyDenied(fragment: Fragment, type: PermissionType): Boolean {
        val permissions = permissionsFor(type)
        if (permissions.isEmpty()) return false
        if (hasPermissions(fragment, permissions)) return false

        val deniedBefore = when (type) {
            PermissionType.CAMERA -> PrefManager.getBoolean(KEY_CAMERA_DENIED, false)
            PermissionType.STORAGE -> PrefManager.getBoolean(KEY_STORAGE_DENIED, false)
            PermissionType.NOTIFICATION -> PrefManager.getBoolean(KEY_NOTIFICATION_DENIED, false)
        }
        if (!deniedBefore) return false

        return permissions.any { permission ->
            !fragment.shouldShowRequestPermissionRationale(permission)
        }
    }

    /** True while the system dialog can still be shown for this permission. */
    fun shouldShowRationale(fragment: Fragment, type: PermissionType): Boolean =
        permissionsFor(type).any { fragment.shouldShowRequestPermissionRationale(it) }

    enum class PermissionOutcome {
        GRANTED,

        /** System dialog can still be shown — never fall back to the settings dialog. */
        CAN_ASK_AGAIN,

        /** Blocked by the system — only Settings can restore it. */
        PERMANENTLY_DENIED
    }

    /**
     * A permanently denied request returns almost instantly because no dialog is shown,
     * while dismissing the visible dialog by tapping outside takes far longer. Some devices
     * report that dismissal as a normal denial, so the elapsed time separates the two.
     */
    private const val AUTO_DENY_MAX_MS = 500L

    fun evaluateResult(
        fragment: Fragment,
        type: PermissionType,
        result: Map<String, Boolean>,
        rationaleBefore: Boolean,
        elapsedMs: Long
    ): PermissionOutcome {
        if (result.isNotEmpty() && result.values.all { it }) return PermissionOutcome.GRANTED

        // Cancelled before the system delivered any decision.
        if (result.isEmpty()) return PermissionOutcome.CAN_ASK_AGAIN

        // A single "Don't allow" — the system still shows the dialog next time.
        if (shouldShowRationale(fragment, type)) return PermissionOutcome.CAN_ASK_AGAIN

        // Rationale was already false and the dialog stayed up: the user tapped outside.
        if (!rationaleBefore && elapsedMs > AUTO_DENY_MAX_MS) {
            return PermissionOutcome.CAN_ASK_AGAIN
        }

        return PermissionOutcome.PERMANENTLY_DENIED
    }

    fun wasDeniedBefore(type: PermissionType): Boolean = when (type) {
        PermissionType.CAMERA -> PrefManager.getBoolean(KEY_CAMERA_DENIED, false)
        PermissionType.STORAGE -> PrefManager.getBoolean(KEY_STORAGE_DENIED, false)
        PermissionType.NOTIFICATION -> PrefManager.getBoolean(KEY_NOTIFICATION_DENIED, false)
    }

    fun markDenied(type: PermissionType) {
        when (type) {
            PermissionType.CAMERA -> PrefManager.putBoolean(KEY_CAMERA_DENIED, true)
            PermissionType.STORAGE -> PrefManager.putBoolean(KEY_STORAGE_DENIED, true)
            PermissionType.NOTIFICATION -> PrefManager.putBoolean(KEY_NOTIFICATION_DENIED, true)
        }
    }

    fun clearDeniedFlag(type: PermissionType) {
        when (type) {
            PermissionType.CAMERA -> PrefManager.putBoolean(KEY_CAMERA_DENIED, false)
            PermissionType.STORAGE -> PrefManager.putBoolean(KEY_STORAGE_DENIED, false)
            PermissionType.NOTIFICATION -> PrefManager.putBoolean(KEY_NOTIFICATION_DENIED, false)
        }
    }

    fun openAppSettings(context: Context) {
        ADSAppManage.skipNextAppOpenAd()
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
