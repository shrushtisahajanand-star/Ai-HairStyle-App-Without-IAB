package com.hairstyle.generator.aimakerover.ui.home.`fun`

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.ConcatAdapter
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentFunBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.domain.model.FunStyle
import com.hairstyle.generator.aimakerover.domain.model.GenerationType
import com.hairstyle.generator.aimakerover.ui.AppNavigator
import com.hairstyle.generator.aimakerover.ui.common.RateAppDialog
import com.hairstyle.generator.aimakerover.ui.common.ShimmerItemFooterAdapter
import com.hairstyle.generator.aimakerover.ui.home.BeforeAfterStyleAdapter
import com.hairstyle.generator.aimakerover.ui.home.HomePageActivity
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FunFragment : Fragment() {

    private var _binding: FragmentFunBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FunViewModel by viewModels()
    private lateinit var funAdapter: BeforeAfterStyleAdapter<FunStyle>
    private lateinit var funShimmerPlaceholder: ShimmerItemFooterAdapter
    private lateinit var funShimmerFooter: ShimmerItemFooterAdapter
    private var activeTab = FunTab.MEN.position

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        funAdapter = BeforeAfterStyleAdapter(::onFunStyleSelected)
        funShimmerPlaceholder = ShimmerItemFooterAdapter(R.layout.item_shimmer_before_after, shimmerItemCount = 3)
        funShimmerFooter = ShimmerItemFooterAdapter(R.layout.item_shimmer_before_after, shimmerItemCount = 1)
        binding.shimmerFunStyles.isVisible = false
        val concatAdapter = ConcatAdapter(funShimmerPlaceholder, funAdapter, funShimmerFooter)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvFunStyles.layoutManager = layoutManager
        binding.rvFunStyles.adapter = concatAdapter
        binding.rvFunStyles.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = concatAdapter.itemCount
                if (total > 0 && lastVisible >= total - 3) {
                    viewModel.loadMoreForTab(activeTab)
                }
            }
        })

        funShimmerPlaceholder.bindPlaceholder(isLoading = true, hasItems = false, hasError = false)

        setupTabLayout()
        observeFunStyles()
    }

    override fun onResume() {
        super.onResume()
        refreshVisibleImages()
        if (!isHidden) {
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
        if (_binding == null || !::funAdapter.isInitialized) return
        if (funAdapter.itemCount > 0) {
            funAdapter.notifyItemRangeChanged(0, funAdapter.itemCount)
        }
    }

    private fun setupTabLayout() {
        val tabTitles = listOf(
            R.string.fun_tab_men,
            R.string.fun_tab_women,
            R.string.fun_tab_baby
        )

        tabTitles.forEach { titleRes ->
            binding.funTabLayout.addTab(
                binding.funTabLayout.newTab().setText(titleRes)
            )
        }

        binding.funTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                activeTab = tab.position
                viewModel.setActiveTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        if (binding.funTabLayout.tabCount > 0) {
            binding.funTabLayout.getTabAt(FunTab.MEN.position)?.select()
        }
    }

    private fun observeFunStyles() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTab
                    .flatMapLatest { tab -> viewModel.tabState(tab) }
                    .collect { state ->
                        val hasItems = state.items.isNotEmpty()
                        val hasError = state.error != null
                        funShimmerPlaceholder.bindPlaceholder(state.isLoading, hasItems, hasError)
                        funShimmerFooter.bindLoadingMore(
                            loadingMore = state.isLoadingMore,
                            hasItems = hasItems,
                            hasMore = state.hasMore
                        )
                        funAdapter.submitList(state.items)
                        state.error?.let { showSnackbar(it) }
                    }
            }
        }
    }

    private fun onFunStyleSelected(style: FunStyle) {
        if (style.hairstyleApiValue.isBlank() || style.hairColorApiValue.isBlank()) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }

        PrefManager.setGenerationType(GenerationType.HAIRSTYLE)
        PrefManager.setSelectedHairstyle(
            title = style.title,
            imageUrl = style.afterImageUrl,
            apiValue = style.hairstyleApiValue
        )
        PrefManager.setSelectedHairColor(
            title = style.hairColorApiValue,
            imageUrl = style.afterImageUrl,
            apiValue = style.hairColorApiValue
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
