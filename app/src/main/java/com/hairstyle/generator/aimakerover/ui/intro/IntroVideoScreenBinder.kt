package com.hairstyle.generator.aimakerover.ui.intro

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hairstyle.generator.aimakeover.databinding.FragmentIntroVideoBinding
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.data.repository.VideosRepository
import com.hairstyle.generator.aimakerover.utils.IntroVideoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/** Shared binder for intro video screens 1 / 2 / 3 (separate Fragment classes). */
internal class IntroVideoScreenBinder(
    private val fragment: Fragment,
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
        fragment.setupSystemBackNavigation()
        ScreenAdsHelper.load(fragment, binding.root, adsConfig)

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

        // Splash already cached intro videos; play right away from disk/URL
        binding.ivVideoPlaceholder.post { playVideo() }

        // Refresh URLs / top-up cache in background (do not block play)
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            runCatching { videosRepository.fetchAndSaveAllVideos() }
            IntroVideoHelper.preload(
                fragment.requireContext().applicationContext,
                okHttpClient,
                page.videoKey
            )
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
        if (!fragment.isAdded || fragment.view == null) return
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
