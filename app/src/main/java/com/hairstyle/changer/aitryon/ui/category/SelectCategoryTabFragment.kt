package com.hairstyle.changer.aitryon.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentSelectCategoryTabBinding
import com.hairstyle.changer.aitryon.ui.common.ShimmerItemFooterAdapter
import com.hairstyle.changer.aitryon.ui.common.ItemLockGate
import com.hairstyle.changer.aitryon.utils.UnlockConfig
import com.hairstyle.changer.aitryon.utils.bindListLoading
import com.hairstyle.changer.aitryon.utils.showListShimmer
import com.hairstyle.changer.aitryon.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectCategoryTabFragment : Fragment() {

    private var _binding: FragmentSelectCategoryTabBinding? = null
    private val binding get() = _binding!!

    private val tabIndex: Int by lazy { requireArguments().getInt(ARG_TAB_INDEX) }
    private val viewModel: SelectCategoryViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: CategoryHairstyleAdapter
    private lateinit var shimmerFooter: ShimmerItemFooterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCategoryTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lockGate = ItemLockGate(UnlockConfig.Group.HOME) { activity }
        lockGate.preload()
        adapter = CategoryHairstyleAdapter(lockGate) { item ->
            viewModel.selectHairstyle(tabIndex, item)
        }
        shimmerFooter = ShimmerItemFooterAdapter(R.layout.item_shimmer_category, shimmerItemCount = 3)
        val concatAdapter = ConcatAdapter(adapter, shimmerFooter)

        // Show grid shimmer immediately until first data arrives
        binding.rvHairstyles.isVisible = false
        binding.shimmerHairstyles.showListShimmer()

        val gridLayoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        binding.rvHairstyles.layoutManager = gridLayoutManager
        binding.rvHairstyles.setHasFixedSize(false)
        val spacingPx = (6 * resources.displayMetrics.density).toInt()
        if (binding.rvHairstyles.itemDecorationCount == 0) {
            binding.rvHairstyles.addItemDecoration(
                GridSpacingItemDecoration(GRID_SPAN_COUNT, spacingPx)
            )
        }
        binding.rvHairstyles.adapter = concatAdapter
        binding.rvHairstyles.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lastVisible = gridLayoutManager.findLastVisibleItemPosition()
                val total = concatAdapter.itemCount
                if (total > 0 && lastVisible >= total - 4) {
                    viewModel.loadMoreForTab(tabIndex)
                }
            }
        })

        observeState()
        viewModel.loadForTab(tabIndex)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tabState(tabIndex).collect { state ->
                        val hasItems = state.items.isNotEmpty()
                        val hasError = state.error != null
                        binding.shimmerHairstyles.bindListLoading(
                            isLoading = state.isLoading,
                            hasItems = hasItems,
                            hasError = hasError,
                            contentView = binding.rvHairstyles
                        )
                        shimmerFooter.bindLoadingMore(
                            loadingMore = state.isLoadingMore,
                            hasItems = hasItems,
                            hasMore = state.hasMore
                        )
                        adapter.submitList(state.items) {
                            val selected = viewModel.selectedForTab(tabIndex).value
                            adapter.setSelectedId(selected?.id)
                        }
                        state.error?.let { showSnackbar(it) }
                    }
                }
                launch {
                    viewModel.selectedForTab(tabIndex).collect { selected ->
                        adapter.setSelectedId(selected?.id)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAB_INDEX = "tab_index"
        private const val GRID_SPAN_COUNT = 3

        fun newInstance(tabIndex: Int): SelectCategoryTabFragment {
            return SelectCategoryTabFragment().apply {
                arguments = bundleOf(ARG_TAB_INDEX to tabIndex)
            }
        }
    }
}
