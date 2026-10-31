package com.hairstyle.changer.aitryon.utils

import com.hairstyle.changer.aitryon.model.LocalHairstyleResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Remembers on this device that the server daily generate limit is used up,
 * until the server `resets_at` time. Lets the app block instantly (no network)
 * as soon as a response reports `remaining == 0`.
 */
object DailyLimitStore {

    fun isReached(): Boolean = System.currentTimeMillis() < PrefManager.getDailyLimitBlockedUntil()

    fun markReached(resetsAtIso: String?) {
        PrefManager.setDailyLimitBlockedUntil(parseIsoUtc(resetsAtIso) ?: nextUtcMidnight())
    }

    fun clear() {
        PrefManager.setDailyLimitBlockedUntil(0L)
    }

    /** Call with every generate response: stores the limit state from `quota` / 429 data. */
    fun update(response: LocalHairstyleResponse) {
        val remaining = response.effectiveRemaining()
        if (response.isDailyLimitReached() || (remaining != null && remaining <= 0)) {
            markReached(response.effectiveResetsAt())
        } else if (remaining != null) {
            clear()
        }
    }

    private fun parseIsoUtc(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { format.parse(raw)?.time }.getOrNull()
            ?.takeIf { it > System.currentTimeMillis() }
    }

    private fun nextUtcMidnight(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
