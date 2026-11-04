package com.hairstyle.changer.aitryon.ui.splash

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.hairstyle.changer.aitryon.databinding.FragmentSplashBinding
import com.hairstyle.changer.aitryon.Ads.ADSAppManage
import com.hairstyle.changer.aitryon.Ads.ADSAppStarting
import com.hairstyle.changer.aitryon.Ads.ADSConsentManager
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.ADSNativeDisplay
import com.hairstyle.changer.aitryon.Ads.ADSUtilitis
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.remoteconfig.RemoteConfigManager
import com.hairstyle.changer.aitryon.repository.VideosRepository
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.utils.AppVisitManager
import com.hairstyle.changer.aitryon.utils.IntroManager
import com.hairstyle.changer.aitryon.utils.InstallReferrerHelper
import com.hairstyle.changer.aitryon.utils.LocaleManager
import com.hairstyle.changer.aitryon.utils.NavGradientHelper.applyBrandTextGradient
import com.hairstyle.changer.aitryon.utils.PermissionHelper
import com.hairstyle.changer.aitryon.utils.SplashAnimationHelper
import dagger.hilt.android.AndroidEntryPoint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.hairstyle.changer.aitryon.utils.PrefManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    override val isSplashScreen = true
    override val useLightStatusBarIcons = false

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var videosRepository: VideosRepository

    @Inject
    lateinit var categoryImagesRepository: com.hairstyle.changer.aitryon.repository.CategoryImagesRepository

    private val startMs = SystemClock.elapsedRealtime()
    private var catalogJob: Deferred<Unit>? = null

    /** Splash timeline in Logcat (tag SplashTime) — shows which step is slow. */
    private fun mark(stage: String) {
        android.util.Log.d("SplashTime", "+${SystemClock.elapsedRealtime() - startMs}ms $stage")
    }

    private var animationReady = false
    private var navigated = false
    private var splashMediaLocked = false
    private var notificationPromptStarted = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            PermissionHelper.clearDeniedFlag(PermissionHelper.PermissionType.NOTIFICATION)
        } else {
            PermissionHelper.markDenied(PermissionHelper.PermissionType.NOTIFICATION)
        }
        // Allow or deny — both continue to splash ads, then next screen.
        continueAfterConsent { showSplashAdOrContinue() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        ADSMainClass.initAppContext(applicationContext)
        ADSAppManage.FastStart = true
        ADSAppStarting.resetSplashAd()
        ADSAppStarting.ADSIsShowing = false
        ADSAppManage.AppStartingScreenOpen = false

        binding.hairCutTryOn.applyBrandTextGradient(useOriginalColors = true)
        // Local splash first so UI is never blank while we fetch+cache
        playLocalWebp()
        fetchRemoteConfigCacheVideosThenAds()
    }

    private fun localSplashRawId(): Int {
        return resources.getIdentifier("splash_video", "raw", packageName)
    }

    private fun playLocalWebp() {
        if (splashMediaLocked) return
        val rawId = localSplashRawId()
        if (rawId == 0) return

        splashMediaLocked = true
        SplashAnimationHelper.playWebp(
            imageView = binding.ivSplashAnimation,
            data = rawId,
            onReady = { animationReady = true },
            onError = { binding.ivSplashAnimation.visibility = View.GONE }
        )
    }

    /**
     * 1) Wait until internet is available (no-internet dialog from BaseActivity)
     * 2) Remote Config + videos API (URLs only)
     * 3) Start background cache of the 3 intro videos — do not wait
     * 4) Notification permission → consent → splash ads → next screen
     */
    private fun fetchRemoteConfigCacheVideosThenAds() {
        lifecycleScope.launch {
            // Block splash flow until online — dialog is shown by BaseActivity
            networkMonitor.awaitOnline()
            if (!isScreenAlive() || navigated) return@launch

            val startedAt = System.currentTimeMillis()
            mark("online")
            startAdsInitEarly()
            runCatching {
                // Capped: a slow referrer / Remote Config must not hold Splash. Returning users
                // already have the last config saved, so they can wait less than first-time users.
                val budget = if (PrefManager.getRemoteConfigFetched()) CONFIG_WAIT_MS else FIRST_CONFIG_WAIT_MS
                val done = withTimeoutOrNull(budget) {
                    InstallReferrerHelper.awaitFetch(this@SplashActivity)
                    mark("referrer")
                    remoteConfigManager.fetchAndSaveAll()
                    true
                }
                mark(if (done == true) "remote-config" else "remote-config TIMEOUT")
            }.onFailure {
                android.util.Log.e("SplashFetch", "Splash fetch/cache failed", it)
            }
            startSplashAdEarly()
            rcDone = true
            tryPrefetchLanguageNative()
            // Home lists + first pictures load now, while consent / the splash ad are in progress.
            runCatching { categoryImagesRepository.prefetchHome() }
            // Videos catalog needs the base url from Remote Config, but it only stores URLs, so
            // it runs alongside consent / ads instead of in front of them. Navigation joins it.
            catalogJob = lifecycleScope.async(Dispatchers.IO) {
                runCatching {
                    videosRepository.fetchAndSaveAllVideos()
                    // Only the 3 intro videos, and only until the intro is completed.
                    if (!IntroManager.isIntroCompleted()) {
                        videosRepository.preloadIntroVideos()
                    } else {
                        // Returning user: Home is next, so warm its animations + hero pictures now.
                        videosRepository.prefetchHomeMedia()
                    }
                }.onFailure {
                    android.util.Log.e("SplashFetch", "Videos catalog failed", it)
                }
                mark("videos-catalog")
            }
            if (!isScreenAlive() || navigated) return@launch

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_SPLASH_MS) {
                delay(MIN_SPLASH_MS - elapsed)
            }
            if (!isScreenAlive() || navigated) return@launch

            requestNotificationThenShowAds()
        }
    }

    /** `shouldShowSplashAd()` counts a visit each time it is asked, so ask once per launch. */
    private var splashAdAllowedCache: Boolean? = null

    private fun splashAdAllowed(): Boolean =
        splashAdAllowedCache ?: ADSMainClass.shouldShowSplashAd().also { splashAdAllowedCache = it }

    private var adsInitDone = false
    private var adConfigReady = false
    private var rcDone = false

    /**
     * Starts as soon as we are online - in parallel with the Firebase fetch, because neither the
     * consent refresh nor the SDK init needs the config. (Mediation adapters such as Vungle make
     * the init slow, so it must not wait behind Remote Config.) Consent state is refreshed first
     * with no form; only if ads are allowed does the init start. If a consent form is needed the
     * normal flow (form -> init -> request) takes over.
     */
    private fun startAdsInitEarly() {
        if (ADSMainClass.getAds_Free()) return
        if (!ADSUtilitis.IsNetworkConnected(this)) return
        ADSConsentManager.gatherConsent(this, false) { _ ->
            if (!isScreenAlive() || navigated || !ADSConsentManager.canRequestAds(this)) {
                mark("early ads-init skipped (consent needed)")
                return@gatherConsent
            }
            mark("ads-init started early")
            ADSAppManage.initializeMobileAdsIfNeeded(applicationContext) {
                runOnUiThread {
                    mark("ads-init complete (early)")
                    adsInitDone = true
                    tryPrefetchLanguageNative()
                    tryPrefetchSplashAd()
                }
            }
        }
    }

    /** Once the Firebase config is in: is a splash ad wanted at all? */
    private fun startSplashAdEarly() {
        if (!isScreenAlive() || navigated) return
        if (ADSMainClass.getAds_Free()) return
        if (!ADSUtilitis.IsNetworkConnected(this)) return
        if (!splashAdAllowed()) return
        val type = ADSMainClass.getSplashADType().orEmpty()
        if (!type.equals("AppOpen", true) && !type.equals("Inter", true) &&
            !type.equals("Interstitial", true)
        ) return
        adConfigReady = true
        tryPrefetchSplashAd()
    }

    private fun tryPrefetchLanguageNative() {
        if (!adsInitDone || !rcDone) return
        prefetchLanguageNativeNow(this)
    }

    /**
     * Next screen is Language (first launch): request its bottom native now, in parallel with the
     * splash ad, so it is ready when Language opens. Needs consent (canRequestAds).
     */
    private fun prefetchLanguageNativeNow(activity: Activity) {
        if (!isScreenAlive() || navigated || isFinishing || isDestroyed) return
        if (LocaleManager.isLanguageOnboardingDone()) return
        if (!ADSMainClass.getLanguageScreenBottomAdShow() || ADSMainClass.getAds_Free()) return
        if (ADSMainClass.getLanguageAdsType().trim().equals("banner", ignoreCase = true)) return
        if (!ADSConsentManager.canRequestAds(activity)) return
        // After the splash ad has loaded / failed: no bandwidth shared with it, Splash not slowed.
        val appContext = activity.applicationContext
        ADSAppStarting.runWhenSplashAdSettled(adConfigReady) {
            ADSNativeDisplay.prefetchLanguageNative(
                appContext,
                ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE)
            )
        }
    }

    /** The single splash ad request: only when consent + SDK init are done AND the config says yes. */
    private fun tryPrefetchSplashAd() {
        if (!adsInitDone || !adConfigReady) return
        if (!isScreenAlive() || navigated || isFinishing || isDestroyed) return
        ADSAppStarting.prefetchSplashAd(this)
    }

    private fun continueAfterConsent(action: () -> Unit) {
        if (isFinishing || isDestroyed) {
            action()
            return
        }

        // No internet → skip consent form, continue next
        if (!ADSUtilitis.IsNetworkConnected(this)) {
//            Log.d(TAG, "No internet — skip consent, continue")
            action()
            return
        }

        val showFormOnSplash = ADSMainClass.isConsentOnSplash()
        ADSConsentManager.gatherConsent(this, showFormOnSplash) { formShown ->
            if (!isScreenAlive() || navigated) return@gatherConsent
            if (isFinishing || isDestroyed) return@gatherConsent

            if (showFormOnSplash) {
                if (formShown) {
                    ADSMainClass.setConsentPendingOnLanguage(false)
                } else if (ADSConsentManager.shouldRetryConsentOnLanguage(this)) {
                    ADSMainClass.setConsentPendingOnLanguage(true)
                }
            }

            proceedAfterConsentGathered(this, action)
        }
    }

    private fun proceedAfterConsentGathered(activity: Activity, action: () -> Unit) {
        mark("consent")
        if (ADSConsentManager.canRequestAds(activity) && !ADSMainClass.getAds_Free()) {
            // MobileAds.initialize waits for every mediation adapter; cap it so a slow adapter
            // cannot hold Splash. `proceed` runs once, from whichever comes first.
            var proceeded = false
            val proceed = Runnable {
                if (proceeded) return@Runnable
                proceeded = true
                activity.runOnUiThread {
                    if (isScreenAlive() && !navigated && !activity.isFinishing && !activity.isDestroyed) {
                        mark("ads-init done")
                        preloadNativeAdsIfNeeded(activity)
                        prefetchLanguageNativeNow(activity)
                        preloadInterstitialAdsIfNeeded(activity)
                        action()
                    }
                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                if (!proceeded) mark("ads-init TIMEOUT")
                proceed.run()
            }, ADS_INIT_TIMEOUT_MS)
            ADSAppManage.initializeMobileAdsIfNeeded(activity.applicationContext) { proceed.run() }
            // Splash ad already loaded / failed → nothing left to wait for.
            if (ADSAppStarting.isSplashAdSettled()) proceed.run()
        } else {
            activity.runOnUiThread {
                if (isScreenAlive() && !navigated) {
                    action()
                }
            }
        }
    }

    /**
     * Ads_Loading_Types:
     * - Load → native loads on each screen open (onLoad) — no splash preload
     * - PreLoad → preload language/native ad on Splash for faster next screens
     */
    private fun preloadNativeAdsIfNeeded(activity: Activity) {
        val loadingType = ADSMainClass.getAdsTypeManage().orEmpty()
        // Log.d(TAG, "Ads_Loading_Types=$loadingType")

        if (!loadingType.equals("PreLoad", ignoreCase = true)) {
            return
        }
        if (ADSMainClass.getAds_Free()) return
        if (!ADSConsentManager.canRequestAds(activity)) return

        val nativeId = ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE)
            .takeIf { it.isNotBlank() }
            ?: ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE)

        if (nativeId.isNullOrBlank()) {
            // Log.d(TAG, "PreLoad skipped: no native ad id")
            return
        }

        // Log.d(TAG, "PreLoading native on Splash id=$nativeId")
        ADSNativeDisplay.preloadNativeAd(activity.applicationContext, nativeId)
    }

    /**
     * inter_ads_load_type:
     * - PreLoad → cache forward interstitial on Splash
     * - Show / Load → request when showing (no splash preload)
     */
    private fun preloadInterstitialAdsIfNeeded(activity: Activity) {
        val loadType = ADSMainClass.getInterAdsLoadType().orEmpty()
        // Log.d(TAG, "inter_ads_load_type=$loadType")

        if (ADSMainClass.isInterShowLoadMode()) return
        if (ADSMainClass.getAds_Free()) return
        if (!ADSConsentManager.canRequestAds(activity)) return

        val adId = InterstitialNavHelper.forwardAdId()
        if (adId.isBlank()) {
            // Log.d(TAG, "Inter PreLoad skipped: no inter ad id")
            return
        }

        // Log.d(TAG, "PreLoading interstitial on Splash id=$adId")
        InterstitialNavHelper.preload(activity)
    }

    private fun showSplashAdOrContinue() {
        if (isFinishing || isDestroyed) {
            navigateFromSplash()
            return
        }

        if (!ADSUtilitis.IsNetworkConnected(this)) {
            navigateFromSplash()
            return
        }

        if (!ADSConsentManager.canRequestAds(this)) {
            navigateFromSplash()
            return
        }

        if (!splashAdAllowed()) {
            // Log.d(TAG, "Splash ad skipped by after_count / show flag")
            navigateFromSplash()
            return
        }

        if (ADSMainClass.getAds_Free()) {
            navigateFromSplash()
            return
        }

        val splashType = ADSMainClass.getSplashADType()
        // Log.d(TAG, "splash_ads_type=$splashType")

        val shouldShowSplashAd = splashType.equals("AppOpen", ignoreCase = true)
            || splashType.equals("Inter", ignoreCase = true)
            || splashType.equals("Interstitial", ignoreCase = true)

        if (shouldShowSplashAd) {
            ADSAppStarting.AdsSplashAppStartingDisplay(
                this,
                ADSAppStarting.OnFinishAds {
                    ADSAppManage.FastStart = true
                    navigateFromSplash()
                }
            )
        } else {
            ADSAppManage.FastStart = true
            navigateFromSplash()
        }
    }

    /**
     * Ask POST_NOTIFICATIONS on Android 13+ before splash ads.
     * Allow or deny — both continue to consent → ads → next screen.
     */
    private fun requestNotificationThenShowAds() {
        if (!isScreenAlive() || navigated) return
        if (notificationPromptStarted) {
            continueAfterConsent { showSplashAdOrContinue() }
            return
        }
        notificationPromptStarted = true

        if (!PermissionHelper.needsNotificationPermission() ||
            PermissionHelper.hasNotificationPermission(this)
        ) {
            continueAfterConsent { showSplashAdOrContinue() }
            return
        }

        val permission = PermissionHelper.notificationPermissions().firstOrNull()
        if (permission == null) {
            continueAfterConsent { showSplashAdOrContinue() }
            return
        }

        notificationPermissionLauncher.launch(permission)
    }

    private fun navigateFromSplash() {
        if (!isScreenAlive() || navigated) return
        navigated = true
        mark("navigate (ad finished / skipped)")

        // Join the videos catalog (started on Splash) for at most a moment, then go.
        lifecycleScope.launch {
            withTimeoutOrNull(CATALOG_WAIT_MS) { catalogJob?.await() }
            if (isFinishing || isDestroyed) return@launch
            openNextScreen()
        }
    }

    private fun openNextScreen() {
        AppVisitManager.incrementLaunchCount()
        ADSAppManage.AppStartingScreenOpen = true

        when {
            !LocaleManager.isLanguageOnboardingDone() ->
                AppNavigator.openLanguage(this, fromSettings = false, finishFrom = true)
            !IntroManager.isIntroCompleted() ->
                AppNavigator.openIntro1(this, finishFrom = true)
            else -> AppNavigator.openHomeClearTask(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (animationReady && _binding != null) {
            SplashAnimationHelper.resume(binding.ivSplashAnimation)
        }
    }

    override fun onPause() {
        if (_binding != null) {
            SplashAnimationHelper.pause(binding.ivSplashAnimation)
        }
        super.onPause()
    }

    override fun onDestroy() {
        _binding?.let { SplashAnimationHelper.release(it.ivSplashAnimation) }
        animationReady = false
        navigated = false
        notificationPromptStarted = false
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val TAG = "SplashAds"
        private const val MIN_SPLASH_MS = 2000L
        private const val CONFIG_WAIT_MS = 5000L
        private const val FIRST_CONFIG_WAIT_MS = 10000L
        private const val ADS_INIT_TIMEOUT_MS = 2000L
        private const val CATALOG_WAIT_MS = 3000L
    }
}
