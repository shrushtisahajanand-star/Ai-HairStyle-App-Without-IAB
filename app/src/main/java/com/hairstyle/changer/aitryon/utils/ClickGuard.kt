package com.hairstyle.changer.aitryon.utils

import android.os.SystemClock

/** Drops taps that arrive right after another one, so fast taps open a screen / sheet only once. */
object ClickGuard {

    private const val DEFAULT_INTERVAL_MS = 800L
    private var lastClickAt = 0L

    fun allow(intervalMs: Long = DEFAULT_INTERVAL_MS): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAt < intervalMs) return false
        lastClickAt = now
        return true
    }
}
