package com.hairstyle.changer.aitryon.ui.intro

import android.os.Bundle
import com.hairstyle.changer.aitryon.databinding.FragmentIntro2Binding
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
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
