package com.hairstyle.generator.aimakerover.ui.splash

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentSplashBinding
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppManage
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppStarting
import com.hairstyle.generator.aimakerover.Advertisement.ADSConsentManager
import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.Advertisement.ADSNativeDisplay
import com.hairstyle.generator.aimakerover.Advertisement.ADSUtilitis
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.data.repository.VideosRepository
import com.hairstyle.generator.aimakerover.utils.AppVisitManager
import com.hairstyle.generator.aimakerover.utils.IntroManager
import com.hairstyle.generator.aimakerover.utils.LocaleManager
import com.hairstyle.generator.aimakerover.utils.NavGradientHelper.applyBrandTextGradient
import com.hairstyle.generator.aimakerover.utils.NetworkMonitor
import com.hairstyle.generator.aimakerover.utils.PermissionHelper
import com.hairstyle.generator.aimakerover.utils.SplashAnimationHelper
import com.hairstyle.generator.aimakerover.utils.VideoTitleKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var videosRepository: VideosRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

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
        // Allow or deny â€” both continue to next screen after Splash.
        navigateFromSplash()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ADSMainClass.initAppContext(requireContext().applicationContext)
        ADSAppManage.FastStart = true
        ADSAppStarting.ADSIsShowing = false
        ADSAppManage.AppStartingScreenOpen = false

        binding.hairCutTryOn.applyBrandTextGradient()
        // Local splash first so UI is never blank while we fetch+cache
        playLocalWebp()
        fetchRemoteConfigCacheVideosThenAds()
    }

    private fun localSplashRawId(): Int {
        return resources.getIdentifier("splash_video", "raw", requireContext().packageName)
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
     * 1) Wait until internet is available (no-internet dialog from MainActivity)
     * 2) Remote Config + videos API
     * 3) Wait until intro/splash videos are cached (or timeout)
     * 4) Then consent â†’ ads â†’ notification permission â†’ next screen
     */
    private fun fetchRemoteConfigCacheVideosThenAds() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Block splash flow until online â€” dialog is shown by MainActivity
            networkMonitor.awaitOnline()
            if (!isAdded || navigated) return@launch

            val startedAt = System.currentTimeMillis()
            runCatching {
                remoteConfigManager.fetchAndSaveAll()
                videosRepository.fetchAndSaveAllVideos()
                // Block next until videos stored in cache (slow net: max 15s)
                videosRepository.awaitCacheVideos(
                    videoTitleKeys = listOf(
                        VideoTitleKeys.SPLASH,
                        VideoTitleKeys.INTRO_VIDEO_1,
                        VideoTitleKeys.INTRO_VIDEO_2,
                        VideoTitleKeys.INTRO_VIDEO_3
                    ),
                    timeoutMs = VIDEO_CACHE_WAIT_MS
                )
            }.onFailure {
                // Log.e(TAG, "Splash fetch/cache failed", it)
            }
            if (!isAdded || navigated) return@launch

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_SPLASH_MS) {
                delay(MIN_SPLASH_MS - elapsed)
            }
            if (!isAdded || navigated) return@launch

            continueAfterConsent { showSplashAdOrContinue() }
        }
    }

    private fun continueAfterConsent(action: () -> Unit) {
        val activity = activity ?: run {
            action()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            action()
            return
        }

        // No internet â†’ skip consent form, continue next
        if (!ADSUtilitis.IsNetworkConnected(activity)) {
//            Log.d(TAG, "No internet â€” skip consent, continue")
            action()
            return
        }

        val showFormOnSplash = ADSMainClass.isConsentOnSplash()
        ADSConsentManager.gatherConsent(activity, showFormOnSplash) { formShown ->
            if (!isAdded || navigated) return@gatherConsent
            if (activity.isFinishing || activity.isDestroyed) return@gatherConsent

            if (showFormOnSplash) {
                if (formShown) {
                    ADSMainClass.setConsentPendingOnLanguage(false)
                } else if (ADSConsentManager.shouldRetryConsentOnLanguage(activity)) {
                    ADSMainClass.setConsentPendingOnLanguage(true)
                }
            }

            proceedAfterConsentGathered(activity, action)
        }
    }

    private fun proceedAfterConsentGathered(activity: Activity, action: () -> Unit) {
        if (ADSConsentManager.canRequestAds(activity) && !ADSMainClass.getAds_Free()) {
            ADSAppManage.initializeMobileAdsIfNeeded(activity.applicationContext) {
                activity.runOnUiThread {
                    if (isAdded && !navigated && !activity.isFinishing && !activity.isDestroyed) {
                        preloadNativeAdsIfNeeded(activity)
                        preloadInterstitialAdsIfNeeded(activity)
                        action()
                    }
                }
            }
        } else {
            activity.runOnUiThread {
                if (isAdded && !navigated) {
                    action()
                }
            }
        }
    }

    /**
     * Ads_Loading_Types:
     * - Load â†’ native loads on each screen open (onLoad) â€” no splash preload
     * - PreLoad â†’ preload language/native ad on Splash for faster next screens
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
     * - PreLoad â†’ cache forward interstitial on Splash
     * - Show / Load â†’ request when showing (no splash preload)
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
        val activity = activity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            requestNotificationThenNavigate()
            return
        }

        if (!ADSUtilitis.IsNetworkConnected(activity)) {
            requestNotificationThenNavigate()
            return
        }

        if (!ADSConsentManager.canRequestAds(activity)) {
            requestNotificationThenNavigate()
            return
        }

        if (!ADSMainClass.shouldShowSplashAd()) {
            // Log.d(TAG, "Splash ad skipped by after_count / show flag")
            requestNotificationThenNavigate()
            return
        }

        if (ADSMainClass.getAds_Free()) {
            requestNotificationThenNavigate()
            return
        }

        val splashType = ADSMainClass.getSplashADType()
        // Log.d(TAG, "splash_ads_type=$splashType")

        val shouldShowSplashAd = splashType.equals("AppOpen", ignoreCase = true)
            || splashType.equals("Inter", ignoreCase = true)
            || splashType.equals("Interstitial", ignoreCase = true)

        if (shouldShowSplashAd) {
            ADSAppStarting.AdsSplashAppStartingDisplay(
                activity,
                ADSAppStarting.OnFinishAds {
                    ADSAppManage.FastStart = true
                    requestNotificationThenNavigate()
                }
            )
        } else {
            ADSAppManage.FastStart = true
            requestNotificationThenNavigate()
        }
    }

    /**
     * After Splash (ads/consent done): ask POST_NOTIFICATIONS on Android 13+,
     * then go Language / Intro / Home. Deny still continues.
     */
    private fun requestNotificationThenNavigate() {
        if (!isAdded || navigated) return
        if (notificationPromptStarted) return
        notificationPromptStarted = true

        if (!PermissionHelper.needsNotificationPermission() ||
            PermissionHelper.hasNotificationPermission(requireContext())
        ) {
            navigateFromSplash()
            return
        }

        val permission = PermissionHelper.notificationPermissions().firstOrNull()
        if (permission == null) {
            navigateFromSplash()
            return
        }

        // Log.d(TAG, "Requesting POST_NOTIFICATIONS before next screen")
        notificationPermissionLauncher.launch(permission)
    }

    private fun navigateFromSplash() {
        if (!isAdded || navigated) return
        navigated = true

        AppVisitManager.incrementLaunchCount()
        ADSAppManage.AppStartingScreenOpen = true

        val action = when {
            !LocaleManager.isLanguageOnboardingDone() -> R.id.action_splash_to_language
            !IntroManager.isIntroCompleted() -> R.id.action_splash_to_intro1
            else -> R.id.action_splash_to_home
        }
        findNavController().navigate(action)
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

    override fun onDestroyView() {
        _binding?.let { SplashAnimationHelper.release(it.ivSplashAnimation) }
        animationReady = false
        navigated = false
        notificationPromptStarted = false
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SplashAds"
        private const val MIN_SPLASH_MS = 2000L
        private const val VIDEO_CACHE_WAIT_MS = 15_000L
    }
}
