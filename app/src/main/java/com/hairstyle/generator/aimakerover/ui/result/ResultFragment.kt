package com.hairstyle.generator.aimakerover.ui.result

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentResultBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.RewardedAdHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.domain.model.GenerationSession
import com.hairstyle.generator.aimakerover.ui.common.RateAppDialog
import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModel
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.ResultImageHelper
import com.hairstyle.generator.aimakerover.utils.ResultWatermarkHelper
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GenerationViewModel by activityViewModels()

    @Inject lateinit var resultImageHelper: ResultImageHelper

    private var localResultFile: File? = null
    private var loadedResultUrl: String? = null
    private var imageSavedToGallery = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()
        setupLeaveHandling()
        updateWatermarkVisibility()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.result())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateWatermarkVisibility()
                viewModel.session.collect { session ->
                    bindBeforeAfter(session)
                    loadResultImage(session)
                }
            }
        }

        setActionButtonsEnabled(false)
        binding.btnSaveHd.setOnClickListener {
            onSaveClicked(highQuality = true)
        }
        binding.btnSavePhoto.setOnClickListener {
            onSaveClicked(highQuality = false)
        }
        binding.btnShare.setOnClickListener { shareImage() }
    }

    /** Premium â†’ save directly (no dialog). Free â†’ show Save dialog. */
    private fun onSaveClicked(highQuality: Boolean) {
        if (PremiumPlanManager.isPremiumActive()) {
            saveImage(highQuality = true)
            return
        }
        showSaveDialog(
            if (highQuality) SaveHairstyleDialog.Mode.SAVE_HD
            else SaveHairstyleDialog.Mode.SAVE_PHOTO
        )
    }

    private fun setupLeaveHandling() {
        binding.toolbar.setNavigationOnClickListener { handleLeaveAttempt() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleLeaveAttempt()
                }
            }
        )
    }

    private fun handleLeaveAttempt() {
        if (imageSavedToGallery) {
            InterstitialNavHelper.showOnBack(this) { navigateHome() }
        } else {
            showImageNotSavedBottomSheet()
        }
    }

    private fun showImageNotSavedBottomSheet() {
        if (childFragmentManager.findFragmentByTag(ImageNotSavedBottomSheet.TAG) != null) return

        ImageNotSavedBottomSheet.newInstance(
            onSaveClick = {
                if (PremiumPlanManager.isPremiumActive()) {
                    saveImage(highQuality = true)
                } else {
                    showSaveDialog(SaveHairstyleDialog.Mode.SAVE_PHOTO)
                }
            },
            onLeaveClick = {
                InterstitialNavHelper.showOnClick(this, ScreenInterAds.leaveButton()) {
                    navigateHome()
                }
            }
        ).show(childFragmentManager, ImageNotSavedBottomSheet.TAG)
    }

    private fun navigateHome() {
        findNavController().navigate(R.id.action_result_to_home)
        viewModel.clearSession()
    }

    private fun showSaveDialog(mode: SaveHairstyleDialog.Mode) {
        SaveHairstyleDialog.show(
            fragment = this,
            mode = mode,
            onFreeAdClick = {
                // Save is never blocked by generate quota (incl. 3rd / last free image).
                RewardedAdHelper.showForReward(
                    fragment = this,
                    onRewardEarned = {
                        saveImage(highQuality = mode == SaveHairstyleDialog.Mode.SAVE_HD)
                    },
                    onFailedOrSkipped = { /* stay on result â€” no save */ }
                )
            },
            onPremiumClick = { findNavController().navigate(R.id.action_result_to_premium) }
        )
    }

    private var beforeAfterBound = false

    private fun bindBeforeAfter(session: GenerationSession) {
        if (beforeAfterBound) return

        val beforePath = session.compressedImagePath ?: return
        val afterPath = session.resultImagePath
        val afterUrl = session.resultImageUrl

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val beforeBitmap = resultImageHelper.decodeResultBitmap(File(beforePath)) ?: return@launch
                val afterFile = when {
                    !afterPath.isNullOrBlank() -> File(afterPath)
                    !afterUrl.isNullOrBlank() -> resultImageHelper.ensureLocalFile(afterUrl, null)
                    else -> return@launch
                }
                val afterBitmap = resultImageHelper.decodeResultBitmap(afterFile) ?: return@launch

                binding.beforeAfterView.setImages(beforeBitmap, afterBitmap)
                beforeAfterBound = true
                setActionButtonsEnabled(true)
            } catch (e: Exception) {
                // Log.e(TAG, "Before/After bind failed", e)
            }
        }
    }

    private fun loadResultImage(session: GenerationSession) {
        val resultUrl = session.resultImageUrl?.takeIf { it.isNotBlank() } ?: return
        if (loadedResultUrl == resultUrl) return
        loadedResultUrl = resultUrl

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resultFile = resultImageHelper.ensureLocalFile(
                    imageUrl = session.resultImageUrl,
                    existingPath = session.resultImagePath
                )
                localResultFile = resultFile
                if (session.resultImagePath != resultFile.absolutePath) {
                    viewModel.updateResultImagePath(resultFile.absolutePath)
                }
                beforeAfterBound = false
                bindBeforeAfter(viewModel.session.value)
            } catch (e: Exception) {
                // Log.e(TAG, "Failed to load result image", e)
                showSnackbar(e.message ?: getString(R.string.error_no_result))
            }
        }
    }

    private fun updateWatermarkVisibility() {
        if (_binding == null) return
        binding.watermarkBadge.isVisible = ResultWatermarkHelper.shouldShowWatermark()
    }

    private fun setActionButtonsEnabled(enabled: Boolean) {
        binding.btnSaveHd.isEnabled = enabled
        binding.btnSavePhoto.isEnabled = enabled
        binding.btnShare.isEnabled = enabled
    }

    private fun saveImage(highQuality: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = getResultFileOrThrow()
                resultImageHelper.saveToGallery(file, highQuality)
                imageSavedToGallery = true
                Toast.makeText(
                    requireContext(),
                    R.string.saved_successfully,
                    Toast.LENGTH_SHORT
                ).show()
                RateAppDialog.showAfterSave(this@ResultFragment)
            } catch (e: Exception) {
                showImageNotSavedBottomSheet()
            }
        }
    }

    private fun shareImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = getResultFileOrThrow()
                resultImageHelper.shareImage(file, requireContext())
            } catch (e: Exception) {
                showSnackbar(e.message ?: getString(R.string.error_no_result))
            }
        }
    }

    private suspend fun getResultFileOrThrow(): File {
        localResultFile?.takeIf { it.exists() && it.length() > 0L }?.let { return it }

        val session = viewModel.session.value
        val file = resultImageHelper.ensureLocalFile(
            imageUrl = session.resultImageUrl,
            existingPath = session.resultImagePath
        )
        localResultFile = file
        viewModel.updateResultImagePath(file.absolutePath)
        return file
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadedResultUrl = null
        localResultFile = null
        beforeAfterBound = false
        imageSavedToGallery = false
        _binding = null
    }

    companion object {
        private const val TAG = "ResultFragment"
    }
}
