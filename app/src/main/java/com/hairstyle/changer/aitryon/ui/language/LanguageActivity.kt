package com.hairstyle.changer.aitryon.ui.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.hairstyle.changer.aitryon.databinding.FragmentLanguageBinding
import com.hairstyle.changer.aitryon.Ads.ADSAppManage
import com.hairstyle.changer.aitryon.Ads.ADSBannerAdaptive
import com.hairstyle.changer.aitryon.Ads.ADSConsentManager
import com.hairstyle.changer.aitryon.Ads.ADSInterDisplay
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.ADSNativeDisplay
import com.hairstyle.changer.aitryon.Ads.ADSUtilitis
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.intro.setupSystemBackNavigation
import com.hairstyle.changer.aitryon.utils.LocaleManager
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageActivity : BaseActivity() {

    override val useLightStatusBarIcons = false

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LanguageAdapter
    private var selectedCode: String = ""
    private var savedCode: String = LocaleManager.DEFAULT_LANGUAGE
    private var fromSettings: Boolean = false
    private var showNextJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        fromSettings = intent.getBooleanExtra(ARG_FROM_SETTINGS, false)
        savedCode = LocaleManager.getLanguageCode()

        // Locale recreate after Confirm — finish navigation without a second Confirm tap.
        when (LocaleManager.consumePendingAfterLanguageChange()) {
            LocaleManager.PENDING_NAV_INTRO -> {
                AppNavigator.openIntro1(this)
                return
            }
            LocaleManager.PENDING_NAV_UP -> {
                finish()
                return
            }
        }

        setupBackNavigation()
        setupLanguageList()

        binding.btnConfirm.setOnClickListener {
            onConfirmClicked()
        }

        if (ADSMainClass.shouldShowConsentOnLanguage()) {
            continueAfterConsent { loadLanguageAds() }
        } else {
            initializeAdsAndLoad { loadLanguageAds() }
        }
    }

    private fun continueAfterConsent(action: () -> Unit) {
        if (isFinishing || isDestroyed) {
            action()
            return
        }

        // No internet → skip consent form, continue next
        if (!ADSUtilitis.IsNetworkConnected(this)) {
            // Log.d(TAG, "No internet — skip consent, continue")
            action()
            return
        }

        ADSConsentManager.gatherConsent(this, true) { formShown ->
            if (!isScreenAlive() || _binding == null) return@gatherConsent
            if (isFinishing || isDestroyed) return@gatherConsent

            if (formShown || ADSConsentManager.canRequestAds(this)) {
                ADSMainClass.setConsentPendingOnLanguage(false)
            }

            initializeAdsAndLoad(action)
        }
    }

    private fun initializeAdsAndLoad(action: () -> Unit) {
        if (ADSConsentManager.canRequestAds(this) && !ADSMainClass.getAds_Free()) {
            ADSAppManage.initializeMobileAdsIfNeeded(applicationContext) {
                runOnUiThread {
                    if (isScreenAlive() && _binding != null && !isFinishing && !isDestroyed) {
                        action()
                    }
                }
            }
        } else {
            runOnUiThread {
                if (isScreenAlive() && _binding != null) {
                    action()
                }
            }
        }
    }

    private fun loadLanguageAds() {
        if (_binding == null) return

        if (!ADSMainClass.getLanguageScreenBottomAdShow()) {
            hideLanguageAdPlaceholders()
            return
        }

        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            hideLanguageAdPlaceholders()
            return
        }

        val adsType = when (ADSMainClass.getLanguageAdsType().trim().lowercase()) {
            "banner" -> "banner"
            else -> "native" // native, empty, or mistaken size value
        }
        val adsSize = ADSNativeDisplay.normalizeNativeSize(ADSMainClass.getLanguageAdsSize())
