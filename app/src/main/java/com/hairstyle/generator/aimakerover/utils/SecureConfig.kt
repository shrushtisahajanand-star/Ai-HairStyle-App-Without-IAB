package com.hairstyle.generator.aimakerover.utils

/**
 * Runtime-only secrets — plain values are never stored as string literals.
 * Generate base URL uses AES (same as Firebase Remote Config).
 * RevenueCat key uses XOR so `goog_...` is not visible in APK / BuildConfig strings.
 *
 * Originals (dev reference only — not used as string literals at runtime):
 * - DEFAULT_BASE_URL = https://aihair.brainfungames.com
 * - REVENUECAT_API_KEY = goog_zlhOECUqwLaybFjrQgGCnDAwyHA
 */
object SecureConfig {

    /**
     * AES ciphertext for the default generate host (Firebase fallback).
     * Original: https://aihair.brainfungames.com
     */
    private const val ENCRYPTED_GENERATE_BASE_URL =
        "U2FsdGVkX1/rZxxq0BOBBln8UThl/Waue62Hxgwq1i158KvhpU1CoY6TAryCW0Q9TEXjflrX6hDGdHS1ndWsmQ=="

    /** XOR mask — not the secret itself. */
    private const val RC_XOR = 0x5A

    /**
     * Obfuscated RevenueCat Google SDK key bytes (each char XOR [RC_XOR]).
     * Original: goog_zlhOECUqwLaybFjrQgGCnDAwyHA
     * Rebuilt only in memory when [revenueCatApiKey] is called.
     */
    private val RC_KEY_XOR = intArrayOf(
        0x3D, 0x35, 0x35, 0x3D, 0x05, 0x20, 0x36, 0x32, 0x15, 0x1F, 0x19, 0x0F,
        0x2B, 0x2D, 0x16, 0x3B, 0x23, 0x38, 0x1C, 0x30, 0x28, 0x0B, 0x3D, 0x1D,
        0x19, 0x34, 0x1E, 0x1B, 0x2D, 0x23, 0x12, 0x1B
    )

    fun defaultGenerateBaseUrl(): String =
        runCatching { AESHelper.decrypt(ENCRYPTED_GENERATE_BASE_URL).trim() }
            .getOrDefault("")

    fun revenueCatApiKey(): String {
        val chars = CharArray(RC_KEY_XOR.size) { i ->
            (RC_KEY_XOR[i] xor RC_XOR).toChar()
        }
        return String(chars)
    }
}
