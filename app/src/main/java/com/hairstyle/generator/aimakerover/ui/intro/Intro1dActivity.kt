package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.hairstyle.generator.aimakeover.databinding.FragmentIntroRatingBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

/** Intro flow page 4 — rating / reviews (was ViewPager last page). Ads: intro_screen1d */
@AndroidEntryPoint
class Intro1dActivity : BaseActivity() {

    override val useLightStatusBarIcons = true

    private var _binding: FragmentIntroRatingBinding? = null
    private val binding get() = _binding!!

    private var reviewAutoScroller: IntroReviewAutoScroller? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentIntroRatingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        setupSystemBackNavigation()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.intro1d())

        val page = Intro1Data.ratingPage
        binding.dotsIndicator.setDotCount(Intro1Data.TOTAL_FLOW_DOTS)
        binding.dotsIndicator.setSelectedIndex(3)
        binding.tvTitle.setText(page.titleRes)
        binding.tvSubtitle.setText(page.subtitleRes)
        binding.btnContinue.setText(page.buttonRes)
        binding.btnContinue.setOnClickListener {
            InterstitialNavHelper.showOnIntroClick(this, ScreenInterAds.intro1d()) {
                navigateIntroToPremiumOrHome()
            }
        }

        val reviewAdapter = IntroReviewAdapter().apply { submitList(page.reviews) }
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(this@Intro1dActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = reviewAdapter
            overScrollMode = View.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
        }
        reviewAutoScroller = IntroReviewAutoScroller(binding.rvReviews).also { it.start() }
    }

    override fun onResume() {
        super.onResume()
        reviewAutoScroller?.start()
    }

    override fun onPause() {
        reviewAutoScroller?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        reviewAutoScroller?.stop()
        reviewAutoScroller = null
        super.onDestroy()
        _binding = null
    }
}
