package com.hairstyle.generator.aimakerover.ui.gallery

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentPhotoSelectBinding
import com.hairstyle.generator.aimakerover.data.repository.FreeLimitCheckResult
import com.hairstyle.generator.aimakerover.data.repository.FreeLimitRepository
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppManage
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.ui.category.SelectCategoryFragment
import com.hairstyle.generator.aimakerover.ui.premium.PremiumBottomSheet
import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModel
import com.hairstyle.generator.aimakerover.utils.FaceDetectionHelper
import com.hairstyle.generator.aimakerover.utils.FileUtils
import com.hairstyle.generator.aimakerover.utils.ImageCompressor
import com.hairstyle.generator.aimakerover.utils.PermissionHelper
import com.hairstyle.generator.aimakerover.utils.PhotoGuideManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhotoSelectFragment : Fragment() {

    private var _binding: FragmentPhotoSelectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GenerationViewModel by activityViewModels()

    @Inject
    lateinit var freeLimitRepository: FreeLimitRepository

    private var cameraOutputFile: java.io.File? = null
    private var pendingAction: PhotoAction? = null
    private var awaitingSettingsReturn = false
    private var isProcessingPhoto = false
    private var rationaleBeforeRequest = false
    private var permissionRequestStartedAt = 0L

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        handlePermissionResult(PermissionHelper.PermissionType.CAMERA, result)
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        handlePermissionResult(PermissionHelper.PermissionType.STORAGE, result)
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraOutputFile?.let { file ->
                processSelectedImage(FileUtils.getUriForFile(requireContext(), file))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()
        setupClickListeners()
        setupPhotoGuideResultListener()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.addPhoto())
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)
    }

    override fun onResume() {
        super.onResume()
        if (!awaitingSettingsReturn) return
        val action = pendingAction ?: return
        val type = when (action) {
            PhotoAction.CAMERA -> PermissionHelper.PermissionType.CAMERA
            PhotoAction.GALLERY -> PermissionHelper.PermissionType.STORAGE
        }
        if (!PermissionHelper.hasPermissions(this, PermissionHelper.permissionsFor(type))) return

        awaitingSettingsReturn = false
        PermissionHelper.clearDeniedFlag(type)
        onPermissionGranted(action)
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener {
            requestPhotoAction(PhotoAction.CAMERA)
        }

        binding.btnChooseGallery.setOnClickListener {
            requestPhotoAction(PhotoAction.GALLERY)
        }
    }

    private fun setupPhotoGuideResultListener() {
        childFragmentManager.setFragmentResultListener(
            PhotoGuideBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val actionName = bundle.getString(PhotoGuideBottomSheet.ARG_ACTION).orEmpty()
            val action = runCatching { PhotoAction.valueOf(actionName) }
                .getOrElse { pendingAction ?: PhotoAction.CAMERA }
            proceedAfterGuide(action)
        }
    }

    private fun requestPhotoAction(action: PhotoAction) {
        pendingAction = action
        awaitingSettingsReturn = false
        val type = when (action) {
            PhotoAction.CAMERA -> PermissionHelper.PermissionType.CAMERA
            PhotoAction.GALLERY -> PermissionHelper.PermissionType.STORAGE
        }
        val permissions = PermissionHelper.permissionsFor(type)

        when {
            PermissionHelper.hasPermissions(this, permissions) -> onPermissionGranted(action)
            PermissionHelper.isPermanentlyDenied(this, type) ->
                showPermissionDialog(type, openSettings = true)
            else -> launchPermissionRequest(type)
        }
    }

    private fun launchPermissionRequest(type: PermissionHelper.PermissionType) {
        PermissionSettingsDialog.dismissIfShowing()
        val permissions = PermissionHelper.permissionsFor(type)
        rationaleBeforeRequest = PermissionHelper.shouldShowRationale(this, type)
        permissionRequestStartedAt = SystemClock.elapsedRealtime()
        when (type) {
            PermissionHelper.PermissionType.CAMERA -> cameraPermissionLauncher.launch(permissions)
            PermissionHelper.PermissionType.STORAGE -> storagePermissionLauncher.launch(permissions)
            PermissionHelper.PermissionType.NOTIFICATION -> Unit
        }
    }

    private fun handlePermissionResult(
        type: PermissionHelper.PermissionType,
        result: Map<String, Boolean>
    ) {
        val action = pendingAction ?: return
        val outcome = PermissionHelper.evaluateResult(
            fragment = this,
            type = type,
            result = result,
            rationaleBefore = rationaleBeforeRequest,
            elapsedMs = SystemClock.elapsedRealtime() - permissionRequestStartedAt
        )

        if (outcome == PermissionHelper.PermissionOutcome.GRANTED) {
            PermissionHelper.clearDeniedFlag(type)
            onPermissionGranted(action)
            return
        }

        pendingAction = null
        awaitingSettingsReturn = false

        if (outcome == PermissionHelper.PermissionOutcome.PERMANENTLY_DENIED) {
            // Next tap opens the settings dialog; otherwise the system dialog is reused.
            PermissionHelper.markDenied(type)
        } else {
            PermissionHelper.clearDeniedFlag(type)
        }
    }

    private fun showPermissionDialog(
        type: PermissionHelper.PermissionType,
        openSettings: Boolean
    ) {
        if (openSettings) awaitingSettingsReturn = true

        val message = if (openSettings) {
            when (type) {
                PermissionHelper.PermissionType.CAMERA ->
                    getString(R.string.permission_camera_settings_message)
                PermissionHelper.PermissionType.STORAGE ->
                    getString(R.string.permission_storage_settings_message)
                PermissionHelper.PermissionType.NOTIFICATION ->
                    getString(R.string.permission_required_message)
            }
        } else {
            getString(R.string.permission_required_message)
        }

        PermissionSettingsDialog.show(
            fragment = this,
            message = message,
            onAllow = {
                if (openSettings) {
                    PermissionHelper.openAppSettings(requireContext())
                } else {
                    pendingAction = when (type) {
                        PermissionHelper.PermissionType.CAMERA -> PhotoAction.CAMERA
                        PermissionHelper.PermissionType.STORAGE -> PhotoAction.GALLERY
                        PermissionHelper.PermissionType.NOTIFICATION -> null
                    }
                    launchPermissionRequest(type)
                }
            },
            onDismiss = {
                if (!awaitingSettingsReturn) {
                    pendingAction = null
                }
            }
        )
    }

    private fun onPermissionGranted(action: PhotoAction) {
        if (PhotoGuideManager.shouldShowGuide()) {
            pendingAction = action
            showPhotoGuideBottomSheet(action)
        } else {
            proceedAfterGuide(action)
        }
    }

    private fun showPhotoGuideBottomSheet(action: PhotoAction) {
        if (childFragmentManager.findFragmentByTag(PhotoGuideBottomSheet.TAG) != null) return
        PhotoGuideBottomSheet.newInstance(action)
            .show(childFragmentManager, PhotoGuideBottomSheet.TAG)
    }

    private fun proceedAfterGuide(action: PhotoAction) {
        pendingAction = null
        awaitingSettingsReturn = false
        when (action) {
            PhotoAction.CAMERA -> launchCamera()
            PhotoAction.GALLERY -> findNavController().navigate(
                R.id.action_photoSelect_to_galleryPhotoSelection,
                bundleOf(ARG_PHOTO_FIRST_FLOW to isPhotoFirstFlow())
            )
        }
    }

    private fun isPhotoFirstFlow(): Boolean =
        arguments?.getBoolean(ARG_PHOTO_FIRST_FLOW, false) == true

    private fun navigateAfterPhotoReady() {
        if (isPhotoFirstFlow()) {
            findNavController().navigate(
                R.id.action_photoSelect_to_selectCategory,
                bundleOf(SelectCategoryFragment.ARG_PHOTO_ALREADY_SELECTED to true)
            )
        } else {
            findNavController().navigate(R.id.action_photoSelect_to_progress)
        }
    }

    private fun launchCamera() {
        ADSAppManage.skipNextAppOpenAd()
        val file = FileUtils.createCameraImageFile(requireContext())
        cameraOutputFile = file
        val uri = FileUtils.getUriForFile(requireContext(), file)
        takePictureLauncher.launch(uri)
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
                viewModel.setOriginalImage(uri, compressed.absolutePath)
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

    private fun showFreeLimitPremiumSheet() {
        if (childFragmentManager.findFragmentByTag(PremiumBottomSheet.TAG) != null) return
        PremiumBottomSheet.newInstance(
            onCancelClick = {
                findNavController().navigate(R.id.action_photoSelect_to_home)
            },
            onPremiumClick = {
                findNavController().navigate(R.id.action_photoSelect_to_premium)
            }
        ).show(childFragmentManager, PremiumBottomSheet.TAG)
    }

    private fun setPhotoProcessingUi(isProcessing: Boolean) {
        isProcessingPhoto = isProcessing
        val safeBinding = _binding ?: return
        safeBinding.btnTakePhoto.isEnabled = !isProcessing
        safeBinding.btnChooseGallery.isEnabled = !isProcessing
        safeBinding.progressBar.isVisible = isProcessing
        safeBinding.root.alpha = if (isProcessing) 0.85f else 1f
    }

    private fun showFaceDetectionFailedDialog() {
        FaceDetectionFailedDialog.show(this) { action ->
            when (action) {
                FaceDetectionFailedDialog.Action.CHOOSE_ANOTHER -> Unit
                FaceDetectionFailedDialog.Action.TRY_AGAIN -> launchCamera()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PHOTO_FIRST_FLOW = "photo_first_flow"
    }
}
