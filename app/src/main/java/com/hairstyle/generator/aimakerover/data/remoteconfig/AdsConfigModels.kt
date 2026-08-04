package com.hairstyle.generator.aimakerover.data.remoteconfig

import com.google.gson.annotations.SerializedName

/**
 * Root can be full Remote Config JSON (`{ "ads": {...} }`) or the `ads` object alone.
 */
data class AdsConfigRoot(
    @SerializedName("ads") val ads: AdsSection? = null,
    @SerializedName("consent_form_show_screen") val consentFormShowScreen: String? = null,
    @SerializedName("Ads_Loading_Types") val adsLoadingTypes: String? = null
)

data class AdsSection(
    @SerializedName("privacy_policy_url") val privacyPolicyUrl: String? = null,
    @SerializedName("consent_form_show_screen") val consentFormShowScreen: String? = null,
    @SerializedName("Ads_Loading_Types") val adsLoadingTypes: String? = null,
    @SerializedName("exit_dialog_show") val exitDialogShow: Boolean? = null,
    /** Native CTA button background — e.g. `"#000000"`. */
    @SerializedName("ads_button_color") val adsButtonColor: String? = null,
    /** Native CTA button text — e.g. `"#FFFFFF"`. */
    @SerializedName("ads_button_text_color") val adsButtonTextColor: String? = null,
    /** "Ad" badge (`adsLayout`) background — e.g. `"#DBA300"`. */
    @SerializedName("ads_layout_background_color") val adsLayoutBackgroundColor: String? = null,
    /** "Ad" badge (`adsLayout`) text — e.g. `"#FFFFFF"`. */
    @SerializedName("ads_layout_text_color") val adsLayoutTextColor: String? = null,
    /** Native card root (`llMainLayout`) background — e.g. `"#191919"`. */
    @SerializedName("ads_background_color") val adsBackgroundColor: String? = null,
    /** Headline / body / advertiser / price / store text — e.g. `"#FFFFFF"`. */
    @SerializedName("ads_all_text_color") val adsAllTextColor: String? = null,
    /**
     * When `true`, Progress screen skips the generate API and opens Result with the user photo.
     * Default / missing → `false` (normal generate flow).
     */
    @SerializedName("temporary_skip_generate_api") val temporarySkipGenerateApi: Boolean? = null,
    /**
     * Firebase kill-switch for all ads (feeds [ADSMainClass.getAds_Free]).
     * `false` → ads run; `true` → all ads stop. Default / missing → `false`.
     */
    @SerializedName("ads_blocked") val adsBlocked: Boolean? = null,
    @SerializedName("inter_and_reward") val interAndReward: InterAndRewardConfig? = null,
    @SerializedName("splash_screen") val splashScreen: SplashAdsConfig? = null,
    @SerializedName("language_screen") val languageScreen: LanguageAdsConfig? = null,
    @SerializedName("setting_screen") val settingScreen: SettingScreenAdsConfig? = null
)

data class InterAndRewardConfig(
    @SerializedName("inter_ads_show") val interAdsShow: Boolean? = null,
    /** `PreLoad` | `Show` (also accepts `Load`) — applies to click + back interstitials. */
    @SerializedName("inter_ads_load_type") val interAdsLoadType: String? = null,
    @SerializedName("inter_ads_click") val interAdsClick: Int? = null,
    /**
     * Intro1/1b/1c/1d/2/3 only — separate from [interAdsClick].
     * Example: `1` → show inter on every intro Continue / option click.
     */
    @SerializedName("intro_inter_ads_click_count") val introInterAdsClickCount: Int? = null,
    /**
     * Intro1–3 system back:
     * `true` → go to next intro screen; `false` → normal back (current behavior).
     */
    @SerializedName("intro_back_send_to_next") val introBackSendToNext: Boolean? = null,
    @SerializedName("inter_ad_id") val interAdId: String? = null,
    @SerializedName("inter_back_ads_show") val interBackAdsShow: Boolean? = null,
    @SerializedName("inter_back_ads_click") val interBackAdsClick: Int? = null,
    @SerializedName("inter_back_ad_id") val interBackAdId: String? = null,
    @SerializedName("reward_ads_show") val rewardAdsShow: Boolean? = null,
    @SerializedName("reward_ad_id") val rewardAdId: String? = null,
    @SerializedName("bottom_navigation_click_ads_show") val bottomNavigationClickAdsShow: Boolean? = null,
    @SerializedName("bottom_navigation_click") val bottomNavigationClick: Int? = null
)

data class SplashAdsConfig(
    @SerializedName("splash_app_open_ads_show") val splashAppOpenAdsShow: Boolean? = null,
    @SerializedName("splash_app_open_after_count") val splashAppOpenAfterCount: Int? = null,
    @SerializedName("splash_ads_type") val splashAdsType: String? = null,
    @SerializedName("app_open_show_in_background") val appOpenShowInBackground: Boolean? = null,
    @SerializedName("app_open_daily_show_count") val appOpenDailyShowCount: Int? = null,
    @SerializedName("app_open_id") val appOpenId: String? = null,
    @SerializedName("splash_inter_id") val splashInterId: String? = null
)

data class LanguageAdsConfig(
    @SerializedName("language_screen_bottom_ad_show") val languageScreenBottomAdShow: Boolean? = null,
    /** `"native"` | `"banner"`. Also accepts Firebase typo `languge_ads_type`. */
    @SerializedName(value = "language_ads_type", alternate = ["languge_ads_type"])
    val languageAdsType: String? = null,
    /** `"small"` | `"medium"` | `"big"` — default small. */
    @SerializedName("language_ads_size") val languageAdsSize: String? = null,
    @SerializedName("language_banner_id") val languageBannerId: String? = null,
    @SerializedName("language_native_id") val languageNativeId: String? = null,
    @SerializedName("language_inter_ads_show") val languageInterAdsShow: Boolean? = null
)

data class SettingScreenAdsConfig(
    @SerializedName("setting_show_bottom_ads") val showBottomAds: Boolean? = null,
    @SerializedName("setting_ads_size") val adsSize: String? = null,
    @SerializedName("setting_ads_type") val adsType: String? = null,
    @SerializedName("setting_native_ads_id") val nativeAdsId: String? = null,
    @SerializedName("setting_banner_ads_id") val bannerAdsId: String? = null,
    @SerializedName("setting_center_ads_show") val centerAdsShow: Boolean? = null,
    @SerializedName("setting_center_ads_size") val centerAdsSize: String? = null,
    @SerializedName("setting_center_ads_type") val centerAdsType: String? = null,
    @SerializedName("setting_center_native_ads_id") val centerNativeAdsId: String? = null,
    @SerializedName("setting_center_banner_ads_id") val centerBannerAdsId: String? = null
)

/**
 * Maps one Remote Config screen object → SharedPreferences keys in [ADSMainClass].
 */
data class ScreenAdsPrefKeys(
    val remoteObjectKey: String,
    val remoteShow: String,
    val remoteSize: String,
    val remoteType: String,
    val remoteNative: String,
    val remoteBanner: String,
    val prefShow: String,
    val prefSize: String,
    val prefType: String,
    val prefNative: String,
    val prefBanner: String
)
