package com.hairstyle.generator.aimakerover.ui.gallery

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentGalleryPhotoSelectionBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.data.repository.FreeLimitCheckResult
import com.hairstyle.generator.aimakerover.data.repository.FreeLimitRepository
import com.hairstyle.generator.aimakerover.ui.category.GridSpacingItemDecoration
import com.hairstyle.generator.aimakerover.ui.category.SelectCategoryFragment
import com.hairstyle.generator.aimakerover.ui.premium.PremiumBottomSheet
import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModel
import com.hairstyle.generator.aimakerover.utils.FaceDetectionHelper
import com.hairstyle.generator.aimakerover.utils.FileUtils
import com.hairstyle.generator.aimakerover.utils.ImageCompressor
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GalleryPhotoSelectionFragment : Fragment() {

    private var _binding: FragmentGalleryPhotoSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryPhotoSelectionViewModel by viewModels()
    private val generationViewModel: GenerationViewModel by activityViewModels()

    @Inject
    lateinit var freeLimitRepository: FreeLimitRepository

    private lateinit var photoAdapter: GalleryPhotoAdapter
    private var isProcessingPhoto = false

    /** Swallows back while face detection runs, so the view can't be destroyed mid-flow. */
    private val blockBackWhileProcessing = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryPhotoSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.galleryPhotos())

        photoAdapter = GalleryPhotoAdapter { photo ->
            viewModel.selectPhoto(photo.id)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        binding.rvGalleryPhotos.layoutManager = gridLayoutManager
        val spacingPx = (4 * resources.displayMetrics.density).toInt()
        if (binding.rvGalleryPhotos.itemDecorationCount == 0) {
            binding.rvGalleryPhotos.addItemDecoration(
                GridSpacingItemDecoration(GRID_SPAN_COUNT, spacingPx)
            )
        }
        binding.rvGalleryPhotos.adapter = photoAdapter

        binding.btnBack.setOnClickListener {
            if (isProcessingPhoto) return@setOnClickListener
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)
        // Registered after the back-inter callback so it wins while processing.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            blockBackWhileProcessing
        )

        binding.albumSelector.setOnClickListener { showAlbumMenu() }
        binding.btnDone.setOnClickListener { onDoneClicked() }

        observeState()
        viewModel.loadGallery()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.tvAlbumName.text = state.selectedAlbumName
                    photoAdapter.setSelectedPhotoId(state.selectedPhotoId)
                    photoAdapter.submitList(state.photos)
                    binding.btnDone.isEnabled = !isProcessingPhoto && state.selectedPhotoId != null
                    state.error?.let { showSnackbar(it) }
                }
            }
        }
    }

    private fun showAlbumMenu() {
        if (isProcessingPhoto) return
        val state = viewModel.state.value
        if (state.albums.isEmpty()) return

        GalleryAlbumDropdown.show(
            anchor = binding.albumSelector,
            albums = state.albums,
            selectedAlbumId = state.selectedAlbumId,
            onAlbumSelected = { album -> viewModel.selectAlbum(album.id) }
        )
    }

    private fun onDoneClicked() {
        if (isProcessingPhoto) return
        val selected = viewModel.state.value.selectedPhoto ?: return
        processSelectedImage(selected.uri)
    }

    private fun processSelectedImage(uri: Uri) {
        if (isProcessingPhoto) return
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            setPhotoProcessingUi(true)
            try {
                when (freeLimitRepository.checkBeforeFaceDetection()) {
                    FreeLimitCheckResult.LimitReached -> {
                        if (isAdded) showFreeLimitPremiumSheet()
                        return@launch
                    }
                    FreeLimitCheckResult.Allowed -> Unit
                }
                if (!FaceDetectionHelper.hasDetectableFace(appContext, uri)) {
                    if (isAdded) showFaceDetectionFailedDialog()
                    return@launch
                }
                val compressed = ImageCompressor.compress(
                    appContext,
                    uri,
                    FileUtils.getCacheDir(appContext, "compressed")
                )
                if (!isAdded || _binding == null) return@launch
                generationViewModel.setOriginalImage(uri, compressed.absolutePath)
                navigateAfterPhotoReady()
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    showSnackbar(e.message ?: getString(R.string.error_image_processing))
                }
            } finally {
                // Runs on cancellation too — the view may already be gone.
                setPhotoProcessingUi(false)
            }
        }
    }

    private fun setPhotoProcessingUi(isProcessing: Boolean) {
        isProcessingPhoto = isProcessing
        blockBackWhileProcessing.isEnabled = isProcessing
        val safeBinding = _binding ?: return
        safeBinding.processingOverlay.isVisible = isProcessing
        safeBinding.btnDone.isEnabled = !isProcessing && viewModel.state.value.selectedPhotoId != null
    }

    private fun isPhotoFirstFlow(): Boolean =
        arguments?.getBoolean(PhotoSelectFragment.ARG_PHOTO_FIRST_FLOW, false) == true

    private fun navigateAfterPhotoReady() {
        if (isPhotoFirstFlow()) {
            findNavController().navigate(
                R.id.action_galleryPhotoSelection_to_selectCategory,
                bundleOf(SelectCategoryFragment.ARG_PHOTO_ALREADY_SELECTED to true)
            )
        } else {
            findNavController().navigate(R.id.action_galleryPhotoSelection_to_progress)
        }
    }

    private fun showFreeLimitPremiumSheet() {
        if (childFragmentManager.findFragmentByTag(PremiumBottomSheet.TAG) != null) return
        PremiumBottomSheet.newInstance(
            onCancelClick = {
                findNavController().navigate(R.id.action_galleryPhotoSelection_to_home)
            },
            onPremiumClick = {
                findNavController().navigate(R.id.action_galleryPhotoSelection_to_premium)
            }
        ).show(childFragmentManager, PremiumBottomSheet.TAG)
    }

    private fun showFaceDetectionFailedDialog() {
        viewModel.clearPhotoSelection()
        FaceDetectionFailedDialog.show(this) { action ->
            if (action == FaceDetectionFailedDialog.Action.TRY_AGAIN) {
                viewModel.clearPhotoSelection()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
    }
}