//        Log.d(TAG, "language_ads_type=$adsType size=$adsSize")

        if (adsType == "native") {
            binding.flBannerSmallPlaceholder.visibility = View.GONE
            binding.cardView.visibility = View.VISIBLE
            ADSNativeDisplay.loadAdmobNativeAdBig(
                ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE),
                binding.flNativeSmallPlaceholder,
                binding.shimmerContainerSmall,
                adsSize,
                this
            )
        } else {
            binding.cardView.visibility = View.GONE
            binding.flNativeSmallPlaceholder.visibility = View.GONE
            ADSBannerAdaptive.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER),
                binding.flBannerSmallPlaceholder,
                binding.shimmerContainerSmall,
                this
            )
        }
    }

    private fun hideLanguageAdPlaceholders() {
        if (_binding == null) return
        binding.shimmerContainerSmall.stopShimmer()
        binding.shimmerContainerSmall.visibility = View.GONE
        binding.flNativeSmallPlaceholder.visibility = View.GONE
        binding.flBannerSmallPlaceholder.visibility = View.GONE
        binding.cardView.visibility = View.GONE
        binding.languageAdContainer.visibility = View.GONE
    }

    private fun setupBackNavigation() {
        if (fromSettings) {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener {
                InterstitialNavHelper.navigateUpWithBackInter(this)
            }
            InterstitialNavHelper.installSystemBackInter(this)
        } else {
            binding.btnBack.visibility = View.GONE
            setupSystemBackNavigation()
        }
    }

    private fun setupLanguageList() {
        adapter = LanguageAdapter { item ->
            onLanguageSelected(item)
        }
        adapter.submitList(LanguageData.languages)
        binding.rvLanguages.adapter = adapter

        if (fromSettings) {
            // Settings: keep current language selected + Next visible.
            selectedCode = savedCode
            adapter.setSelectedCode(selectedCode)
            binding.btnConfirm.visibility = View.VISIBLE
            scrollToSelectedLanguage()
        } else {
            // First-time: no preselect — user must pick a language; Next stays hidden.
            selectedCode = ""
            adapter.setSelectedCode("")
            binding.btnConfirm.visibility = View.INVISIBLE
        }
    }

    private fun onLanguageSelected(item: LanguageItem) {
        selectedCode = item.code
        adapter.setSelectedCode(item.code)

        if (fromSettings) {
            binding.btnConfirm.visibility = View.VISIBLE
            return
        }

        // First-time: show Next icon 1 second after selection.
        showNextJob?.cancel()
        if (binding.btnConfirm.visibility == View.VISIBLE) return

        showNextJob = lifecycleScope.launch {
            delay(NEXT_ICON_DELAY_MS)
            if (_binding == null || !isScreenAlive()) return@launch
            if (selectedCode.isNotBlank()) {
                binding.btnConfirm.visibility = View.VISIBLE
            }
        }
    }

    private fun scrollToSelectedLanguage() {
        val index = LanguageData.languages.indexOfFirst { it.code == selectedCode }
        if (index > 0) {
            binding.rvLanguages.post {
                binding.rvLanguages.scrollToPosition(index)
            }
        }
    }

    private fun onConfirmClicked() {
        if (selectedCode.isBlank()) return

        if (fromSettings) {
            applySelectedLanguage()
            return
        }

        if (!ADSMainClass.getLanguageInterAdsShow()) {
            applySelectedLanguage()
            return
        }

        ADSInterDisplay.ADSInterstitialShowingForced(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplay.OnFinishAds {
                applySelectedLanguage()
            }
        )
    }

    private fun applySelectedLanguage() {
        if (!isScreenAlive() || selectedCode.isBlank()) return

        if (fromSettings) {
            if (selectedCode == savedCode) {
                finish()
                return
            }
            // Activity recreates for locale — resume with finish (no second Confirm).
            LocaleManager.setPendingAfterLanguageChange(LocaleManager.PENDING_NAV_UP)
            LocaleManager.applyLanguage(selectedCode)
            return
        }

        if (selectedCode != savedCode) {
            // Activity recreates for locale — resume with Intro1 (no second Confirm).
            LocaleManager.setPendingAfterLanguageChange(LocaleManager.PENDING_NAV_INTRO)
            LocaleManager.completeOnboarding(selectedCode)
        } else {
            LocaleManager.setLanguageOnboardingDone()
            LocaleManager.setLanguageCode(selectedCode)
            AppNavigator.openIntro1(this)
        }
    }

    override fun onDestroy() {
        showNextJob?.cancel()
        showNextJob = null
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val TAG = "LanguageAds"
        private const val NEXT_ICON_DELAY_MS = 400L
        const val ARG_FROM_SETTINGS = "from_settings"

        fun createIntent(context: Context, fromSettings: Boolean): Intent {
            return Intent(context, LanguageActivity::class.java).apply {
                putExtra(ARG_FROM_SETTINGS, fromSettings)
            }
        }
    }
}
