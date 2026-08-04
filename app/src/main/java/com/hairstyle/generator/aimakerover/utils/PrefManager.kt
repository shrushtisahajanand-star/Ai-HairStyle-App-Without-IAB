package com.hairstyle.generator.aimakerover.utils

import android.content.Context
import android.content.SharedPreferences
import com.hairstyle.generator.aimakerover.domain.model.GenerationType

/**
 * Common SharedPreferences helper.
 * Init once from Application, then use get/set methods anywhere.
 *
 * Example:
 * ```
 * PrefManager.setImagesBaseUrl(encryptedUrl)
 * PrefManager.getImagesBaseUrl()
 * ```
 */
object PrefManager {

    private const val PREF_NAME = "ai_hairstyle_prefs"

    private const val KEY_REMOTE_CONFIG_JSON = "remote_config_json"
    private const val KEY_ADS_CONFIG_JSON = "ads_config_json"
    private const val KEY_IMAGES_BASE_URL = "images_base_url"
    private const val KEY_VIDEOS_API_URL = "videos_api_url"
    private const val KEY_GENERATE_BASE_URL = "generate_base_url"
    private const val KEY_PRIVACY_POLICY_URL = "privacy_policy_url"
    private const val KEY_FEEDBACK_URL = "feedback_url"
    private const val KEY_PAGE_LIMIT = "images_page_limit"
    private const val KEY_REMOTE_CONFIG_FETCHED = "remote_config_fetched"
    private const val KEY_PREMIUM_PLAN_DETAILS_JSON = "premium_plan_details_json"
    private const val KEY_ACTIVE_PREMIUM_PLAN = "active_premium_plan"
    private const val KEY_ACTIVE_PREMIUM_PLANS_JSON = "active_premium_plans_json"
    private const val KEY_PREMIUM_EXPIRES_AT = "premium_expires_at"
    private const val KEY_PREMIUM_IMAGES_USED = "premium_images_used"
    private const val KEY_PREMIUM_IMAGE_ACCESS_LIMIT = "premium_image_access_limit"
    private const val KEY_PREMIUM_PURCHASE_TOKEN = "premium_purchase_token"
    private const val KEY_PREMIUM_PRODUCT_ID = "premium_product_id"
    /** RevenueCat originalAppUserId — sent as generate API `purchase_id` when premium. */
    private const val KEY_RC_ORIGINAL_APP_USER_ID = "rc_original_app_user_id"
    private const val KEY_FREE_IMAGES_USED = "free_images_used"
    private const val KEY_YEARLY_LAST_MONTHLY_CREDIT_AT = "yearly_last_monthly_credit_at"
    private const val KEY_PREMIUM_PLAN_STARTED_AT = "premium_plan_started_at"
    private const val KEY_PREMIUM_QUOTA_PERIOD_STARTED_AT = "premium_quota_period_started_at"
    private const val KEY_PREMIUM_PERIOD_IMAGES_USED = "premium_period_images_used"
    private const val KEY_PREMIUM_CURRENT_DAY_INDEX = "premium_current_day_index"
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

    // region images base_url (stores encrypted value from RC)

