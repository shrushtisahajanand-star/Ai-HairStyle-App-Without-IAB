package com.hairstyle.changer.aitryon.ui.result

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentResultBinding
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.RewardedAdHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.domain.model.GenerationSession
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.common.RateAppDialog
import com.hairstyle.changer.aitryon.ui.viewmodel.GenerationViewModel
import com.hairstyle.changer.aitryon.ui.viewmodel.GenerationViewModelOwner
import com.hairstyle.changer.aitryon.utils.ResultImageHelper
import com.hairstyle.changer.aitryon.utils.ResultWatermarkHelper
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import com.hairstyle.changer.aitryon.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ResultActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GenerationViewModel by sharedViewModels({ GenerationViewModelOwner })

    @Inject lateinit var resultImageHelper: ResultImageHelper

    private var localResultFile: File? = null
    private var loadedResultUrl: String? = null
    private var imageSavedToGallery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        setupLeaveHandling()
        updateWatermarkVisibility()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.result())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    /** save_photo_show_ads false → save directly; otherwise show the Save dialog (watch ad to save). */
    private fun onSaveClicked(highQuality: Boolean) {
        if (!shouldShowSavePhotoAds()) {
            saveImage(highQuality = highQuality)
            return
        }
        showSaveDialog(
            if (highQuality) SaveHairstyleDialog.Mode.SAVE_HD
            else SaveHairstyleDialog.Mode.SAVE_PHOTO
        )
    }

    private fun shouldShowSavePhotoAds(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.SAVE_PHOTO_SHOW_ADS, false)

    private fun setupLeaveHandling() {
        binding.toolbar.setNavigationOnClickListener { handleLeaveAttempt() }

        onBackPressedDispatcher.addCallback(
            this,
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
        if (supportFragmentManager.findFragmentByTag(ImageNotSavedBottomSheet.TAG) != null) return

        ImageNotSavedBottomSheet.newInstance(
            onSaveClick = {
                if (!shouldShowSavePhotoAds()) {
                    saveImage(highQuality = false)
                } else {
                    showSaveDialog(SaveHairstyleDialog.Mode.SAVE_PHOTO)
                }
            },
            onLeaveClick = {
                InterstitialNavHelper.showOnClick(this, ScreenInterAds.leaveButton()) {
                    navigateHome()
                }
            }
        ).show(supportFragmentManager, ImageNotSavedBottomSheet.TAG)
    }

    private fun navigateHome() {
        viewModel.clearSession()
        AppNavigator.openHomeClearTask(this)
    }

    private fun showSaveDialog(mode: SaveHairstyleDialog.Mode) {
        SaveHairstyleDialog.show(
            activity = this,
            mode = mode,
            onFreeAdClick = {
                // Save is never blocked by generate quota (incl. 3rd / last free image).
                RewardedAdHelper.showForReward(
                    activity = this,
                    onRewardEarned = {
                        saveImage(highQuality = mode == SaveHairstyleDialog.Mode.SAVE_HD)
                    },
                    onFailedOrSkipped = { /* stay on result — no save */ }
                )
            }
        )
    }

    private var beforeAfterBound = false

    private fun bindBeforeAfter(session: GenerationSession) {
        if (beforeAfterBound) return

        val beforePath = session.compressedImagePath ?: return
        val afterPath = session.resultImagePath
        val afterUrl = session.resultImageUrl

        lifecycleScope.launch {
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

        lifecycleScope.launch {
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
                showSnackbar(e.message ?: getString(R.string.error_no_result))
            }
        }
    }

    private fun updateWatermarkVisibility() {
        if (_binding == null) return
        binding.watermarkBadge.isVisible = true
    }

    private fun setActionButtonsEnabled(enabled: Boolean) {
        binding.btnSaveHd.isEnabled = enabled
        binding.btnSavePhoto.isEnabled = enabled
        binding.btnShare.isEnabled = enabled
    }

    private fun saveImage(highQuality: Boolean) {
        lifecycleScope.launch {
            try {
                val file = getResultFileOrThrow()
                resultImageHelper.saveToGallery(file, highQuality)
                imageSavedToGallery = true
                Toast.makeText(
                    this@ResultActivity,
                    R.string.saved_successfully,
                    Toast.LENGTH_SHORT
                ).show()
                RateAppDialog.showAfterSave(this@ResultActivity)
            } catch (e: Exception) {
                showImageNotSavedBottomSheet()
            }
        }
    }

    private fun shareImage() {
        lifecycleScope.launch {
            try {
                val file = getResultFileOrThrow()
                resultImageHelper.shareImage(file, this@ResultActivity)
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

    override fun onDestroy() {
        loadedResultUrl = null
        localResultFile = null
        beforeAfterBound = false
        imageSavedToGallery = false
        _binding = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ResultActivity"
    }
}
