package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.content.SharedPreferences
import com.hairstyle.changer.aitryon.domain.model.GenerationType

/**
 * Common SharedPreferences helper.
 * Init once from Application, then use get/set methods anywhere.
 *
 * Example:
 * ```
 * PrefManager.setPrivacyPolicyUrl(url)
 * PrefManager.getPrivacyPolicyUrl()
 * ```
 */
object PrefManager {

    private const val PREF_NAME = "ai_hairstyle_prefs"

    private const val KEY_REMOTE_CONFIG_JSON = "remote_config_json"
    private const val KEY_ADS_CONFIG_JSON = "ads_config_json"
    private const val KEY_PRIVACY_POLICY_URL = "privacy_policy_url"
    private const val KEY_FEEDBACK_URL = "feedback_url"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_DAILY_LIMIT_BLOCKED_UNTIL = "daily_limit_blocked_until"
    private const val KEY_PAGE_LIMIT = "images_page_limit"
    private const val KEY_UNLOCK_ITEMS_JSON = "unlock_items_json"
    private const val KEY_UNLOCK_SEQUENCE_POS = "unlock_sequence_pos"
    private const val KEY_REMOTE_CONFIG_FETCHED = "remote_config_fetched"
    private const val KEY_SELECTED_HAIRSTYLE_TITLE = "selected_hairstyle_title"
    private const val KEY_SELECTED_HAIRSTYLE_API = "selected_hairstyle_api"
    private const val KEY_SELECTED_HAIRSTYLE_IMAGE = "selected_hairstyle_image"
    private const val KEY_SELECTED_HAIR_COLOR_TITLE = "selected_hair_color_title"
    private const val KEY_SELECTED_HAIR_COLOR_API = "selected_hair_color_api"
    private const val KEY_SELECTED_HAIR_COLOR_IMAGE = "selected_hair_color_image"
    private const val KEY_GENERATION_TYPE = "generation_type"
    private const val KEY_HOME_HERO_IMAGES_JSON = "home_hero_images_json"
    private const val KEY_PROMO_IMAGES_JSON = "promo_images_json"
    private const val KEY_UPDATE_APP_JSON = "update_app_json"
    private const val KEY_IN_APP_UPDATE_DIALOG_DAY = "in_app_update_dialog_day"
    private const val KEY_IN_APP_UPDATE_DIALOG_COUNT = "in_app_update_dialog_count"
    private const val KEY_FEEDBACK_SUBMITTED = "feedback_submitted"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        // applicationContext is null during Application.attachBaseContext()
        val appContext = context.applicationContext ?: context
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isInitialized(): Boolean = ::prefs.isInitialized

    private fun prefsOrThrow(): SharedPreferences {
        check(::prefs.isInitialized) { "PrefManager.init(context) must be called first" }
        return prefs
    }

    // region Remote Config – full JSON

    fun setRemoteConfigJson(value: String) {
        prefsOrThrow().edit().putString(KEY_REMOTE_CONFIG_JSON, value).apply()
    }

    fun getRemoteConfigJson(): String {
        return prefsOrThrow().getString(KEY_REMOTE_CONFIG_JSON, "") ?: ""
    }

    /** Raw `ads` object from Remote Config (Splash fetch). */
    fun setAdsConfigJson(value: String) {
        prefsOrThrow().edit().putString(KEY_ADS_CONFIG_JSON, value).apply()
    }

    fun getAdsConfigJson(): String {
        return prefsOrThrow().getString(KEY_ADS_CONFIG_JSON, "") ?: ""
    }

    // endregion

    // region API urls and page limit (from Remote Config)

