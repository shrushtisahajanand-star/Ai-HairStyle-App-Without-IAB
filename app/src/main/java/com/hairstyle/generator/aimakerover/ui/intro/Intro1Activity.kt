package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import com.hairstyle.generator.aimakeover.databinding.FragmentIntro1Binding
import com.hairstyle.generator.aimakeover.databinding.FragmentIntroVideoBinding
import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.data.repository.VideosRepository
import com.hairstyle.generator.aimakerover.ui.AppNavigator
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Intro screen 1 entry.
 * - Firebase `intro_show_screen_type = single` → ViewPager (1a–1d in one screen).
 * - Firebase `intro_show_screen_type = multi` → first video screen, then Intro1b/1c/1d.
 */
@AndroidEntryPoint
class Intro1Activity : BaseActivity() {

    override val useLightStatusBarIcons = true

    private var singleBinding: FragmentIntro1Binding? = null
    private var multiBinding: FragmentIntroVideoBinding? = null

    @Inject lateinit var videosRepository: VideosRepository
    @Inject lateinit var okHttpClient: OkHttpClient

    private var singleBinder: Intro1SingleScreenBinder? = null
    private var multiBinder: IntroVideoScreenBinder? = null

    private val isSingleScreen: Boolean
        get() = ADSMainClass.isIntroSingleScreen()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isSingleScreen) {
            val binding = FragmentIntro1Binding.inflate(layoutInflater).also { singleBinding = it }
            setContentView(binding.root)
            setupWindow(binding.root)
            singleBinder = Intro1SingleScreenBinder(
                activity = this,
                binding = binding,
                videosRepository = videosRepository,
                okHttpClient = okHttpClient
            ).also { it.setup() }
        } else {
            val binding = FragmentIntroVideoBinding.inflate(layoutInflater).also { multiBinding = it }
            setContentView(binding.root)
            setupWindow(binding.root)
            val page = Intro1Data.videoPages[0]
            multiBinder = IntroVideoScreenBinder(
                activity = this,
                binding = binding,
                page = page,
                pageIndex = 0,
                adsConfig = ScreenAdsHelper.intro1(),
                videosRepository = videosRepository,
                okHttpClient = okHttpClient,
                onContinue = {
                    InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro1()) {
                        AppNavigator.openIntro1b(this)
                    }
                }
            ).also { it.setup() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSingleScreen) {
            singleBinder?.onResume()
        } else {
            multiBinder?.onResume()
        }
    }

    override fun onPause() {
        if (isSingleScreen) {
            singleBinder?.onPause()
        } else {
            multiBinder?.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        singleBinder?.onDestroyView()
        singleBinder = null
        multiBinder?.onDestroyView()
        multiBinder = null
        singleBinding = null
        multiBinding = null
        super.onDestroy()
    }
}
