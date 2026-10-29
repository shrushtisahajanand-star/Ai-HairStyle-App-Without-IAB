package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.provider.Settings

/**
 * Returns a stable device identifier that survives app storage clear.
 * Uses [Settings.Secure.ANDROID_ID] — tied to the device, not app data.
 */
object DeviceIdProvider {

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: "unknown-device"
    }
}
