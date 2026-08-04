package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentIntroVideoBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.data.repository.VideosRepository
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/** Intro screen 3 — Beard video. Ads: intro_screen1c */
@AndroidEntryPoint
class Intro1cFragment : Fragment() {

    private var _binding: FragmentIntroVideoBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var videosRepository: VideosRepository
    @Inject lateinit var okHttpClient: OkHttpClient

    private var binder: IntroVideoScreenBinder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val page = Intro1Data.videoPages[2]
        binder = IntroVideoScreenBinder(
            fragment = this,
            binding = binding,
            page = page,
            pageIndex = 2,
            adsConfig = ScreenAdsHelper.intro1c(),
            videosRepository = videosRepository,
            okHttpClient = okHttpClient,
            onContinue = {
                InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro1c()) {
                    findNavController().navigate(R.id.action_intro1c_to_intro1d)
                }
            }
        ).also { it.setup() }
    }

    override fun onResume() {
        super.onResume()
        binder?.onResume()
    }

    override fun onPause() {
        binder?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binder?.onDestroyView()
        binder = null
        super.onDestroyView()
        _binding = null
    }
}
