package com.hairstyle.changer.aitryon.ui.intro

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hairstyle.changer.aitryon.databinding.FragmentIntroVideoBinding
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.repository.VideosRepository
import com.hairstyle.changer.aitryon.utils.IntroVideoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/** Shared binder for intro video screens 1 / 2 / 3 (separate Activity classes). */
internal class IntroVideoScreenBinder(
    private val activity: AppCompatActivity,
    private val binding: FragmentIntroVideoBinding,
    private val page: Intro1Page.VideoPage,
    private val pageIndex: Int,
    private val adsConfig: ScreenAdsHelper.Config,
    private val videosRepository: VideosRepository,
    private val okHttpClient: OkHttpClient,
    private val onContinue: () -> Unit
) {
    private var videoPrepared = false

    fun setup() {
        activity.setupSystemBackNavigation()
        ScreenAdsHelper.load(activity, binding.root, adsConfig)

        binding.dotsIndicator.setDotCount(Intro1Data.TOTAL_FLOW_DOTS)
        binding.dotsIndicator.setSelectedIndex(pageIndex)
        binding.tvTitle.setText(page.titleRes)
        binding.tvSubtitle.setText(page.subtitleRes)
        binding.btnContinue.setText(page.buttonRes)
        binding.btnContinue.setOnClickListener { onContinue() }

        // Show fallback immediately — never leave a blank screen while cache/network runs
        binding.ivSplashVector.visibility = View.VISIBLE
        binding.ivVideoPlaceholder.setImageResource(page.fallbackImageRes)
        binding.ivVideoPlaceholder.visibility = View.VISIBLE
        binding.shimmerIntroVideo.visibility = View.GONE

        // Splash already started intro cache; play cache-first, then wait for THIS page file.
        binding.ivVideoPlaceholder.post { playVideo() }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            videosRepository.ensureCached(page.videoKey)
            withContext(Dispatchers.Main) {
                if (!activity.isFinishing && !activity.isDestroyed && !videoPrepared) {
                    playVideo()
                }
            }
            videosRepository.preloadIntroVideos()
        }
    }

    fun onResume() {
        if (videoPrepared) {
            IntroVideoHelper.resume(binding.ivVideoPlaceholder)
        }
    }

    fun onPause() {
        IntroVideoHelper.pause(binding.ivVideoPlaceholder)
    }

    fun onDestroyView() {
        IntroVideoHelper.release(binding.ivVideoPlaceholder)
        videoPrepared = false
    }

    private fun playVideo() {
        if (activity.isFinishing || activity.isDestroyed) return
        binding.ivSplashVector.visibility = View.VISIBLE
        if (videoPrepared) {
            IntroVideoHelper.resume(binding.ivVideoPlaceholder)
            return
        }
        IntroVideoHelper.play(
            webpView = binding.ivVideoPlaceholder,
            shimmerLayout = binding.shimmerIntroVideo,
            videoTitleKey = page.videoKey,
            fallbackImageRes = page.fallbackImageRes,
            onPrepared = { _, _ -> videoPrepared = true }
        )
    }
}
