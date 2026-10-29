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
import com.hairstyle.changer.aitryon.utils.AESDebugHelper
import com.hairstyle.changer.aitryon.utils.AppVisitManager
import com.hairstyle.changer.aitryon.utils.IntroManager
import com.hairstyle.changer.aitryon.utils.InstallReferrerHelper
import com.hairstyle.changer.aitryon.utils.LocaleManager
import com.hairstyle.changer.aitryon.utils.NavGradientHelper.applyBrandTextGradient
import com.hairstyle.changer.aitryon.utils.PermissionHelper
import com.hairstyle.changer.aitryon.utils.SplashAnimationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
            billingManager.refreshEntitlements()
            runCatching {
                InstallReferrerHelper.awaitFetch(this@SplashActivity)
                remoteConfigManager.fetchAndSaveAll()
                AESDebugHelper.logRemoteConfigUrlsFromPrefs()
                videosRepository.fetchAndSaveAllVideos()
                if (!IntroManager.isIntroCompleted()) {
                    videosRepository.preloadIntroVideos()
                }
                // Await billing sync to ensure correct premium/ad state before consent/ad decisions
                withTimeoutOrNull(2500L) {
                    billingManager.isInitialSyncComplete.first { it }
                }
            }.onFailure {
                // Log.e(TAG, "Splash fetch/cache failed", it)
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
        if (ADSConsentManager.canRequestAds(activity) && !ADSMainClass.getAds_Free()) {
            ADSAppManage.initializeMobileAdsIfNeeded(activity.applicationContext) {
                activity.runOnUiThread {
                    if (isScreenAlive() && !navigated && !activity.isFinishing && !activity.isDestroyed) {
                        preloadNativeAdsIfNeeded(activity)
                        preloadInterstitialAdsIfNeeded(activity)
                        action()
                    }
                }
            }
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

        if (!ADSMainClass.shouldShowSplashAd()) {
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
    }
}
