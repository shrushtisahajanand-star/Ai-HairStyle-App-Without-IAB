package com.hairstyle.changer.aitryon.utils

/**
 * Reads encrypted video URLs from SharedPreferences (keyed by API title) and decrypts them.
 */
object VideoUrlManager {

    fun getVideoUrl(title: String): String? {
        if (title.isBlank()) return null
        val encrypted = PrefManager.getVideoUrlEncrypted(title)
        if (encrypted.isBlank()) return null
        return runCatching { AESHelper.decrypt(encrypted) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun hasVideoUrl(title: String): Boolean = !getVideoUrl(title).isNullOrBlank()
}
