package com.hairstyle.generator.aimakerover.ui.home.beard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentBeardBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.domain.model.BeardStyle
import com.hairstyle.generator.aimakerover.domain.model.GenerationType
import com.hairstyle.generator.aimakerover.ui.AppNavigator
import com.hairstyle.generator.aimakerover.ui.common.RateAppDialog
import com.hairstyle.generator.aimakerover.ui.common.ShimmerItemFooterAdapter
import com.hairstyle.generator.aimakerover.ui.home.BeforeAfterStyleAdapter
import com.hairstyle.generator.aimakerover.ui.home.HomePageActivity
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BeardFragment : Fragment() {

    private var _binding: FragmentBeardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BeardViewModel by viewModels()
    private lateinit var beardAdapter: BeforeAfterStyleAdapter<BeardStyle>
    private lateinit var beardShimmerPlaceholder: ShimmerItemFooterAdapter
    private lateinit var beardShimmerFooter: ShimmerItemFooterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBeardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        beardAdapter = BeforeAfterStyleAdapter(::onBeardStyleSelected)
        beardShimmerPlaceholder = ShimmerItemFooterAdapter(R.layout.item_shimmer_before_after, shimmerItemCount = 3)
        beardShimmerFooter = ShimmerItemFooterAdapter(R.layout.item_shimmer_before_after, shimmerItemCount = 1)
        binding.shimmerBeardStyles.isVisible = false
        val concatAdapter = ConcatAdapter(beardShimmerPlaceholder, beardAdapter, beardShimmerFooter)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvBeardStyles.layoutManager = layoutManager
        binding.rvBeardStyles.adapter = concatAdapter
        binding.rvBeardStyles.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = concatAdapter.itemCount
                if (total > 0 && lastVisible >= total - 3) {
                    viewModel.loadMoreBeardStyles()
                }
            }
        })

        beardShimmerPlaceholder.bindPlaceholder(isLoading = true, hasItems = false, hasError = false)

        observeBeardStyles()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            refreshVisibleImages()
            (activity as? HomePageActivity)?.maybeShowRateAppPrompt()
                ?: RateAppDialog.showIfNeededForAppOpen(this)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshVisibleImages()
            (activity as? HomePageActivity)?.maybeShowRateAppPrompt()
                ?: RateAppDialog.showIfNeededForAppOpen(this)
        }
    }

    private fun refreshVisibleImages() {
        if (_binding == null || !::beardAdapter.isInitialized) return
        if (beardAdapter.itemCount > 0) {
            beardAdapter.notifyItemRangeChanged(0, beardAdapter.itemCount)
        }
    }

    private fun observeBeardStyles() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.beardStyles.collect { state ->
                    val hasItems = state.items.isNotEmpty()
                    val hasError = state.error != null
                    beardShimmerPlaceholder.bindPlaceholder(state.isLoading, hasItems, hasError)
                    beardShimmerFooter.bindLoadingMore(
                        loadingMore = state.isLoadingMore,
                        hasItems = hasItems,
                        hasMore = state.hasMore
                    )
                    beardAdapter.submitList(state.items)
                    state.error?.let { showSnackbar(it) }
                }
            }
        }
    }

    private fun onBeardStyleSelected(style: BeardStyle) {
        if (style.apiValue.isBlank()) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }

        PrefManager.setGenerationType(GenerationType.BEARD)
        PrefManager.clearSelectedHairColor()
        PrefManager.setSelectedHairstyle(
            title = style.title,
            imageUrl = style.afterImageUrl,
            apiValue = style.apiValue
        )

        InterstitialNavHelper.showOnClick(this, ScreenInterAds.funBeardTryIt()) {
            val host = activity ?: return@showOnClick
            AppNavigator.openPhotoSelect(host)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
