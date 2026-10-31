package com.hairstyle.changer.aitryon.remoteconfig

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.ADSNativeDisplay
import com.hairstyle.changer.aitryon.utils.PrefManager

/**
 * Splash â†’ Remote Config fetch â†’ applies full `ads` block into [ADSMainClass] SharedPreferences
 * (and stores raw ads JSON in [PrefManager]).
 */
object AdsConfigApplier {

    private const val TAG = "AdsConfigApplier"
    private const val DEFAULT_CONSENT_SCREEN = ADSMainClass.CONSENT_SCREEN_SPLASH

    private val BOTTOM_SCREENS = listOf(
        ScreenAdsPrefKeys(
            remoteObjectKey = "intro_screen1",
            remoteShow = "intro1_show_bottom_ads",
            remoteSize = "intro1_ads_size",
            remoteType = "intro1_ads_type",
            remoteNative = "intro1_native_ads_id",
            remoteBanner = "intro1_banner_ads_id",
            prefShow = ADSMainClass.INTRO1_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.INTRO1_ADS_SIZE,
            prefType = ADSMainClass.INTRO1_ADS_TYPE,
            prefNative = ADSMainClass.INTRO1_NATIVE,
            prefBanner = ADSMainClass.INTRO1_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "intro_screen1b",
            remoteShow = "intro1b_show_bottom_ads",
            remoteSize = "intro1b_ads_size",
            remoteType = "intro1b_ads_type",
            remoteNative = "intro1b_native_ads_id",
            remoteBanner = "intro1b_banner_ads_id",
            prefShow = ADSMainClass.INTRO1B_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.INTRO1B_ADS_SIZE,
            prefType = ADSMainClass.INTRO1B_ADS_TYPE,
            prefNative = ADSMainClass.INTRO1B_NATIVE,
            prefBanner = ADSMainClass.INTRO1B_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "intro_screen1c",
            remoteShow = "intro1c_show_bottom_ads",
            remoteSize = "intro1c_ads_size",
            remoteType = "intro1c_ads_type",
            remoteNative = "intro1c_native_ads_id",
            remoteBanner = "intro1c_banner_ads_id",
            prefShow = ADSMainClass.INTRO1C_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.INTRO1C_ADS_SIZE,
            prefType = ADSMainClass.INTRO1C_ADS_TYPE,
            prefNative = ADSMainClass.INTRO1C_NATIVE,
            prefBanner = ADSMainClass.INTRO1C_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "intro_screen1d",
            remoteShow = "intro1d_show_bottom_ads",
            remoteSize = "intro1d_ads_size",
            remoteType = "intro1d_ads_type",
            remoteNative = "intro1d_native_ads_id",
            remoteBanner = "intro1d_banner_ads_id",
            prefShow = ADSMainClass.INTRO1D_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.INTRO1D_ADS_SIZE,
            prefType = ADSMainClass.INTRO1D_ADS_TYPE,
            prefNative = ADSMainClass.INTRO1D_NATIVE,
            prefBanner = ADSMainClass.INTRO1D_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "intro_screen2",
            remoteShow = "intro2_show_bottom_ads",
            remoteSize = "intro2_ads_size",
            remoteType = "intro2_ads_type",
            remoteNative = "intro2_native_ads_id",
            remoteBanner = "intro2_banner_ads_id",
            prefShow = ADSMainClass.INTRO2_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.INTRO2_ADS_SIZE,
            prefType = ADSMainClass.INTRO2_ADS_TYPE,
            prefNative = ADSMainClass.INTRO2_NATIVE,
            prefBanner = ADSMainClass.INTRO2_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "intro_screen3",
            remoteShow = "intro3_show_bottom_ads",
            remoteSize = "intro3_ads_size",
            remoteType = "intro3_ads_type",
            remoteNative = "intro3_native_ads_id",
            remoteBanner = "intro3_banner_ads_id",
            prefShow = ADSMainClass.INTRO3_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.INTRO3_ADS_SIZE,
            prefType = ADSMainClass.INTRO3_ADS_TYPE,
            prefNative = ADSMainClass.INTRO3_NATIVE,
            prefBanner = ADSMainClass.INTRO3_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "home_screen",
            remoteShow = "home_show_bottom_ads",
            remoteSize = "home_ads_size",
            remoteType = "home_ads_type",
            remoteNative = "home_native_ads_id",
            remoteBanner = "home_banner_ads_id",
            prefShow = "HomeScreenBottomShow",
            prefSize = ADSMainClass.HOME_SCREEN_ADS_SIZE,
            prefType = "HomeScreenAdsType",
            prefNative = ADSMainClass.HOME_SCREEN_NATIVE,
            prefBanner = ADSMainClass.HOME_SCREEN_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "select_image_screen",
            remoteShow = "select_image_show_bottom_ads",
            remoteSize = "select_image_ads_size",
            remoteType = "select_image_ads_type",
            remoteNative = "select_image_native_ads_id",
            remoteBanner = "select_image_banner_ads_id",
            prefShow = ADSMainClass.SELECT_IMAGE_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.SELECT_IMAGE_ADS_SIZE,
            prefType = ADSMainClass.SELECT_IMAGE_ADS_TYPE,
            prefNative = ADSMainClass.SELECT_IMAGE_NATIVE,
            prefBanner = ADSMainClass.SELECT_IMAGE_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "add_photo_screen",
            remoteShow = "add_photo_show_bottom_ads",
            remoteSize = "add_photo_ads_size",
            remoteType = "add_photo_ads_type",
            remoteNative = "add_photo_native_ads_id",
            remoteBanner = "add_photo_banner_ads_id",
            prefShow = ADSMainClass.ADD_PHOTO_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.ADD_PHOTO_ADS_SIZE,
            prefType = ADSMainClass.ADD_PHOTO_ADS_TYPE,
            prefNative = ADSMainClass.ADD_PHOTO_NATIVE,
            prefBanner = ADSMainClass.ADD_PHOTO_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "gallery_photos_screen",
            remoteShow = "gallery_photos_show_bottom_ads",
            remoteSize = "gallery_photos_ads_size",
            remoteType = "gallery_photos_ads_type",
            remoteNative = "gallery_photos_native_ads_id",
            remoteBanner = "gallery_photos_banner_ads_id",
            prefShow = ADSMainClass.GALLERY_PHOTOS_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.GALLERY_PHOTOS_ADS_SIZE,
            prefType = ADSMainClass.GALLERY_PHOTOS_ADS_TYPE,
            prefNative = ADSMainClass.GALLERY_PHOTOS_NATIVE,
            prefBanner = ADSMainClass.GALLERY_PHOTOS_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "progress_screen",
            remoteShow = "progress_show_bottom_ads",
            remoteSize = "progress_ads_size",
            remoteType = "progress_ads_type",
            remoteNative = "progress_native_ads_id",
            remoteBanner = "progress_banner_ads_id",
            prefShow = ADSMainClass.PROGRESS_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.PROGRESS_ADS_SIZE,
            prefType = ADSMainClass.PROGRESS_ADS_TYPE,
            prefNative = ADSMainClass.PROGRESS_NATIVE,
            prefBanner = ADSMainClass.PROGRESS_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "result_screen",
            remoteShow = "result_show_bottom_ads",
            remoteSize = "result_ads_size",
            remoteType = "result_ads_type",
            remoteNative = "result_native_ads_id",
            remoteBanner = "result_banner_ads_id",
            prefShow = ADSMainClass.RESULT_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.RESULT_ADS_SIZE,
            prefType = ADSMainClass.RESULT_ADS_TYPE,
            prefNative = ADSMainClass.RESULT_NATIVE,
            prefBanner = ADSMainClass.RESULT_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "user_creation_screen",
            remoteShow = "user_creation_show_bottom_ads",
            remoteSize = "user_creation_ads_size",
            remoteType = "user_creation_ads_type",
            remoteNative = "user_creation_native_ads_id",
            remoteBanner = "user_creation_banner_ads_id",
            prefShow = ADSMainClass.USER_CREATION_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.USER_CREATION_ADS_SIZE,
            prefType = ADSMainClass.USER_CREATION_ADS_TYPE,
            prefNative = ADSMainClass.USER_CREATION_NATIVE,
            prefBanner = ADSMainClass.USER_CREATION_BANNER
        ),
        ScreenAdsPrefKeys(
            remoteObjectKey = "feedback_screen",
            remoteShow = "feedback_show_bottom_ads",
            remoteSize = "feedback_ads_size",
            remoteType = "feedback_ads_type",
            remoteNative = "feedback_native_ads_id",
            remoteBanner = "feedback_banner_ads_id",
            prefShow = ADSMainClass.FEEDBACK_SHOW_BOTTOM_ADS,
            prefSize = ADSMainClass.FEEDBACK_ADS_SIZE,
            prefType = ADSMainClass.FEEDBACK_ADS_TYPE,
            prefNative = ADSMainClass.FEEDBACK_NATIVE,
            prefBanner = ADSMainClass.FEEDBACK_BANNER
        )
    )

