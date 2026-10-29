package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hairstyle.changer.aitryon.Ads.ADSAppManage
import java.util.Locale

object LocaleManager {

    const val KEY_LANGUAGE = "app_language_code"
    const val KEY_LANGUAGE_ONBOARDING_DONE = "language_onboarding_done"
    private const val KEY_PENDING_AFTER_LANGUAGE = "pending_after_language_change"
    const val DEFAULT_LANGUAGE = "en-GB"

    /** No pending navigation after locale recreate. */
    const val PENDING_NONE = 0
    /** First-time Confirm → Intro1 after recreate. */
    const val PENDING_NAV_INTRO = 1
    /** Settings Confirm → pop back after recreate. */
    const val PENDING_NAV_UP = 2

    private val languageSubtitleMap = mapOf(
        "en-GB" to "English (UK)",
        "fr" to "French",
        "hi" to "Hindi",
        "es" to "Spanish",
        "pt" to "Portuguese",
        "ru" to "Russian",
        "in" to "Indonesian",
        "fil" to "Filipino",
        "bn" to "Bangla",
        "pt-BR" to "Portuguese (Brazil)",
        "af" to "Afrikaans",
        "de" to "German",
        "en-CA" to "English (Canada)",
        "ko" to "Korean",
        "nl" to "Dutch"
    )

    fun getLanguageCode(): String {
        if (!PrefManager.isInitialized()) return DEFAULT_LANGUAGE
        return PrefManager.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
    }

    fun setLanguageCode(code: String) {
        PrefManager.putString(KEY_LANGUAGE, code)
    }

    fun getLanguageSubtitle(code: String = getLanguageCode()): String {
        return languageSubtitleMap[code] ?: "English (UK)"
    }

    fun isLanguageOnboardingDone(): Boolean {
        if (!PrefManager.isInitialized()) return false
        return PrefManager.getBoolean(KEY_LANGUAGE_ONBOARDING_DONE, false)
    }

    fun setLanguageOnboardingDone() {
        PrefManager.putBoolean(KEY_LANGUAGE_ONBOARDING_DONE, true)
    }

    fun setPendingAfterLanguageChange(action: Int) {
        PrefManager.putInt(KEY_PENDING_AFTER_LANGUAGE, action)
    }

    /** Returns and clears any navigation that should run after locale Activity recreate. */
    fun consumePendingAfterLanguageChange(): Int {
        if (!PrefManager.isInitialized()) return PENDING_NONE
        val pending = PrefManager.getInt(KEY_PENDING_AFTER_LANGUAGE, PENDING_NONE)
        if (pending != PENDING_NONE) {
            PrefManager.putInt(KEY_PENDING_AFTER_LANGUAGE, PENDING_NONE)
        }
        return pending
    }

    fun completeOnboarding(code: String) {
        setLanguageOnboardingDone()
        setLanguageCode(code)
        applyApplicationLocales(code, skipAppOpen = true)
    }

    fun applyStoredLocale() {
        applyApplicationLocales(getLanguageCode(), skipAppOpen = false)
    }

    fun applyLanguage(code: String) {
        if (code == getLanguageCode()) return
        setLanguageCode(code)
        applyApplicationLocales(code, skipAppOpen = true)
    }

    /**
     * Locale change recreates the Activity (looks like background→foreground).
     * Skip the next App Open so Confirm is not interrupted.
     */
    private fun applyApplicationLocales(code: String, skipAppOpen: Boolean) {
        if (skipAppOpen) {
            ADSAppManage.skipNextAppOpenAd()
            ADSAppManage.blockAppOpenAd(3_000L)
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(toBcp47Tag(code))
        )
    }

    fun wrap(context: Context): Context {
        val locale = localeFromCode(getLanguageCode())
        return applyLocale(context, locale)
    }

    fun applyLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun localeFromCode(code: String): Locale {
        return when (code) {
            "in" -> Locale.forLanguageTag("id")
            "pt-BR" -> Locale.forLanguageTag("pt-BR")
            "en-GB" -> Locale.forLanguageTag("en-GB")
            "en-CA" -> Locale.forLanguageTag("en-CA")
            "fil" -> Locale.forLanguageTag("fil")
            else -> {
                if (code.contains("-")) {
                    Locale.forLanguageTag(code)
                } else {
                    Locale(code)
                }
            }
        }
    }

    private fun toBcp47Tag(code: String): String {
        return when (code) {
            "in" -> "id"
            else -> code
        }
    }
}