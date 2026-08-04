package com.hairstyle.generator.aimakerover.ui.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentUserCreationPreviewBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.ui.common.RateAppDialog
import com.hairstyle.generator.aimakerover.ui.result.SaveHairstyleDialog
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.ResultImageHelper
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserCreationPreviewFragment : Fragment() {

    private var _binding: FragmentUserCreationPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserCreationPreviewViewModel by viewModels()

    @Inject lateinit var resultImageHelper: ResultImageHelper

    private var currentImageFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserCreationPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()

        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        val creationId = arguments?.getString(ARG_CREATION_ID).orEmpty()
        if (creationId.isBlank()) {
            findNavController().navigateUp()
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.deleted) {
                        findNavController().navigateUp()
                        return@collect
                    }
                    val creation = state.creation
                    if (creation == null && !state.isLoading) {
                        showSnackbar(getString(R.string.error_no_result))
                        findNavController().navigateUp()
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

    /** Premium → save directly. Free → Save dialog with Upgrade to Premium. */
    private fun onDownloadClicked() {
        if (resolveImageFile() == null) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }
        if (PremiumPlanManager.isPremiumActive()) {
            downloadImage()
            return
        }
        SaveHairstyleDialog.show(
            fragment = this,
            mode = SaveHairstyleDialog.Mode.SAVE_HD,
            onPremiumClick = {
                findNavController().navigate(R.id.action_userCreationPreview_to_premium)
            }
        )
    }

    private fun downloadImage() {
        val file = resolveImageFile()
        if (file == null) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                resultImageHelper.saveToGallery(file, highQuality = true)
                Toast.makeText(
                    requireContext(),
                    R.string.saved_successfully,
                    Toast.LENGTH_SHORT
                ).show()
                RateAppDialog.showAfterSave(this@UserCreationPreviewFragment)
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                resultImageHelper.shareImage(file, requireContext())
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

    override fun onDestroyView() {
        super.onDestroyView()
        currentImageFile = null
        _binding = null
    }

    companion object {
        const val ARG_CREATION_ID = "creation_id"
    }
}
