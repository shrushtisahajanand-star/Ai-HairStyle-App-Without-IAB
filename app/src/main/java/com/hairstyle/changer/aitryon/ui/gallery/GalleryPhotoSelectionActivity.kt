package com.hairstyle.changer.aitryon.ui.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentGalleryPhotoSelectionBinding
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.repository.FreeLimitCheckResult
import com.hairstyle.changer.aitryon.repository.FreeLimitRepository
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.category.GridSpacingItemDecoration
import com.hairstyle.changer.aitryon.ui.common.DailyQuotaFinishedDialog
import com.hairstyle.changer.aitryon.ui.viewmodel.GenerationViewModel
import com.hairstyle.changer.aitryon.ui.viewmodel.GenerationViewModelOwner
import com.hairstyle.changer.aitryon.utils.FaceDetectionHelper
import com.hairstyle.changer.aitryon.utils.FileUtils
import com.hairstyle.changer.aitryon.utils.ImageCompressor
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import com.hairstyle.changer.aitryon.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GalleryPhotoSelectionActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentGalleryPhotoSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryPhotoSelectionViewModel by viewModels()
    private val generationViewModel: GenerationViewModel by sharedViewModels({ GenerationViewModelOwner })

    @Inject
    lateinit var freeLimitRepository: FreeLimitRepository

    private lateinit var photoAdapter: GalleryPhotoAdapter
    private var isProcessingPhoto = false

    /** Swallows back while face detection runs, so the view can't be destroyed mid-flow. */
    private val blockBackWhileProcessing = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentGalleryPhotoSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.galleryPhotos())

        photoAdapter = GalleryPhotoAdapter { photo ->
            viewModel.selectPhoto(photo.id)
        }

        val gridLayoutManager = GridLayoutManager(this, GRID_SPAN_COUNT)
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
        onBackPressedDispatcher.addCallback(
            this,
            blockBackWhileProcessing
        )

        binding.albumSelector.setOnClickListener { showAlbumMenu() }
        binding.btnDone.setOnClickListener { onDoneClicked() }

        observeState()
        viewModel.loadGallery()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        val appContext = applicationContext
        lifecycleScope.launch {
            setPhotoProcessingUi(true)
            try {
                when (freeLimitRepository.checkBeforeFaceDetection()) {
                    FreeLimitCheckResult.Allowed -> Unit
                    FreeLimitCheckResult.DailyLimitReached -> {
                        if (isScreenAlive()) DailyQuotaFinishedDialog.show(this@GalleryPhotoSelectionActivity)
                        return@launch
                    }
                }
                if (!FaceDetectionHelper.hasDetectableFace(appContext, uri)) {
                    if (isScreenAlive()) showFaceDetectionFailedDialog()
                    return@launch
                }
                val compressed = ImageCompressor.compress(
                    appContext,
                    uri,
                    FileUtils.getCacheDir(appContext, "compressed")
                )
                if (!isScreenAlive() || _binding == null) return@launch
                generationViewModel.setOriginalImage(uri, compressed.absolutePath)
                navigateAfterPhotoReady()
            } catch (e: Exception) {
                if (isScreenAlive() && _binding != null) {
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
        intent.getBooleanExtra(ARG_PHOTO_FIRST_FLOW, false)

    private fun navigateAfterPhotoReady() {
        if (isPhotoFirstFlow()) {
            AppNavigator.openSelectCategory(this, photoAlreadySelected = true)
        } else {
            AppNavigator.openProgress(this)
        }
    }

    private fun showFaceDetectionFailedDialog() {
        viewModel.clearPhotoSelection()
        FaceDetectionFailedDialog.show(this) { action ->
            if (action == FaceDetectionFailedDialog.Action.TRY_AGAIN) {
                viewModel.clearPhotoSelection()
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
        const val ARG_PHOTO_FIRST_FLOW = "photo_first_flow"

        fun createIntent(context: Context, photoFirstFlow: Boolean = false): Intent =
            Intent(context, GalleryPhotoSelectionActivity::class.java).apply {
                putExtra(ARG_PHOTO_FIRST_FLOW, photoFirstFlow)
            }
    }
}
