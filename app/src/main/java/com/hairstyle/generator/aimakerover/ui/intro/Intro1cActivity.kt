package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import com.hairstyle.generator.aimakeover.databinding.FragmentIntroVideoBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.data.repository.VideosRepository
import com.hairstyle.generator.aimakerover.ui.AppNavigator
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/** Intro screen 3 — Beard video. Ads: intro_screen1c */
@AndroidEntryPoint
class Intro1cActivity : BaseActivity() {

    override val useLightStatusBarIcons = true

    private var _binding: FragmentIntroVideoBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var videosRepository: VideosRepository
    @Inject lateinit var okHttpClient: OkHttpClient

    private var binder: IntroVideoScreenBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentIntroVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        val page = Intro1Data.videoPages[2]
        binder = IntroVideoScreenBinder(
            activity = this,
            binding = binding,
            page = page,
            pageIndex = 2,
            adsConfig = ScreenAdsHelper.intro1c(),
            videosRepository = videosRepository,
            okHttpClient = okHttpClient,
            onContinue = {
                InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro1c()) {
                    AppNavigator.openIntro1d(this)
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

    override fun onDestroy() {
        binder?.onDestroyView()
        binder = null
        super.onDestroy()
        _binding = null
    }
}
