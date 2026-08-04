package com.hairstyle.generator.aimakerover.ui.creation

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentUserCreationBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.ui.category.GridSpacingItemDecoration
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserCreationFragment : Fragment() {

    private var _binding: FragmentUserCreationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserCreationViewModel by viewModels()
    private lateinit var adapter: UserCreationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.userCreation())

        adapter = UserCreationAdapter { creation ->
            openPreview(creation.id)
        }
        val spacingPx = (8 * resources.displayMetrics.density).toInt()
        binding.rvCreations.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        if (binding.rvCreations.itemDecorationCount == 0) {
            binding.rvCreations.addItemDecoration(
                GridSpacingItemDecoration(GRID_SPAN_COUNT, spacingPx)
            )
        }
        binding.rvCreations.adapter = adapter

        observeCreations()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCreations()
    }

    private fun observeCreations() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    val showEmpty = !state.isLoading && state.items.isEmpty()
                    binding.tvEmpty.isVisible = showEmpty
                    binding.rvCreations.isVisible = !showEmpty
                }
            }
        }
    }

    private fun openPreview(creationId: String) {
        findNavController().navigate(
            R.id.action_userCreation_to_preview,
            bundleOf(UserCreationPreviewFragment.ARG_CREATION_ID to creationId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
    }
}
