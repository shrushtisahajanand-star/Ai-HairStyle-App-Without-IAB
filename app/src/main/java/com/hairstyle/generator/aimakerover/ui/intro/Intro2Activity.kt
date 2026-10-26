package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import com.hairstyle.generator.aimakeover.databinding.FragmentIntro2Binding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Intro2Activity : BaseActivity() {

    override val useLightStatusBarIcons = false

    private var _binding: FragmentIntro2Binding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IntroOptionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentIntro2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        setupSystemBackNavigation()
        binding.btnClose.applyStatusBarPadding()
        setupIntroCloseButton(binding.btnClose)

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.intro2())

        adapter = IntroOptionAdapter {
            InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro2()) {
                navigateIntroToPremiumOrHome()
            }
        }
        binding.rvOptions.adapter = adapter
        binding.rvOptions.addItemDecoration(IntroItemSpacingDecoration())
        adapter.submitList(Intro2Data.options)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
