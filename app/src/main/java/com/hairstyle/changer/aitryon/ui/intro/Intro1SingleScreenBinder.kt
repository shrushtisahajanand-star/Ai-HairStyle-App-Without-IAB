package com.hairstyle.changer.aitryon.ui.intro

import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hairstyle.changer.aitryon.databinding.FragmentIntro1Binding
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.repository.VideosRepository
import com.hairstyle.changer.aitryon.utils.IntroVideoHelper
import com.hairstyle.changer.aitryon.utils.VideoTitleKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/** Single-screen intro: ViewPager with 3 videos + rating (Firebase {@code intro_show_screen_type = single}). */
internal class Intro1SingleScreenBinder(
    private val activity: AppCompatActivity,
    private val binding: FragmentIntro1Binding,
    private val videosRepository: VideosRepository,
    private val okHttpClient: OkHttpClient
) {
    private lateinit var pagerAdapter: Intro1PagerAdapter
    private var reviewAutoScroller: IntroReviewAutoScroller? = null
    private var currentVideoKey: String? = null
    private var videoPrepared = false

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateBottomUi(position)
            handlePageVideo(position)
        }
    }

    fun setup() {
        setupSystemBackNavigation()
        ScreenAdsHelper.load(activity, binding.root, ScreenAdsHelper.intro1())

        binding.dotsIndicator.setDotCount(Intro1Data.pages.size)
        pagerAdapter = Intro1PagerAdapter().apply {
            onRatingPageBound = { recyclerView, page ->
                setupReviewsRecycler(recyclerView, page.reviews)
                if (binding.viewPager.currentItem == Intro1Data.pages.lastIndex) {
                    reviewAutoScroller?.start()
                }
            }
            submitPages(Intro1Data.pages)
        }

        binding.viewPager.apply {
            adapter = pagerAdapter
            isUserInputEnabled = true
            offscreenPageLimit = 2
            registerOnPageChangeCallback(pageChangeCallback)
        }

        binding.btnContinue.setOnClickListener { onContinueClicked() }
        updateBottomUi(0)

        // Splash already cached intro videos — show fallback + play from disk immediately.
        val firstPage = Intro1Data.pages.firstOrNull() as? Intro1Page.VideoPage
        if (firstPage != null) {
            binding.ivSplashVector.visibility = View.VISIBLE
            binding.ivVideoPlaceholder.setImageResource(firstPage.fallbackImageRes)
            binding.ivVideoPlaceholder.visibility = View.VISIBLE
            binding.shimmerIntroVideo.visibility = View.GONE
        }
        binding.viewPager.post { handlePageVideo(binding.viewPager.currentItem) }
        preloadIntroVideosInBackground()
    }

    fun onResume() {
        handlePageVideo(binding.viewPager.currentItem)
    }

    fun onPause() {
        IntroVideoHelper.pause(binding.ivVideoPlaceholder)
        reviewAutoScroller?.stop()
    }

    fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        IntroVideoHelper.release(binding.ivVideoPlaceholder)
        reviewAutoScroller?.stop()
        reviewAutoScroller = null
        currentVideoKey = null
        videoPrepared = false
    }

    private fun setupSystemBackNavigation() {
        activity.onBackPressedDispatcher.addCallback(activity) {
            if (ADSMainClass.getIntroBackSendToNext() && advancePagerOrIntro2OnBack()) {
                return@addCallback
            }
            InterstitialNavHelper.navigateUpWithBackInter(activity)
        }
    }

    private fun advancePagerOrIntro2OnBack(): Boolean {
        val current = binding.viewPager.currentItem
        return if (current < Intro1Data.pages.lastIndex) {
            binding.viewPager.setCurrentItem(current + 1, true)
            true
        } else {
            activity.completeIntroAndGoHome()
            true
        }
    }

    /** Cache current page if needed, then play; preload remaining intro clips in background. */
    private fun preloadIntroVideosInBackground() {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val key = currentVideoKey ?: VideoTitleKeys.INTRO_VIDEO_1
            videosRepository.ensureCached(key)
            withContext(Dispatchers.Main) {
                if (!activity.isFinishing && !activity.isDestroyed && !videoPrepared) {
                    handlePageVideo(binding.viewPager.currentItem)
                }
            }
            videosRepository.preloadIntroVideos()
        }
    }

    private fun handlePageVideo(position: Int) {
        when (val page = Intro1Data.pages.getOrNull(position)) {
            is Intro1Page.VideoPage -> playVideoPage(page)
            is Intro1Page.RatingPage -> showRatingPage()
            null -> Unit
        }
    }

    private fun playVideoPage(page: Intro1Page.VideoPage) {
        reviewAutoScroller?.stop()
        binding.ivSplashVector.visibility = View.VISIBLE

        if (page.videoKey == currentVideoKey && videoPrepared) {
            IntroVideoHelper.resume(binding.ivVideoPlaceholder)
            return
        }

        currentVideoKey = page.videoKey
        videoPrepared = false
        IntroVideoHelper.play(
            webpView = binding.ivVideoPlaceholder,
            shimmerLayout = binding.shimmerIntroVideo,
            videoTitleKey = page.videoKey,
            fallbackImageRes = page.fallbackImageRes,
            onPrepared = { _, _ -> videoPrepared = true }
        )
        activity.lifecycleScope.launch(Dispatchers.IO) {
            videosRepository.ensureCached(page.videoKey)
            withContext(Dispatchers.Main) {
                if (currentVideoKey == page.videoKey && !videoPrepared) {
                    IntroVideoHelper.play(
                        webpView = binding.ivVideoPlaceholder,
                        shimmerLayout = binding.shimmerIntroVideo,
                        videoTitleKey = page.videoKey,
                        fallbackImageRes = page.fallbackImageRes,
                        onPrepared = { _, _ -> videoPrepared = true }
                    )
                }
            }
        }
    }

    private fun showRatingPage() {
        IntroVideoHelper.pause(binding.ivVideoPlaceholder)
        binding.ivVideoPlaceholder.visibility = View.GONE
        binding.shimmerIntroVideo.visibility = View.GONE
        binding.ivSplashVector.visibility = View.GONE
        currentVideoKey = null
        videoPrepared = false
        reviewAutoScroller?.start()
    }

    private fun setupReviewsRecycler(
        recyclerView: RecyclerView,
        reviews: List<IntroReviewItem>
    ) {
        val reviewAdapter = IntroReviewAdapter().apply { submitList(reviews) }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = reviewAdapter
            overScrollMode = View.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
        }
        reviewAutoScroller?.stop()
        reviewAutoScroller = IntroReviewAutoScroller(recyclerView)
    }

    private fun updateBottomUi(position: Int) {
        val page = Intro1Data.pages[position]
        binding.tvTitle.setText(
            when (page) {
                is Intro1Page.VideoPage -> page.titleRes
                is Intro1Page.RatingPage -> page.titleRes
            }
        )
        binding.tvSubtitle.setText(
            when (page) {
                is Intro1Page.VideoPage -> page.subtitleRes
                is Intro1Page.RatingPage -> page.subtitleRes
            }
        )
        binding.btnContinue.setText(
            when (page) {
                is Intro1Page.VideoPage -> page.buttonRes
                is Intro1Page.RatingPage -> page.buttonRes
            }
        )
        binding.dotsIndicator.setSelectedIndex(position)
    }

    private fun onContinueClicked() {
        val current = binding.viewPager.currentItem
        val interConfig = when (current) {
            0 -> ScreenInterAds.intro1()
            1 -> ScreenInterAds.intro1b()
            2 -> ScreenInterAds.intro1c()
            else -> ScreenInterAds.intro1d()
        }
        InterstitialNavHelper.showOnIntroClick(activity, interConfig) {
            if (current < Intro1Data.pages.lastIndex) {
                binding.viewPager.setCurrentItem(current + 1, true)
            } else {
                activity.completeIntroAndGoHome()
            }
        }
    }
}
