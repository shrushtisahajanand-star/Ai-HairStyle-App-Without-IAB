package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentIntro2Binding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Intro2Fragment : Fragment() {

    private var _binding: FragmentIntro2Binding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IntroOptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntro2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()
        setupSystemBackNavigation()
        binding.btnClose.applyStatusBarPadding()
        setupIntroCloseButton(binding.btnClose)

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.intro2())

        adapter = IntroOptionAdapter {
            InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro2()) {
                findNavController().navigate(R.id.action_intro2_to_intro3)
            }
        }
        binding.rvOptions.adapter = adapter
        binding.rvOptions.addItemDecoration(IntroItemSpacingDecoration())
        adapter.submitList(Intro2Data.options)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
