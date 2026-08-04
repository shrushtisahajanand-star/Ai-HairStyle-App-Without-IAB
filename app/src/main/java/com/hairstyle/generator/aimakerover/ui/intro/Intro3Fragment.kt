package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.databinding.FragmentIntro3Binding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.utils.UserGenderManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Intro3Fragment : Fragment() {

    private var _binding: FragmentIntro3Binding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IntroOptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntro3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
