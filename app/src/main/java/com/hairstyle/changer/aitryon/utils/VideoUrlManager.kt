package com.hairstyle.changer.aitryon.utils

/**
 * Reads video URLs from SharedPreferences (keyed by API title).
 */
object VideoUrlManager {

    fun getVideoUrl(title: String): String? {
        if (title.isBlank()) return null
        return PrefManager.getVideoUrl(title).takeIf { it.startsWith("http", ignoreCase = true) }
    }

    fun hasVideoUrl(title: String): Boolean = !getVideoUrl(title).isNullOrBlank()
}
