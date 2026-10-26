package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import com.hairstyle.generator.aimakeover.databinding.FragmentIntro3Binding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import com.hairstyle.generator.aimakerover.utils.UserGenderManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Intro3Activity : BaseActivity() {

    override val useLightStatusBarIcons = false

    private var _binding: FragmentIntro3Binding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IntroOptionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentIntro3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        setupSystemBackNavigation()
        binding.btnClose.applyStatusBarPadding()
        setupIntroCloseButton(binding.btnClose)

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.intro3())

        adapter = IntroOptionAdapter { option ->
            UserGenderManager.setGenderFromIntroOptionId(option.id)
            InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro3()) {
                navigateIntroToPremiumOrHome()
            }
        }
        binding.rvOptions.adapter = adapter
        binding.rvOptions.addItemDecoration(IntroItemSpacingDecoration())
        adapter.submitList(Intro3Data.options)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
