package com.hairstyle.generator.aimakerover.ui.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentIntroRatingBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import dagger.hilt.android.AndroidEntryPoint

/** Intro flow page 4 — rating / reviews (was ViewPager last page). Ads: intro_screen1d */
@AndroidEntryPoint
class Intro1dFragment : Fragment() {

    private var _binding: FragmentIntroRatingBinding? = null
    private val binding get() = _binding!!

    private var reviewAutoScroller: IntroReviewAutoScroller? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                findNavController().navigate(R.id.action_intro1d_to_intro2)
            }
        }

        val reviewAdapter = IntroReviewAdapter().apply { submitList(page.reviews) }
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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

    override fun onDestroyView() {
        reviewAutoScroller?.stop()
        reviewAutoScroller = null
        super.onDestroyView()
        _binding = null
    }
}
