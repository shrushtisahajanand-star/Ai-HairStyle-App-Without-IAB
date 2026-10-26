package com.hairstyle.generator.aimakerover.ui.category

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.BottomSheetHairColorBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.domain.model.GenerationType
import com.hairstyle.generator.aimakerover.ui.AppNavigator
import com.hairstyle.generator.aimakerover.ui.common.ShimmerItemFooterAdapter
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HairColorBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHairColorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HairColorViewModel by viewModels()

    private lateinit var colorAdapter: HairColorSheetAdapter
    private lateinit var colorShimmerAdapter: ShimmerItemFooterAdapter
    private var previewImageUrl: String = ""
    private var hairstyleTitle: String = ""
    private var hairstyleApiValue: String = ""
    private var source: Source = Source.SELECT_CATEGORY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewImageUrl = arguments?.getString(ARG_PREVIEW_URL).orEmpty()
        hairstyleTitle = arguments?.getString(ARG_HAIRSTYLE_TITLE).orEmpty()
        hairstyleApiValue = arguments?.getString(ARG_HAIRSTYLE_API).orEmpty()
        source = arguments?.getString(ARG_SOURCE)?.let { value ->
            Source.entries.firstOrNull { it.name == value }
        } ?: Source.SELECT_CATEGORY
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHairColorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivPreview.load(previewImageUrl) { crossfade(true) }
        binding.btnClose.setOnClickListener { dismiss() }

        colorAdapter = HairColorSheetAdapter { }
        colorShimmerAdapter = ShimmerItemFooterAdapter(
            itemLayoutRes = R.layout.item_shimmer_hair_color_sheet,
            shimmerItemCount = 5,
            initiallyVisible = true
        )
        binding.rvHairColors.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvHairColors.adapter = ConcatAdapter(colorShimmerAdapter, colorAdapter)
        // NestedScrollView + wrap_content RV: force a pass so shimmer placeholders measure.
        binding.rvHairColors.setHasFixedSize(false)
        binding.rvHairColors.isNestedScrollingEnabled = false

        binding.btnNext.setOnClickListener { onNextClicked() }

        viewModel.loadHairColors()
        observeHairColors()
    }

    private fun observeHairColors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val hasItems = state.items.isNotEmpty()
                    val hasError = state.error != null
                    // Keep row shimmer until API returns items (or errors).
                    colorShimmerAdapter.bindPlaceholder(
                        isLoading = state.isLoading || (!hasItems && !hasError),
                        hasItems = hasItems,
                        hasError = hasError
                    )
                    colorAdapter.submitList(state.items) {
                        if (state.items.isNotEmpty() && colorAdapter.getSelected() == null) {
                            colorAdapter.setSelectedId(state.items.first().id)
                        }
                    }
                    state.error?.let { showSnackbar(it) }
                }
            }
        }
    }

    private fun onNextClicked() {
        val selectedColor = colorAdapter.getSelected()
        if (selectedColor == null) {
            showSnackbar(getString(R.string.choose_your_hair_color))
            return
        }

        val styleApiValue = hairstyleApiValue.ifBlank { hairstyleTitle }
        if (styleApiValue.isBlank()) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }
        if (selectedColor.apiValue.isBlank()) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }

        PrefManager.setGenerationType(GenerationType.HAIRSTYLE)
        PrefManager.setSelectedHairstyle(
            title = hairstyleTitle,
            imageUrl = previewImageUrl,
            apiValue = styleApiValue
        )
        PrefManager.setSelectedHairColor(
            title = selectedColor.title,
            imageUrl = selectedColor.imageUrl,
            apiValue = selectedColor.apiValue
        )

        InterstitialNavHelper.showOnClick(this, ScreenInterAds.selectImageNext()) {
            dismiss()
            val host = activity ?: return@showOnClick
            when (source) {
                Source.HOME, Source.SELECT_CATEGORY -> AppNavigator.openPhotoSelect(host)
                Source.PHOTO_FIRST -> AppNavigator.openProgress(host)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener { dialogInterface ->
                val bottomSheet = (dialogInterface as BottomSheetDialog)
                    .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

                bottomSheet.setBackgroundResource(R.drawable.bg_photo_guide_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class Source {
        HOME,
        SELECT_CATEGORY,
        /** Photo already selected (Home → Upload Your Photo flow). */
        PHOTO_FIRST
    }

    companion object {
        const val TAG = "HairColorBottomSheet"
        private const val ARG_PREVIEW_URL = "preview_url"
        private const val ARG_HAIRSTYLE_TITLE = "hairstyle_title"
        private const val ARG_HAIRSTYLE_API = "hairstyle_api"
        private const val ARG_SOURCE = "source"

        fun newInstance(
            previewImageUrl: String,
            hairstyleTitle: String,
            hairstyleApiValue: String,
            source: Source = Source.SELECT_CATEGORY
        ): HairColorBottomSheet {
            return HairColorBottomSheet().apply {
                arguments = bundleOf(
                    ARG_PREVIEW_URL to previewImageUrl,
                    ARG_HAIRSTYLE_TITLE to hairstyleTitle,
                    ARG_HAIRSTYLE_API to hairstyleApiValue,
                    ARG_SOURCE to source.name
                )
            }
        }
    }
}
