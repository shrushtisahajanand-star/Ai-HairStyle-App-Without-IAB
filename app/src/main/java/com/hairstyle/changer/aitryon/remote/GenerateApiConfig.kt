package com.hairstyle.changer.aitryon.remote

import com.hairstyle.changer.aitryon.utils.AESHelper
import com.hairstyle.changer.aitryon.utils.PrefManager

/**
 * Single backend host and its API endpoints.
 * The host comes AES-encrypted from Firebase Remote Config (`APi.base_url`);
 * only an encrypted fallback is kept in the app.
 */
object GenerateApiConfig {

    private const val ENCRYPTED_FALLBACK_BASE_URL =
        "U2FsdGVkX1++f2YXx70KC4nl/zEno9BC3CkSznrUZwPakdI6gTUe/iR3taF5IsAYz0NpLTMqAdGCOFAcnVIxcA=="

    const val HAIRSTYLE_ENDPOINT = "api/hairstyle"
    const val CHECK_FREE_LIMIT_ENDPOINT = "api/check-free-limit"
    const val IMAGES_ENDPOINT = "api/images"
    const val VIDEOS_ENDPOINT = "api/videos"
    const val CATEGORIES_ENDPOINT = "api/categories"

    fun baseUrl(): String {
        val stored = PrefManager.getBaseUrl().trim()
        val resolved = when {
            stored.startsWith("http", ignoreCase = true) -> stored
            stored.isNotBlank() -> runCatching { AESHelper.decrypt(stored).trim() }.getOrDefault("")
            else -> ""
        }
        return resolved.ifBlank { AESHelper.decrypt(ENCRYPTED_FALLBACK_BASE_URL).trim() }
    }

    fun buildUrl(baseUrl: String, endpoint: String): String {
        val base = baseUrl.trimEnd('/')
        val path = endpoint.trimStart('/')
        return "$base/$path"
    }

    fun buildUrl(endpoint: String): String = buildUrl(baseUrl(), endpoint)
}