    fun applyFromRemoteJson(json: String, gson: Gson) {
        if (json.isBlank()) {
            return
        }

        runCatching {
            val rootObj = JsonParser.parseString(json).asJsonObject
            val adsObj = when {
                rootObj.has("ads") && rootObj.get("ads").isJsonObject ->
                    rootObj.getAsJsonObject("ads")
                rootObj.has("splash_screen") || rootObj.has("language_screen") ->
                    rootObj
                else -> null
            }

            if (adsObj != null) {
                PrefManager.setAdsConfigJson(adsObj.toString())
//                Log.d(TAG, "Ads JSON stored in PrefManager (${adsObj.keySet().size} keys)")
            }

            val root = gson.fromJson(json, AdsConfigRoot::class.java)
            val ads = root.ads ?: gson.fromJson(adsObj, AdsSection::class.java)

            applyConsent(root, ads)
            applyAdsLoadingType(root, ads)
            applyGlobalAdsFlags(ads)
            applyNativeAdsReloadConfig(adsObj, ads)
            applySplash(ads?.splashScreen)
            applyInterAndReward(ads?.interAndReward)
            applyLanguage(ads?.languageScreen)
            applySetting(ads?.settingScreen)

            if (adsObj != null) {
                BOTTOM_SCREENS.forEach { keys ->
                    applyBottomScreen(adsObj, keys)
                }
                applyActionInterFlags(adsObj)
                // `unlock_ads_show` may sit inside `unlock_items` or directly under `ads`.
                val unlockObj = adsObj.getAsJsonObjectOrNull("unlock_items")?.deepCopy() ?: JsonObject()
                adsObj.get("unlock_ads_show")?.takeIf { !it.isJsonNull }
                    ?.let { unlockObj.add("unlock_ads_show", it) }
                PrefManager.setUnlockItemsJson(if (unlockObj.size() == 0) "" else unlockObj.toString())
                // Unlock ads use their own ad units (blank → falls back to the normal ones).
                ADSMainClass.setStringValue(
                    ADSMainClass.UNLOCK_REWARD_AD_ID,
                    unlockObj.get("reward_ad_id")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                )
                ADSMainClass.setStringValue(
                    ADSMainClass.UNLOCK_INTER_AD_ID,
                    unlockObj.get("inter_ad_id")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                )
                // Home / Setting also use typed ADSMainClass getters
                syncHomeTypedPrefs(adsObj)
                syncSettingTypedPrefs(adsObj)
                // Fallback OTHER_SCREEN ids from home (or language) for preload / legacy
                applyOtherScreenFallback(adsObj, ads)
            }

            // Log.d(TAG, "All ads config applied to SharedPreferences")
        }.onFailure {
            // Log.e(TAG, "Failed to apply ads config", it)
        }
    }

