package com.hairstyle.changer.aitryon.ui.intro

import android.os.Bundle
import com.hairstyle.changer.aitryon.databinding.FragmentIntroVideoBinding
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.repository.VideosRepository
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/** Intro screen 2 — Mens hairstyle video. Ads: intro_screen1b */
@AndroidEntryPoint
class Intro1bActivity : BaseActivity() {

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

        val page = Intro1Data.videoPages[1]
        binder = IntroVideoScreenBinder(
            activity = this,
            binding = binding,
            page = page,
            pageIndex = 1,
            adsConfig = ScreenAdsHelper.intro1b(),
            videosRepository = videosRepository,
            okHttpClient = okHttpClient,
            onContinue = {
                InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro1b()) {
                    AppNavigator.openIntro1c(this)
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
