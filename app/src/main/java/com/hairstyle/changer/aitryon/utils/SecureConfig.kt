package com.hairstyle.changer.aitryon.utils

/**
 * Runtime-only secrets — plain values are never stored as string literals.
 * Generate base URL uses AES (same as Firebase Remote Config).
 *
 * Originals (dev reference only — not used as string literals at runtime):
 * - DEFAULT_BASE_URL = https://aihair.brainfungames.com
 */
object SecureConfig {

    /**
     * AES ciphertext for the default generate host (Firebase fallback).
     * Original: https://aihair.brainfungames.com
     */
    private const val ENCRYPTED_GENERATE_BASE_URL =
        "U2FsdGVkX1/rZxxq0BOBBln8UThl/Waue62Hxgwq1i158KvhpU1CoY6TAryCW0Q9TEXjflrX6hDGdHS1ndWsmQ=="

    fun defaultGenerateBaseUrl(): String =
        runCatching { AESHelper.decrypt(ENCRYPTED_GENERATE_BASE_URL).trim() }
            .getOrDefault("")

}