    private fun applyConsent(root: AdsConfigRoot, ads: AdsSection?) {
        val screen = root.consentFormShowScreen
            ?.takeIf { it.isNotBlank() }
            ?: ads?.consentFormShowScreen?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CONSENT_SCREEN
        ADSMainClass.setConsentScreenShow(screen)
        // Log.d(TAG, "consent_form_show_screen=$screen")
    }

    private fun applyAdsLoadingType(root: AdsConfigRoot, ads: AdsSection?) {
        val type = root.adsLoadingTypes
            ?.takeIf { it.isNotBlank() }
            ?: ads?.adsLoadingTypes?.takeIf { it.isNotBlank() }
            ?: "Load"
        ADSMainClass.setAdsTypeManage(type)
        // Log.d(TAG, "Ads_Loading_Types=$type")
    }

    private fun applyGlobalAdsFlags(ads: AdsSection?) {
        if (ads == null) return
        ads.exitDialogShow?.let { ADSMainClass.setExitAds(it) }
        ads.temporarySkipGenerateApi?.let { ADSMainClass.setTemporarySkipGenerateApi(it) }
        ads.adsBlocked?.let { ADSMainClass.setAdsBlocked(it) }
        //Log.d(
//            TAG,
//            "applyGlobalAdsFlags: temporary_skip_generate_api=${ads.temporarySkipGenerateApi} " +
//                "ads_blocked=${ADSMainClass.getAdsBlocked()}"
//        )
        ads.privacyPolicyUrl?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setPrivacyPolicy(it)
            PrefManager.setPrivacyPolicyUrl(it)
        }
        ads.termsUrl?.takeIf { it.isNotBlank() }?.let {
            PrefManager.setTermsUrl(it)
        }
        ads.adsButtonColor?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setNativeButtonColor(it.trim())
        }
        ads.adsButtonTextColor?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setNativeButtonTextColor(it.trim())
        }
        ads.adsLayoutBackgroundColor?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setAdsLayoutBackgroundColor(it.trim())
        }
        ads.adsLayoutTextColor?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setAdsLayoutTextColor(it.trim())
        }
        ads.adsBackgroundColor?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setAdsBackgroundColor(it.trim())
        }
        ads.adsAllTextColor?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setAdsAllTextColor(it.trim())
        }
        // Log.d(
            // TAG,
            // "Native CTA colors: bg=${ADSMainClass.getNativeButtonColor()}, " +
                // "text=${ADSMainClass.getNativeButtonTextColor()}"
        // )
    }

    /** Firebase {@code ads.native_ads_reloading} + {@code ads.native_ads_reload_seconds}. */
    private fun applyNativeAdsReloadConfig(adsObj: JsonObject?, ads: AdsSection?) {
        val enabled = ads?.nativeAdsReloading
            ?: adsObj?.getBooleanOrNull("native_ads_reloading")
            ?: false
        ADSMainClass.setNativeAdsReloading(enabled)

        val fromModel = ads?.nativeAdsReloadSeconds
        val fromJson = adsObj.getIntOrNull("native_ads_reload_seconds")
        val seconds = fromModel ?: fromJson ?: 0
        ADSMainClass.setNativeAdsReloadSeconds(if (seconds > 0) seconds else 0)
    }

    private fun applyInterAndReward(config: InterAndRewardConfig?) {
        if (config == null) {
            // Log.d(TAG, "No inter_and_reward in remote config")
            return
        }
        config.interAdsShow?.let { ADSMainClass.setInterAdsShow(it) }
        config.interAdsLoadType?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setInterAdsLoadType(ADSMainClass.normalizeInterAdsLoadType(it))
        }
        config.interAdsShowType?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setInterAdsShowType(ADSMainClass.normalizeInterAdsShowType(it))
        }
        config.interAdsTimeSeconds?.let { ADSMainClass.setInterAdsTimeSeconds(it) }
        config.interAdsClick?.let { ADSMainClass.setAdsClick(it) }
        config.introInterAdsClickCount?.let { ADSMainClass.setIntroInterAdsClick(it) }
        config.introBackSendToNext?.let { ADSMainClass.setIntroBackSendToNext(it) }
        config.introShowScreenType?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setIntroShowScreenType(it.trim())
        }
        config.interAdId?.let {
            ADSMainClass.setStringValue(ADSMainClass.INTER_SECOND_TIME, it)
            // If splash_inter_id empty, language / forced inter can use this id
            if (ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME).isBlank()) {
                ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, it)
            }
        }
        config.interBackAdsShow?.let { ADSMainClass.setInterAdsOnBackShow(it) }
        config.interBackAdsClick?.let { ADSMainClass.setAdsBackClick(it) }
        config.interBackAdId?.let {
            ADSMainClass.setStringValue(ADSMainClass.INTER_BACK_AD_ID, it)
        }
        config.rewardAdsShow?.let { ADSMainClass.setRewardAdsShow(it) }
        config.rewardAdId?.let {
            ADSMainClass.setStringValue(ADSMainClass.REWARD_AD_ID, it)
        }
        config.bottomNavigationClickAdsShow?.let {
            ADSMainClass.setBottomNavigationClickAdsShow(it)
        }
        config.bottomNavigationClick?.let {
            ADSMainClass.setBottomNavigationClick(it)
        }
        // Log.d(
            // TAG,
            // "Inter/Reward: show=${ADSMainClass.getInterAdsShow()}, " +
                // "loadType=${ADSMainClass.getInterAdsLoadType()}, " +
                // "click=${ADSMainClass.getAdsClick()}, " +
                // "introClick=${ADSMainClass.getIntroInterAdsClick()}, " +
                // "introBackToNext=${ADSMainClass.getIntroBackSendToNext()}, " +
                // "backShow=${ADSMainClass.getInterAdsOnBackShow()}, " +
                // "backClick=${ADSMainClass.getAdsBackClick()}, " +
                // "bottomNavShow=${ADSMainClass.getBottomNavigationClickAdsShow()}, " +
                // "bottomNavClick=${ADSMainClass.getBottomNavigationClick()}, " +
                // "rewardShow=${ADSMainClass.getRewardAdsShow()}"
        // )
    }

    private fun applySplash(splash: SplashAdsConfig?) {
        if (splash == null) {
            // Log.d(TAG, "No ads.splash_screen in remote config")
            return
        }

        splash.splashAppOpenAdsShow?.let { ADSMainClass.setSplashAppOpenShow(it) }
        splash.splashAppOpenAfterCount?.let { ADSMainClass.setSplashAppOpenAfterCount(it) }
        splash.splashAdsType?.takeIf { it.isNotBlank() }?.let { ADSMainClass.setSplashADType(it) }
        splash.appOpenShowInBackground?.let { ADSMainClass.setAppOpenBackgroundShow(it) }
        splash.appOpenDailyShowCount?.let { ADSMainClass.setAppOpenAdDailyLimit(it) }
        splash.appOpenId?.let { ADSMainClass.setStringValue(ADSMainClass.APP_OPEN_ID, it) }
        splash.splashInterId?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, it)
        }

        // Log.d(
            // TAG,
            // "Splash ads: show=${ADSMainClass.getSplashAppOpenShow()}, " +
                // "afterCount=${ADSMainClass.getSplashAppOpenAfterCount()}, " +
                // "type=${ADSMainClass.getSplashADType()}, " +
                // "appOpenId=${ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID)}"
        // )
    }

    private fun applyLanguage(language: LanguageAdsConfig?) {
        if (language == null) {
            // Log.d(TAG, "No ads.language_screen in remote config")
            return
        }

        language.languageScreenBottomAdShow?.let { ADSMainClass.setLanguageScreenBottomAdShow(it) }

        // type = native | banner only (never small/medium/big)
        val rawType = language.languageAdsType
        ADSMainClass.setLanguageAdsType(normalizeLanguageAdsType(rawType))

        language.languageAdsSize?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setLanguageAdsSize(ADSNativeDisplay.normalizeNativeSize(it))
        }
        language.languageInterAdsShow?.let { ADSMainClass.setLanguageInterAdsShow(it) }
        language.languageNativeId?.let {
            ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE, it)
        }
        language.languageBannerId?.let {
            ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER, it)
        }

        // If old prefs stored size into type ("small"), repair to native
        val fixedType = normalizeLanguageAdsType(ADSMainClass.getLanguageAdsType())
        if (!fixedType.equals(ADSMainClass.getLanguageAdsType(), ignoreCase = true)) {
            ADSMainClass.setLanguageAdsType(fixedType)
        }

        // Log.d(
            // TAG,
            // "Language ads: bottomShow=${ADSMainClass.getLanguageScreenBottomAdShow()}, " +
                // "type=${ADSMainClass.getLanguageAdsType()}, " +
                // "size=${ADSMainClass.getLanguageAdsSize()}, " +
                // "nativeId=${ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE)}"
        // )
    }

    /** Only `native` or `banner`. Size values / typos â†’ native. */
    private fun normalizeLanguageAdsType(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            "banner" -> "banner"
            "native" -> "native"
            else -> "native"
        }
    }

    private fun applySetting(setting: SettingScreenAdsConfig?) {
        if (setting == null) return

        setting.showBottomAds?.let { ADSMainClass.setSettingBottomAdsShow(it) }
        setting.adsType?.takeIf { it.isNotBlank() }?.let { ADSMainClass.setSettingAdsType(it) }
        setting.adsSize?.let {
            ADSMainClass.setStringValue(
                ADSMainClass.SETTING_SCREEN_ADS_SIZE,
                ADSNativeDisplay.normalizeNativeSize(it)
            )
        }
        setting.nativeAdsId?.let {
            ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_NATIVE, it)
        }
        setting.bannerAdsId?.let {
            ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_BANNER, it)
        }
        setting.centerAdsShow?.let {
            ADSMainClass.setBooleanValue(ADSMainClass.SETTING_CENTER_ADS_SHOW, it)
        }
        setting.centerAdsSize?.let {
            ADSMainClass.setStringValue(
                ADSMainClass.SETTING_CENTER_ADS_SIZE,
                ADSNativeDisplay.normalizeNativeSize(it)
            )
        }
        setting.centerAdsType?.let {
            ADSMainClass.setStringValue(ADSMainClass.SETTING_CENTER_ADS_TYPE, it)
        }
        setting.centerNativeAdsId?.let {
            ADSMainClass.setStringValue(ADSMainClass.SETTING_CENTER_NATIVE, it)
        }
        setting.centerBannerAdsId?.let {
            ADSMainClass.setStringValue(ADSMainClass.SETTING_CENTER_BANNER, it)
        }
    }

    private fun applyBottomScreen(adsObj: JsonObject, keys: ScreenAdsPrefKeys) {
        val screen = adsObj.getAsJsonObjectOrNull(keys.remoteObjectKey)
        if (screen != null) {
            writeBottomScreenPrefs(screen, keys)
            return
        }

        // intro_screen1b/1c/1d missing â†’ copy intro_screen1 until Firebase adds them
        val fallbackFromIntro1 = keys.remoteObjectKey in setOf(
            "intro_screen1b",
            "intro_screen1c",
            "intro_screen1d"
        )
        if (!fallbackFromIntro1) return

        val intro1 = adsObj.getAsJsonObjectOrNull("intro_screen1") ?: return
        intro1.getBooleanOrNull("intro1_show_bottom_ads")?.let {
            ADSMainClass.setBooleanValue(keys.prefShow, it)
        }
        intro1.getStringOrNull("intro1_ads_size")?.let {
            ADSMainClass.setStringValue(keys.prefSize, ADSNativeDisplay.normalizeNativeSize(it))
        }
        intro1.getStringOrNull("intro1_ads_type")?.let {
            ADSMainClass.setStringValue(keys.prefType, it)
        }
        intro1.getStringOrNull("intro1_native_ads_id")?.let {
            ADSMainClass.setStringValue(keys.prefNative, it)
        }
        intro1.getStringOrNull("intro1_banner_ads_id")?.let {
            ADSMainClass.setStringValue(keys.prefBanner, it)
        }
        // Log.d(TAG, "${keys.remoteObjectKey}: missing â€” fell back to intro_screen1")
    }

    private fun writeBottomScreenPrefs(screen: JsonObject, keys: ScreenAdsPrefKeys) {
        screen.getBooleanOrNull(keys.remoteShow)?.let {
            ADSMainClass.setBooleanValue(keys.prefShow, it)
        }
        screen.getStringOrNull(keys.remoteSize)?.let {
            ADSMainClass.setStringValue(keys.prefSize, ADSNativeDisplay.normalizeNativeSize(it))
        }
        screen.getStringOrNull(keys.remoteType)?.let {
            ADSMainClass.setStringValue(keys.prefType, it)
        }
        screen.getStringOrNull(keys.remoteNative)?.let {
            ADSMainClass.setStringValue(keys.prefNative, it)
        }
        screen.getStringOrNull(keys.remoteBanner)?.let {
            ADSMainClass.setStringValue(keys.prefBanner, it)
        }

        // Log.d(
            // TAG,
            // "${keys.remoteObjectKey}: show=${ADSMainClass.getBooleanValue(keys.prefShow)}, " +
                // "type=${ADSMainClass.getStringValue(keys.prefType)}, " +
                // "native=${ADSMainClass.getStringValue(keys.prefNative)}"
        // )
    }

    /** Per-action `*_show_inter` flags nested under each screen object. */
    private fun applyActionInterFlags(adsObj: JsonObject) {
        fun JsonObject.applyBool(remoteKey: String, prefKey: String) {
            getBooleanOrNull(remoteKey)?.let { ADSMainClass.setBooleanValue(prefKey, it) }
        }

        adsObj.getAsJsonObjectOrNull("intro_screen1")?.applyBool(
            "intro1_show_inter", ADSMainClass.INTRO1_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("intro_screen1b")?.applyBool(
            "intro1b_show_inter", ADSMainClass.INTRO1B_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("intro_screen1c")?.applyBool(
            "intro1c_show_inter", ADSMainClass.INTRO1C_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("intro_screen1d")?.applyBool(
            "intro1d_show_inter", ADSMainClass.INTRO1D_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("intro_screen2")?.applyBool(
            "intro2_show_inter", ADSMainClass.INTRO2_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("intro_screen3")?.applyBool(
            "intro3_show_inter", ADSMainClass.INTRO3_SHOW_INTER
        )

        adsObj.getAsJsonObjectOrNull("home_screen")?.let { home ->
            home.applyBool("tryit_show_inter", ADSMainClass.TRYIT_SHOW_INTER)
            home.applyBool("seeall_show_inter", ADSMainClass.SEEALL_SHOW_INTER)
            home.applyBool("uplode_photo_button_show_inter", ADSMainClass.UPLOAD_PHOTO_BUTTON_SHOW_INTER)
            home.applyBool("setting_icon_show_inter", ADSMainClass.SETTING_ICON_SHOW_INTER)
        }

        adsObj.getAsJsonObjectOrNull("select_image_screen")?.applyBool(
            "select_image_next_button_show_inter",
            ADSMainClass.SELECT_IMAGE_NEXT_BUTTON_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("progress_screen")?.applyBool(
            "close_icon_show_inter", ADSMainClass.CLOSE_ICON_SHOW_INTER
        )
        adsObj.getAsJsonObjectOrNull("result_screen")?.let { result ->
            result.applyBool("leave_button_show_inter", ADSMainClass.LEAVE_BUTTON_SHOW_INTER)
            result.applyBool("save_photo_show_ads", ADSMainClass.SAVE_PHOTO_SHOW_ADS)
        }
        adsObj.getAsJsonObjectOrNull("setting_screen")?.applyBool(
            "setting_show_inter", ADSMainClass.SETTING_SHOW_INTER
        )
    }

    private fun syncHomeTypedPrefs(adsObj: JsonObject) {
        val home = adsObj.getAsJsonObjectOrNull("home_screen") ?: return
        home.getBooleanOrNull("home_show_bottom_ads")?.let {
            ADSMainClass.setHomeScreenBottomShow(it)
        }
        home.getStringOrNull("home_ads_type")?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setHomeScreenAdsType(it)
        }
    }

    private fun syncSettingTypedPrefs(adsObj: JsonObject) {
        val setting = adsObj.getAsJsonObjectOrNull("setting_screen") ?: return
        setting.getBooleanOrNull("setting_show_bottom_ads")?.let {
            ADSMainClass.setSettingBottomAdsShow(it)
        }
        setting.getStringOrNull("setting_ads_type")?.takeIf { it.isNotBlank() }?.let {
            ADSMainClass.setSettingAdsType(it)
        }
    }

    private fun applyOtherScreenFallback(adsObj: JsonObject, ads: AdsSection?) {
        val homeNative = adsObj.getAsJsonObjectOrNull("home_screen")
            ?.getStringOrNull("home_native_ads_id")
        val homeBanner = adsObj.getAsJsonObjectOrNull("home_screen")
            ?.getStringOrNull("home_banner_ads_id")
        val languageNative = ads?.languageScreen?.languageNativeId
        val languageBanner = ads?.languageScreen?.languageBannerId

        val otherNative = homeNative?.takeIf { it.isNotBlank() }
            ?: languageNative?.takeIf { it.isNotBlank() }
        val otherBanner = homeBanner?.takeIf { it.isNotBlank() }
            ?: languageBanner?.takeIf { it.isNotBlank() }

        otherNative?.let { ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_NATIVE, it) }
        otherBanner?.let { ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_BANNER, it) }

        // Enable OtherAdsShow if any flow screen wants bottom ads
        val anyOtherShow = listOf(
            ADSMainClass.INTRO1_SHOW_BOTTOM_ADS,
            ADSMainClass.INTRO1B_SHOW_BOTTOM_ADS,
            ADSMainClass.INTRO1C_SHOW_BOTTOM_ADS,
            ADSMainClass.INTRO1D_SHOW_BOTTOM_ADS,
            ADSMainClass.INTRO2_SHOW_BOTTOM_ADS,
            ADSMainClass.INTRO3_SHOW_BOTTOM_ADS,
            ADSMainClass.SELECT_IMAGE_SHOW_BOTTOM_ADS,
            ADSMainClass.ADD_PHOTO_SHOW_BOTTOM_ADS,
            ADSMainClass.GALLERY_PHOTOS_SHOW_BOTTOM_ADS,
            ADSMainClass.PROGRESS_SHOW_BOTTOM_ADS,
            ADSMainClass.RESULT_SHOW_BOTTOM_ADS,
            ADSMainClass.USER_CREATION_SHOW_BOTTOM_ADS,
            ADSMainClass.FEEDBACK_SHOW_BOTTOM_ADS
        ).any { ADSMainClass.getBooleanValue(it) }
        ADSMainClass.setOtherAdsShow(anyOtherShow)
        if (otherNative != null) {
            ADSMainClass.setOtherAdsType("native")
        }
    }

    private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? {
        if (!has(key) || get(key).isJsonNull || !get(key).isJsonObject) return null
        return getAsJsonObject(key)
    }

    private fun JsonObject.getBooleanOrNull(key: String): Boolean? {
        if (!has(key) || get(key).isJsonNull) return null
        val el = get(key)
        return when {
            el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asBoolean
            el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asInt != 0
            el.isJsonPrimitive && el.asJsonPrimitive.isString ->
                el.asString.trim().equals("true", ignoreCase = true)
            else -> null
        }
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        if (!has(key) || get(key).isJsonNull) return null
        val el = get(key)
        return if (el.isJsonPrimitive) el.asString else null
    }

    private fun JsonObject?.getIntOrNull(key: String): Int? {
        if (this == null || !has(key) || get(key).isJsonNull) return null
        val el = get(key)
        if (!el.isJsonPrimitive) return null
        val primitive = el.asJsonPrimitive
        return when {
            primitive.isNumber -> runCatching { primitive.asInt }.getOrNull()
            primitive.isString -> primitive.asString.trim().toIntOrNull()
            else -> null
        }
    }
}