    fun setImagesBaseUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_IMAGES_BASE_URL, value).apply()
    }

    fun getImagesBaseUrl(): String {
        return prefsOrThrow().getString(KEY_IMAGES_BASE_URL, "") ?: ""
    }

    fun setVideosApiUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_VIDEOS_API_URL, value).apply()
    }

    fun getVideosApiUrl(): String {
        return prefsOrThrow().getString(KEY_VIDEOS_API_URL, "") ?: ""
    }

    fun setGenerateBaseUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_GENERATE_BASE_URL, value).apply()
    }

    fun getGenerateBaseUrl(): String {
        return prefsOrThrow().getString(KEY_GENERATE_BASE_URL, "") ?: ""
    }

    fun setPrivacyPolicyUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_PRIVACY_POLICY_URL, value).apply()
    }

    fun getPrivacyPolicyUrl(): String {
        return prefsOrThrow().getString(KEY_PRIVACY_POLICY_URL, "") ?: ""
    }

    /** Plain feedback API base URL from Firebase (no encrypt/decrypt). */
    fun setFeedbackUrl(value: String) {
        prefsOrThrow().edit().putString(KEY_FEEDBACK_URL, value).apply()
    }

    fun getFeedbackUrl(): String {
        return prefsOrThrow().getString(KEY_FEEDBACK_URL, "") ?: ""
    }

    private const val VIDEO_URL_PREFIX = "video_url_"

    fun setVideoUrl(title: String, encryptedUrl: String) {
        if (title.isBlank()) return
        prefsOrThrow().edit()
            .putString("$VIDEO_URL_PREFIX$title", encryptedUrl)
            .apply()
    }

    fun getVideoUrlEncrypted(title: String): String {
        if (title.isBlank()) return ""
        return prefsOrThrow().getString("$VIDEO_URL_PREFIX$title", "") ?: ""
    }

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

    // region Premium plan details (from Remote Config)

    fun setPremiumPlanDetailsJson(value: String) {
        prefsOrThrow().edit().putString(KEY_PREMIUM_PLAN_DETAILS_JSON, value).apply()
    }

    fun getPremiumPlanDetailsJson(): String {
        return prefsOrThrow().getString(KEY_PREMIUM_PLAN_DETAILS_JSON, "") ?: ""
    }

    fun setActivePremiumPlan(value: String) {
        prefsOrThrow().edit().putString(KEY_ACTIVE_PREMIUM_PLAN, value).apply()
    }

    fun getActivePremiumPlan(): String {
        return prefsOrThrow().getString(KEY_ACTIVE_PREMIUM_PLAN, "") ?: ""
    }

    fun setActivePremiumPlansJson(value: String) {
        prefsOrThrow().edit().putString(KEY_ACTIVE_PREMIUM_PLANS_JSON, value).apply()
    }

    fun getActivePremiumPlansJson(): String {
        return prefsOrThrow().getString(KEY_ACTIVE_PREMIUM_PLANS_JSON, "") ?: ""
    }

    fun setPremiumExpiresAt(value: Long) {
        prefsOrThrow().edit().putLong(KEY_PREMIUM_EXPIRES_AT, value).apply()
    }

    fun getPremiumExpiresAt(): Long {
        return prefsOrThrow().getLong(KEY_PREMIUM_EXPIRES_AT, 0L)
    }

    fun setPremiumImagesUsed(value: Int) {
        prefsOrThrow().edit().putInt(KEY_PREMIUM_IMAGES_USED, value.coerceAtLeast(0)).apply()
    }

    fun getPremiumImagesUsed(): Int {
        return prefsOrThrow().getInt(KEY_PREMIUM_IMAGES_USED, 0).coerceAtLeast(0)
    }

    fun setPremiumImageAccessLimit(value: Int) {
        prefsOrThrow().edit().putInt(KEY_PREMIUM_IMAGE_ACCESS_LIMIT, value.coerceAtLeast(0)).apply()
    }

    fun getPremiumImageAccessLimit(): Int {
        return prefsOrThrow().getInt(KEY_PREMIUM_IMAGE_ACCESS_LIMIT, 0).coerceAtLeast(0)
    }

    fun setPremiumPurchaseToken(value: String) {
        prefsOrThrow().edit().putString(KEY_PREMIUM_PURCHASE_TOKEN, value).apply()
    }

    fun getPremiumPurchaseToken(): String {
        return prefsOrThrow().getString(KEY_PREMIUM_PURCHASE_TOKEN, "") ?: ""
    }

    fun setPremiumProductId(value: String) {
        prefsOrThrow().edit().putString(KEY_PREMIUM_PRODUCT_ID, value).apply()
    }

    fun getPremiumProductId(): String {
        return prefsOrThrow().getString(KEY_PREMIUM_PRODUCT_ID, "") ?: ""
    }

    fun setRcOriginalAppUserId(value: String) {
        prefsOrThrow().edit().putString(KEY_RC_ORIGINAL_APP_USER_ID, value.trim()).apply()
    }

    fun getRcOriginalAppUserId(): String {
        return prefsOrThrow().getString(KEY_RC_ORIGINAL_APP_USER_ID, "")?.trim().orEmpty()
    }

    fun setFreeImagesUsed(value: Int) {
        prefsOrThrow().edit().putInt(KEY_FREE_IMAGES_USED, value.coerceAtLeast(0)).apply()
    }

    fun getFreeImagesUsed(): Int {
        return prefsOrThrow().getInt(KEY_FREE_IMAGES_USED, 0).coerceAtLeast(0)
    }

    fun setYearlyLastMonthlyCreditAt(value: Long) {
        prefsOrThrow().edit().putLong(KEY_YEARLY_LAST_MONTHLY_CREDIT_AT, value.coerceAtLeast(0L)).apply()
    }

    fun getYearlyLastMonthlyCreditAt(): Long {
        return prefsOrThrow().getLong(KEY_YEARLY_LAST_MONTHLY_CREDIT_AT, 0L).coerceAtLeast(0L)
    }

    fun setPremiumPlanStartedAt(value: Long) {
        prefsOrThrow().edit().putLong(KEY_PREMIUM_PLAN_STARTED_AT, value.coerceAtLeast(0L)).apply()
    }

    fun getPremiumPlanStartedAt(): Long {
        return prefsOrThrow().getLong(KEY_PREMIUM_PLAN_STARTED_AT, 0L).coerceAtLeast(0L)
    }

    fun setPremiumQuotaPeriodStartedAt(value: Long) {
        prefsOrThrow().edit().putLong(KEY_PREMIUM_QUOTA_PERIOD_STARTED_AT, value.coerceAtLeast(0L)).apply()
    }

    fun getPremiumQuotaPeriodStartedAt(): Long {
        return prefsOrThrow().getLong(KEY_PREMIUM_QUOTA_PERIOD_STARTED_AT, 0L).coerceAtLeast(0L)
    }

    fun setPremiumPeriodImagesUsed(value: Int) {
        prefsOrThrow().edit().putInt(KEY_PREMIUM_PERIOD_IMAGES_USED, value.coerceAtLeast(0)).apply()
    }

    fun getPremiumPeriodImagesUsed(): Int {
        return prefsOrThrow().getInt(KEY_PREMIUM_PERIOD_IMAGES_USED, 0).coerceAtLeast(0)
    }

    fun setPremiumCurrentDayIndex(value: Int) {
        prefsOrThrow().edit().putInt(KEY_PREMIUM_CURRENT_DAY_INDEX, value).apply()
    }

    fun getPremiumCurrentDayIndex(): Int {
        return prefsOrThrow().getInt(KEY_PREMIUM_CURRENT_DAY_INDEX, -1)
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
