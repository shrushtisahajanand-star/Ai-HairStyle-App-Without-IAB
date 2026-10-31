package com.hairstyle.changer.aitryon.ui.setting

import android.os.Bundle
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentSettingBinding
import com.hairstyle.changer.aitryon.databinding.ItemSettingsRowBinding
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.utils.AppLinkHelper
import com.hairstyle.changer.aitryon.utils.InAppReviewHelper
import com.hairstyle.changer.aitryon.utils.LocaleManager
import com.hairstyle.changer.aitryon.utils.RateAppManager
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import com.hairstyle.changer.aitryon.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        setupRows()
        setupClicks()
        updateLanguageSubtitle()
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

    private fun setupRows() {
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
        binding.rowUserCreations.root.setOnClickListener {
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.settingRows()) {
                AppNavigator.openUserCreation(this)
            }
        }
        binding.rowLanguage.root.setOnClickListener {
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.settingRows()) {
                AppNavigator.openLanguage(this, fromSettings = true)
            }
        }
        binding.rowShareApp.root.setOnClickListener {
            AppLinkHelper.shareApp(this)
        }
        binding.rowRateUs.root.setOnClickListener {
            RateAppManager.markUserRated()
            if (!isFinishing && !isDestroyed) {
                InAppReviewHelper.launch(this)
            } else {
                AppLinkHelper.openPlayStore(this)
            }
        }
        binding.rowFeedback.root.setOnClickListener {
            AppNavigator.openFeedback(this)
        }
        binding.rowPrivacyPolicy.root.setOnClickListener {
            AppLinkHelper.openPrivacyPolicy(this)
        }
        binding.rowCheckForUpdate.root.setOnClickListener {
            showSnackbar(getString(R.string.checking_for_update))
            AppLinkHelper.openPlayStore(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateLanguageSubtitle()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
