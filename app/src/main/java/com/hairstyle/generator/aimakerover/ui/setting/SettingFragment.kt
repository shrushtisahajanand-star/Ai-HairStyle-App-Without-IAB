package com.hairstyle.generator.aimakerover.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentSettingBinding
import com.hairstyle.generator.aimakeover.databinding.ItemSettingsRowBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.ui.language.LanguageFragment
import com.hairstyle.generator.aimakerover.utils.AppLinkHelper
import com.hairstyle.generator.aimakerover.utils.InAppReviewHelper
import com.hairstyle.generator.aimakerover.utils.LocaleManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.RateAppManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        setupRows()
        setupClicks()
        updateLanguageSubtitle()
        updatePremiumUi()
        ScreenAdsHelper.load(this, binding.settingBottomAds, ScreenAdsHelper.settingBottom())
        ScreenAdsHelper.loadSettingCenter(
            this,
            binding.settingCenterAds.root,
            ScreenAdsHelper.settingCenter()
        )
    }


    private fun updateLanguageSubtitle() {
        binding.rowLanguage.tvSubtitle.text = LocaleManager.getLanguageSubtitle()
    }

    private fun updatePremiumUi() {
        val premiumActive = PremiumPlanManager.isPremiumActive()
        binding.upgradeCard.isVisible = !premiumActive
        binding.activeSubscriptionContainer.isVisible = premiumActive
    }

    private fun setupRows() {
        bindRow(
            binding.rowActiveSubscription,
            R.drawable.ic_active_subscription,
            R.string.active_subscription,
            R.string.active_subscription_subtitle
        )
        bindRow(
            binding.rowUserCreations,
            R.drawable.ic_user_creation,
            R.string.user_creations,
            R.string.user_creations_subtitle
        )
        bindRow(
            binding.rowLanguage,
            R.drawable.ic_language,
            R.string.language,
            R.string.language_subtitle
        )
        bindRow(
            binding.rowShareApp,
            R.drawable.ic_share,
            R.string.share_app,
            R.string.share_app_subtitle
        )
        bindRow(
            binding.rowRateUs,
            R.drawable.ic_rate_us,
            R.string.rate_us,
            R.string.rate_us_subtitle
        )
        bindRow(
            binding.rowFeedback,
            R.drawable.ic_feed_back,
            R.string.feedback,
            R.string.feedback_subtitle
        )
        bindRow(
            binding.rowPrivacyPolicy,
            R.drawable.ic_privacy_policy,
            R.string.privacy_policy,
            R.string.privacy_policy_subtitle
        )
        bindRow(
            binding.rowTermsOfService,
            R.drawable.ic_terms_and_condition,
            R.string.terms_of_service,
            R.string.terms_of_service_subtitle
        )
        bindRow(
            binding.rowCheckForUpdate,
            R.drawable.ic_check_for_update,
            R.string.check_for_update,
            R.string.check_for_update_subtitle
        )
    }

    private fun bindRow(
        rowBinding: ItemSettingsRowBinding,
        iconRes: Int,
        titleRes: Int,
        subtitleRes: Int
    ) {
        rowBinding.ivIcon.setImageResource(iconRes)
        rowBinding.tvTitle.setText(titleRes)
        rowBinding.tvSubtitle.setText(subtitleRes)
    }

    private fun setupClicks() {
        binding.btnUpgrade.setOnClickListener {
            findNavController().navigate(R.id.action_setting_to_premium)
        }
        binding.rowActiveSubscription.root.setOnClickListener {
            findNavController().navigate(R.id.action_setting_to_activeSubscription)
        }
        binding.rowUserCreations.root.setOnClickListener {
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.settingRows()) {
                findNavController().navigate(R.id.action_setting_to_userCreation)
            }
        }
        binding.rowLanguage.root.setOnClickListener {
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.settingRows()) {
                findNavController().navigate(
                    R.id.action_setting_to_language,
                    bundleOf(LanguageFragment.ARG_FROM_SETTINGS to true)
                )
            }
        }
        binding.rowShareApp.root.setOnClickListener {
            AppLinkHelper.shareApp(requireContext())
        }
        binding.rowRateUs.root.setOnClickListener {
            RateAppManager.markUserRated()
            val activity = activity
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                InAppReviewHelper.launch(activity)
            } else {
                AppLinkHelper.openPlayStore(requireContext())
            }
        }
        binding.rowFeedback.root.setOnClickListener {
            findNavController().navigate(R.id.action_setting_to_feedback)
        }
        binding.rowPrivacyPolicy.root.setOnClickListener {
            AppLinkHelper.openPrivacyPolicy(requireContext())
        }
        binding.rowTermsOfService.root.setOnClickListener {
            AppLinkHelper.openPrivacyPolicy(requireContext())
        }
        binding.rowCheckForUpdate.root.setOnClickListener {
            showSnackbar(getString(R.string.checking_for_update))
            AppLinkHelper.openPlayStore(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        updateLanguageSubtitle()
        updatePremiumUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