    fun setPrivacyPolicyUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_PRIVACY_POLICY_URL, value).apply()
    }

    fun getPrivacyPolicyUrl(): String {
        return prefsOrThrow().getString(KEY_PRIVACY_POLICY_URL, "") ?: ""
    }

    fun setDailyLimitBlockedUntil(value: Long) {
        prefsOrThrow().edit().putLong(KEY_DAILY_LIMIT_BLOCKED_UNTIL, value).apply()
    }

    fun getDailyLimitBlockedUntil(): Long {
        return prefsOrThrow().getLong(KEY_DAILY_LIMIT_BLOCKED_UNTIL, 0L)
    }

    /** AES-encrypted API base URL from Firebase `APi.base_url`. */
    fun setBaseUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_BASE_URL, value).apply()
    }

    fun getBaseUrl(): String {
        return prefsOrThrow().getString(KEY_BASE_URL, "") ?: ""
    }

    /** Plain feedback API base URL from Firebase (no encrypt/decrypt). */
    fun setFeedbackUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_FEEDBACK_URL, value).apply()
    }

    fun getFeedbackUrl(): String {
        return prefsOrThrow().getString(KEY_FEEDBACK_URL, "") ?: ""
    }

    private const val VIDEO_URL_PREFIX = "video_url_"

    fun setVideoUrl(title: String, url: String) {
        if (title.isBlank()) return
        prefsOrThrow().edit()
            .putString("$VIDEO_URL_PREFIX$title", url)
            .apply()
    }

    fun getVideoUrl(title: String): String {
        if (title.isBlank()) return ""
        return prefsOrThrow().getString("$VIDEO_URL_PREFIX$title", "") ?: ""
    }

    /** Raw `ads.unlock_items` JSON object from Remote Config (blank → app defaults). */
    fun setUnlockItemsJson(value: String) {
        prefsOrThrow().edit().putString(KEY_UNLOCK_ITEMS_JSON, value).apply()
    }

    /** How many unlock ads were completed so far — picks the next step of `ads_sequence`. */
    fun getUnlockSequencePos(): Int = prefsOrThrow().getInt(KEY_UNLOCK_SEQUENCE_POS, 0)

    fun setUnlockSequencePos(value: Int) {
        prefsOrThrow().edit().putInt(KEY_UNLOCK_SEQUENCE_POS, value).apply()
    }

    fun getUnlockItemsJson(): String =
        prefsOrThrow().getString(KEY_UNLOCK_ITEMS_JSON, "").orEmpty()

    fun setImagesPageLimit(value: Int) {
        prefsOrThrow().edit().putInt(KEY_PAGE_LIMIT, value).apply()
    }

    fun getImagesPageLimit(default: Int): Int {
        val stored = prefsOrThrow().getInt(KEY_PAGE_LIMIT, default)
        return stored.takeIf { it > 0 } ?: default
    }

    // endregion

    // region selected style / color (for flow after category)

    fun setSelectedHairstyle(title: String, imageUrl: String, apiValue: String = "") {
        prefsOrThrow().edit()
            .putString(KEY_SELECTED_HAIRSTYLE_TITLE, title)
            .putString(KEY_SELECTED_HAIRSTYLE_IMAGE, imageUrl)
            .putString(KEY_SELECTED_HAIRSTYLE_API, apiValue)
            .apply()
    }

    fun getSelectedHairstyleTitle(): String =
        prefsOrThrow().getString(KEY_SELECTED_HAIRSTYLE_TITLE, "") ?: ""

    fun getSelectedHairstyleApiValue(): String =
        prefsOrThrow().getString(KEY_SELECTED_HAIRSTYLE_API, "") ?: ""

    fun getSelectedHairstyleImage(): String =
        prefsOrThrow().getString(KEY_SELECTED_HAIRSTYLE_IMAGE, "") ?: ""

    fun setSelectedHairColor(title: String, imageUrl: String, apiValue: String = "") {
        prefsOrThrow().edit()
            .putString(KEY_SELECTED_HAIR_COLOR_TITLE, title)
            .putString(KEY_SELECTED_HAIR_COLOR_IMAGE, imageUrl)
            .putString(KEY_SELECTED_HAIR_COLOR_API, apiValue)
            .apply()
    }

    fun getSelectedHairColorTitle(): String =
        prefsOrThrow().getString(KEY_SELECTED_HAIR_COLOR_TITLE, "") ?: ""

    fun getSelectedHairColorApiValue(): String =
        prefsOrThrow().getString(KEY_SELECTED_HAIR_COLOR_API, "") ?: ""

    fun getSelectedHairColorImage(): String =
        prefsOrThrow().getString(KEY_SELECTED_HAIR_COLOR_IMAGE, "") ?: ""

    fun clearSelectedHairColor() {
        prefsOrThrow().edit()
            .remove(KEY_SELECTED_HAIR_COLOR_TITLE)
            .remove(KEY_SELECTED_HAIR_COLOR_API)
            .remove(KEY_SELECTED_HAIR_COLOR_IMAGE)
            .apply()
    }

    fun setGenerationType(type: GenerationType) {
        prefsOrThrow().edit().putString(KEY_GENERATION_TYPE, type.name).apply()
    }

    fun getGenerationType(): GenerationType {
        val stored = prefsOrThrow().getString(KEY_GENERATION_TYPE, GenerationType.HAIRSTYLE.name)
            ?: GenerationType.HAIRSTYLE.name
        return runCatching { GenerationType.valueOf(stored) }
            .getOrDefault(GenerationType.HAIRSTYLE)
    }

    fun setHomeHeroImagesJson(value: String) {
        prefsOrThrow().edit().putString(KEY_HOME_HERO_IMAGES_JSON, value).apply()
    }

    fun getHomeHeroImagesJson(): String {
        return prefsOrThrow().getString(KEY_HOME_HERO_IMAGES_JSON, "") ?: ""
    }

    fun setPromoImagesJson(value: String) {
        prefsOrThrow().edit().putString(KEY_PROMO_IMAGES_JSON, value).apply()
    }

    fun getPromoImagesJson(): String {
        return prefsOrThrow().getString(KEY_PROMO_IMAGES_JSON, "") ?: ""
    }

    fun setUpdateAppJson(value: String) {
        prefsOrThrow().edit().putString(KEY_UPDATE_APP_JSON, value).apply()
    }

    fun getUpdateAppJson(): String {
        return prefsOrThrow().getString(KEY_UPDATE_APP_JSON, "") ?: ""
    }

    fun getInAppUpdateDialogDay(): String {
        return prefsOrThrow().getString(KEY_IN_APP_UPDATE_DIALOG_DAY, "") ?: ""
    }

    fun setInAppUpdateDialogDay(value: String) {
        prefsOrThrow().edit().putString(KEY_IN_APP_UPDATE_DIALOG_DAY, value).apply()
    }

    fun getInAppUpdateDialogCount(): Int {
        return prefsOrThrow().getInt(KEY_IN_APP_UPDATE_DIALOG_COUNT, 0)
    }

    fun setInAppUpdateDialogCount(value: Int) {
        prefsOrThrow().edit().putInt(KEY_IN_APP_UPDATE_DIALOG_COUNT, value.coerceAtLeast(0)).apply()
    }

    // endregion

    // region fetch flag

    fun setRemoteConfigFetched(value: Boolean) {
        prefsOrThrow().edit().putBoolean(KEY_REMOTE_CONFIG_FETCHED, value).apply()
    }

    fun getRemoteConfigFetched(): Boolean {
        return prefsOrThrow().getBoolean(KEY_REMOTE_CONFIG_FETCHED, false)
    }

    // endregion

    // region generic helpers (add new keys easily)

    fun putString(key: String, value: String) {
        prefsOrThrow().edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String = ""): String {
        return prefsOrThrow().getString(key, default) ?: default
    }

    fun putBoolean(key: String, value: Boolean) {
        prefsOrThrow().edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return prefsOrThrow().getBoolean(key, default)
    }

    fun putInt(key: String, value: Int) {
        prefsOrThrow().edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int {
        return prefsOrThrow().getInt(key, default)
    }

    fun putLong(key: String, value: Long) {
        prefsOrThrow().edit().putLong(key, value).apply()
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return prefsOrThrow().getLong(key, default)
    }

    fun remove(key: String) {
        prefsOrThrow().edit().remove(key).apply()
    }

    fun setFeedbackSubmitted(value: Boolean) {
        prefsOrThrow().edit().putBoolean(KEY_FEEDBACK_SUBMITTED, value).apply()
    }

    fun isFeedbackSubmitted(): Boolean {
        return prefsOrThrow().getBoolean(KEY_FEEDBACK_SUBMITTED, false)
    }

    fun clear() {
        prefsOrThrow().edit().clear().apply()
    }

    // endregion
}
