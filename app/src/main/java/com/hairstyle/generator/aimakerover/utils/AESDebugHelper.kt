package com.hairstyle.generator.aimakerover.utils

import android.util.Log
import com.hairstyle.generator.aimakeover.BuildConfig

/**
 * Debug-only AES encrypt/decrypt checks for Logcat.
 *
 * Filter: **AESDebug**
 *
 * ```
 * // Encrypt plain text (e.g. new Firebase base_url)
 * AESDebugHelper.logEncrypt("base_url", "https://aihair.brainfungames.com/api/images")
 *
 * // Decrypt cipher from Firebase / prefs
 * AESDebugHelper.logDecrypt("base_url", "U2FsdGVkX1...")
 *
 * // After Splash — all stored Remote Config URLs
 * AESDebugHelper.logRemoteConfigUrlsFromPrefs()
 * ```
 */
object AESDebugHelper {

    private const val TAG = "AESDebug"

    /** Encrypt [plainText] and log cipher + round-trip verify. Debug builds only. */
    fun logEncrypt(
        label: String,
        plainText: String,
        password: String = AESHelper.PASSWORD
    ) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            val cipher = AESHelper.encrypt(plainText, password)
            val roundTrip = AESHelper.decrypt(cipher, password)
            Log.d(TAG, "[$label] ENCRYPT ok")
            Log.d(TAG, "  plain     = $plainText")
            Log.d(TAG, "  cipher    = $cipher")
            Log.d(TAG, "  roundTrip = ${roundTrip == plainText} | decrypted = $roundTrip")
        }.onFailure {
            Log.e(TAG, "[$label] ENCRYPT failed | plain=$plainText", it)
        }
    }

    /** Decrypt [cipherText] and log plain result. Debug builds only. */
    fun logDecrypt(
        label: String,
        cipherText: String,
        password: String = AESHelper.PASSWORD
    ) {
        if (!BuildConfig.DEBUG) return
        if (cipherText.isBlank()) {
            Log.w(TAG, "[$label] DECRYPT skipped — blank cipher")
            return
        }
        if (cipherText.startsWith("http", ignoreCase = true)) {
            Log.d(TAG, "[$label] already plain URL (not encrypted): $cipherText")
            return
        }
        runCatching {
            val plain = AESHelper.decrypt(cipherText, password)
            Log.d(TAG, "[$label] DECRYPT ok")
            Log.d(TAG, "  cipher = ${cipherPreview(cipherText)}")
            Log.d(TAG, "  plain  = $plain")
        }.onFailure {
            Log.e(TAG, "[$label] DECRYPT failed | cipher=${cipherPreview(cipherText)}", it)
        }
    }

    /** Log encrypt + decrypt for the same plain text in one call. */
    fun logRoundTrip(label: String, plainText: String, password: String = AESHelper.PASSWORD) {
        logEncrypt(label, plainText, password)
    }

    /** Decrypt all Remote Config URL fields saved in [PrefManager]. Call after Splash fetch. */
    fun logRemoteConfigUrlsFromPrefs() {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "—— Remote Config URLs (from prefs) ——")
        logDecrypt("images_base_url", PrefManager.getImagesBaseUrl())
        logDecrypt("videos_url", PrefManager.getVideosApiUrl())
        logDecrypt("generate_base_url", PrefManager.getGenerateBaseUrl())
        logDecrypt("privacy_policy_url", PrefManager.getPrivacyPolicyUrl())
        logDecrypt("terms_url", PrefManager.getTermsUrl())
        val feedback = PrefManager.getFeedbackUrl()
        if (feedback.startsWith("http", ignoreCase = true)) {
            Log.d(TAG, "[feedback_url] plain (not encrypted) = $feedback")
        } else {
            logDecrypt("feedback_url", feedback)
        }
        Log.d(TAG, "—— end ——")
    }

    private fun cipherPreview(cipher: String): String {
        return if (cipher.length <= 120) cipher else cipher.take(120) + "…(${cipher.length} chars)"
    }
}
