package com.hairstyle.changer.aitryon.ui.creation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentUserCreationPreviewBinding
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.common.RateAppDialog
import com.hairstyle.changer.aitryon.ui.result.ImageSavedDialog
import com.hairstyle.changer.aitryon.ui.result.SaveHairstyleDialog
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.RewardedAdHelper
import com.hairstyle.changer.aitryon.utils.ResultImageHelper
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import com.hairstyle.changer.aitryon.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserCreationPreviewActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentUserCreationPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserCreationPreviewViewModel by viewModels()

    @Inject lateinit var resultImageHelper: ResultImageHelper

    private var currentImageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentUserCreationPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()

        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        val creationId = intent.getStringExtra(ARG_CREATION_ID).orEmpty()
        if (creationId.isBlank()) {
            finish()
            return
        }

        binding.btnShare.setOnClickListener { shareCurrentImage() }
        binding.btnDownload.setOnClickListener { onDownloadClicked() }
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(creationId)
        }

        viewModel.load(creationId)
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.deleted) {
                        finish()
                        return@collect
                    }
                    val creation = state.creation
                    if (creation == null && !state.isLoading) {
                        showSnackbar(getString(R.string.error_no_result))
                        finish()
                        return@collect
                    }
                    creation?.let {
                        val file = File(it.localPath)
                        currentImageFile = file.takeIf { image -> image.exists() && image.length() > 0L }
                        binding.ivPreview.load(file) {
                            crossfade(true)
                        }
                    }
                }
            }
        }
    }

    /** save_photo_show_ads false → save directly; otherwise Save dialog (watch ad to save). */
    private fun onDownloadClicked() {
        if (resolveImageFile() == null) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }
        if (!ADSMainClass.getBooleanValue(ADSMainClass.SAVE_PHOTO_SHOW_ADS, false)) {
            downloadImage()
            return
        }
        SaveHairstyleDialog.show(
            activity = this,
            mode = SaveHairstyleDialog.Mode.SAVE_HD,
            onFreeAdClick = {
                RewardedAdHelper.showForReward(
                    activity = this,
                    onRewardEarned = { downloadImage() }
                )
            }
        )
    }

    private fun downloadImage() {
        val file = resolveImageFile()
        if (file == null) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }
        lifecycleScope.launch {
            try {
                resultImageHelper.saveToGallery(file, highQuality = true)
                ImageSavedDialog.show(this@UserCreationPreviewActivity) {
                    RateAppDialog.showAfterSave(this@UserCreationPreviewActivity)
                }
            } catch (_: Exception) {
                showSnackbar(getString(R.string.error_no_result))
            }
        }
    }

    private fun showDeleteConfirmationDialog(creationId: String) {
        DeleteCreationDialog.show(this) {
            viewModel.delete(creationId)
        }
    }

    private fun shareCurrentImage() {
        val file = resolveImageFile()
        if (file == null) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }

        lifecycleScope.launch {
            try {
                resultImageHelper.shareImage(file, this@UserCreationPreviewActivity)
            } catch (_: Exception) {
                showSnackbar(getString(R.string.error_no_result))
            }
        }
    }

    private fun resolveImageFile(): File? {
        return currentImageFile?.takeIf { it.exists() && it.length() > 0L }
            ?: viewModel.state.value.creation
                ?.localPath
                ?.let { File(it) }
                ?.takeIf { it.exists() && it.length() > 0L }
    }

    override fun onDestroy() {
        currentImageFile = null
        _binding = null
        super.onDestroy()
    }

    companion object {
        const val ARG_CREATION_ID = "creation_id"

        fun createIntent(context: Context, creationId: String): Intent =
            Intent(context, UserCreationPreviewActivity::class.java).apply {
                putExtra(ARG_CREATION_ID, creationId)
            }
    }
}
