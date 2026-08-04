package com.hairstyle.generator.aimakerover.ui.language

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentLanguageBinding
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppManage
import com.hairstyle.generator.aimakerover.Advertisement.ADSBannerAdaptive
import com.hairstyle.generator.aimakerover.Advertisement.ADSConsentManager
import com.hairstyle.generator.aimakerover.Advertisement.ADSInterDisplay
import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.Advertisement.ADSNativeDisplay
import com.hairstyle.generator.aimakerover.Advertisement.ADSUtilitis
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.ui.intro.setupSystemBackNavigation
import com.hairstyle.generator.aimakerover.utils.LocaleManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageFragment : Fragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LanguageAdapter
    private var selectedCode: String = ""
    private var savedCode: String = LocaleManager.DEFAULT_LANGUAGE
    private var fromSettings: Boolean = false
    private var showNextJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()
        fromSettings = arguments?.getBoolean(ARG_FROM_SETTINGS, false) ?: false
        savedCode = LocaleManager.getLanguageCode()

        // Locale recreate after Confirm â€” finish navigation without a second Confirm tap.
        when (LocaleManager.consumePendingAfterLanguageChange()) {
            LocaleManager.PENDING_NAV_INTRO -> {
                findNavController().navigate(R.id.action_language_to_intro1)
                return
            }
            LocaleManager.PENDING_NAV_UP -> {
                findNavController().navigateUp()
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
            // Log.d(TAG, "No internet â€” skip consent, continue")
            action()
            return
        }

        ADSConsentManager.gatherConsent(activity, true) { formShown ->
            if (!isAdded || _binding == null) return@gatherConsent
            if (activity.isFinishing || activity.isDestroyed) return@gatherConsent

            if (formShown || ADSConsentManager.canRequestAds(activity)) {
                ADSMainClass.setConsentPendingOnLanguage(false)
            }

            initializeAdsAndLoad(action)
        }
    }

    private fun initializeAdsAndLoad(action: () -> Unit) {
        val activity = activity ?: run {
            action()
            return
        }
        if (ADSConsentManager.canRequestAds(activity) && !ADSMainClass.getAds_Free()) {
            ADSAppManage.initializeMobileAdsIfNeeded(activity.applicationContext) {
                activity.runOnUiThread {
                    if (isAdded && _binding != null && !activity.isFinishing && !activity.isDestroyed) {
                        action()
                    }
                }
            }
        } else {
            activity.runOnUiThread {
                if (isAdded && _binding != null) {
                    action()
                }
            }
        }
    }

    private fun loadLanguageAds() {
        val activity = activity
        if (activity == null || _binding == null) return

        if (!ADSMainClass.getLanguageScreenBottomAdShow()) {
            hideLanguageAdPlaceholders()
            return
        }

        if (!ADSConsentManager.canRequestAds(activity) || ADSMainClass.getAds_Free()) {
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
                activity
            )
        } else {
            binding.cardView.visibility = View.GONE
            binding.flNativeSmallPlaceholder.visibility = View.GONE
            ADSBannerAdaptive.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER),
                binding.flBannerSmallPlaceholder,
                binding.shimmerContainerSmall,
                activity
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
            // First-time: no preselect â€” user must pick a language; Next stays hidden.
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

        showNextJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(NEXT_ICON_DELAY_MS)
            if (_binding == null || !isAdded) return@launch
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

        val activity = activity ?: run {
            applySelectedLanguage()
            return
        }

        ADSInterDisplay.ADSInterstitialShowingForced(
            activity,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplay.OnFinishAds {
                applySelectedLanguage()
            }
        )
    }

    private fun applySelectedLanguage() {
        if (!isAdded || selectedCode.isBlank()) return

        if (fromSettings) {
            if (selectedCode == savedCode) {
                findNavController().navigateUp()
                return
            }
            // Activity recreates for locale â€” resume with navigateUp (no second Confirm).
            LocaleManager.setPendingAfterLanguageChange(LocaleManager.PENDING_NAV_UP)
            LocaleManager.applyLanguage(selectedCode)
            return
        }

        if (selectedCode != savedCode) {
            // Activity recreates for locale â€” resume with Intro1 (no second Confirm).
            LocaleManager.setPendingAfterLanguageChange(LocaleManager.PENDING_NAV_INTRO)
            LocaleManager.completeOnboarding(selectedCode)
        } else {
            LocaleManager.setLanguageOnboardingDone()
            LocaleManager.setLanguageCode(selectedCode)
            findNavController().navigate(R.id.action_language_to_intro1)
        }
    }

    override fun onDestroyView() {
        showNextJob?.cancel()
        showNextJob = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LanguageAds"
        private const val NEXT_ICON_DELAY_MS = 1_000L
        const val ARG_FROM_SETTINGS = "from_settings"
    }
}
