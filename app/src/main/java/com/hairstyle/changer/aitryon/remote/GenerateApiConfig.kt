package com.hairstyle.changer.aitryon.remote

import com.hairstyle.changer.aitryon.utils.SecureConfig

/**
 * Generate / free-limit API — only path endpoints.
 * Base URL comes from Firebase Remote Config (`APi.generate_base_url`),
 * with an AES-encrypted local fallback (never a plain host string).
 */
object GenerateApiConfig {

    /** Decrypted at call time — do not expose as a plain const. */
    val DEFAULT_BASE_URL: String
        get() = SecureConfig.defaultGenerateBaseUrl()

    const val HAIRSTYLE_ENDPOINT = "api/hairstyle"
    const val CHECK_FREE_LIMIT_ENDPOINT = "api/check-free-limit"

    fun buildUrl(baseUrl: String, endpoint: String): String {
        val base = baseUrl.trimEnd('/')
        val path = endpoint.trimStart('/')
        return "$base/$path"
    }
}
