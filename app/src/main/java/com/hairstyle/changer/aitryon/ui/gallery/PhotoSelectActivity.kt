package com.hairstyle.changer.aitryon.ui.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentPhotoSelectBinding
import com.hairstyle.changer.aitryon.repository.FreeLimitCheckResult
import com.hairstyle.changer.aitryon.repository.FreeLimitRepository
import com.hairstyle.changer.aitryon.Ads.ADSAppManage
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.common.DailyQuotaFinishedDialog
import com.hairstyle.changer.aitryon.ui.premium.PremiumBottomSheet
import com.hairstyle.changer.aitryon.ui.viewmodel.GenerationViewModel
import com.hairstyle.changer.aitryon.ui.viewmodel.GenerationViewModelOwner
import com.hairstyle.changer.aitryon.utils.FaceDetectionHelper
import com.hairstyle.changer.aitryon.utils.FileUtils
import com.hairstyle.changer.aitryon.utils.ImageCompressor
import com.hairstyle.changer.aitryon.utils.PermissionHelper
import com.hairstyle.changer.aitryon.utils.PhotoGuideManager
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import com.hairstyle.changer.aitryon.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhotoSelectActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentPhotoSelectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GenerationViewModel by sharedViewModels({ GenerationViewModelOwner })

    @Inject
    lateinit var freeLimitRepository: FreeLimitRepository

    private var cameraOutputFile: java.io.File? = null
    private var pendingAction: PhotoAction? = null
    private var awaitingSettingsReturn = false
    private var isProcessingPhoto = false
    private var rationaleBeforeRequest = false
    private var permissionRequestStartedAt = 0L
    /** Last source used to pick a photo — drives Face Detection retry actions. */
    private var lastPhotoSource: PhotoAction = PhotoAction.GALLERY

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        handlePermissionResult(PermissionHelper.PermissionType.CAMERA, result)
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processSelectedImage(uri)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraOutputFile?.let { file ->
                processSelectedImage(FileUtils.getUriForFile(this, file))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentPhotoSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

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
        if (pendingAction != PhotoAction.CAMERA) return
        if (!PermissionHelper.hasPermissions(
                this,
                PermissionHelper.permissionsFor(PermissionHelper.PermissionType.CAMERA)
            )
        ) {
            return
        }

        awaitingSettingsReturn = false
        PermissionHelper.clearDeniedFlag(PermissionHelper.PermissionType.CAMERA)
        onPermissionGranted(PhotoAction.CAMERA)
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
        supportFragmentManager.setFragmentResultListener(
            PhotoGuideBottomSheet.RESULT_KEY,
            this
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
        when (action) {
            // System Photo Picker — no storage permission required.
            PhotoAction.GALLERY -> onPermissionGranted(action)
            PhotoAction.CAMERA -> requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        val type = PermissionHelper.PermissionType.CAMERA
        val permissions = PermissionHelper.permissionsFor(type)
        when {
            PermissionHelper.hasPermissions(this, permissions) -> onPermissionGranted(PhotoAction.CAMERA)
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
            PermissionHelper.PermissionType.STORAGE -> Unit // system Photo Picker — no storage permission
            PermissionHelper.PermissionType.NOTIFICATION -> Unit
        }
    }

    private fun handlePermissionResult(
        type: PermissionHelper.PermissionType,
        result: Map<String, Boolean>
    ) {
        val action = pendingAction ?: return
        val outcome = PermissionHelper.evaluateResult(
            this,
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
            getString(R.string.permission_camera_settings_message)
        } else {
            getString(R.string.permission_required_message)
        }

        PermissionSettingsDialog.show(
            this,
            message = message,
            onAllow = {
                if (openSettings) {
                    PermissionHelper.openAppSettings(this)
                } else {
                    pendingAction = PhotoAction.CAMERA
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
        if (supportFragmentManager.findFragmentByTag(PhotoGuideBottomSheet.TAG) != null) return
        PhotoGuideBottomSheet.newInstance(action)
            .show(supportFragmentManager, PhotoGuideBottomSheet.TAG)
    }

    private fun proceedAfterGuide(action: PhotoAction) {
        pendingAction = null
        awaitingSettingsReturn = false
        when (action) {
            PhotoAction.CAMERA -> launchCamera()
            PhotoAction.GALLERY -> launchGalleryPicker()
        }
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

    private fun launchGalleryPicker() {
        lastPhotoSource = PhotoAction.GALLERY
        ADSAppManage.skipNextAppOpenAd()
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun launchCamera() {
        lastPhotoSource = PhotoAction.CAMERA
        ADSAppManage.skipNextAppOpenAd()
        val file = FileUtils.createCameraImageFile(this)
        cameraOutputFile = file
        val uri = FileUtils.getUriForFile(this, file)
        takePictureLauncher.launch(uri)
    }

    private fun processSelectedImage(uri: Uri) {
        if (isProcessingPhoto) return
        val appContext = applicationContext
        lifecycleScope.launch {
            setPhotoProcessingUi(true)
            try {
                when (val limitResult = freeLimitRepository.checkBeforeFaceDetection()) {
                    FreeLimitCheckResult.Allowed -> Unit
                    FreeLimitCheckResult.FreeLimitReached,
                    FreeLimitCheckResult.PremiumExpired -> {
                        if (isScreenAlive()) {
                            if (limitResult is FreeLimitCheckResult.PremiumExpired) {
                                Toast.makeText(
                                    this@PhotoSelectActivity,
                                    getString(R.string.premium_subscription_expired),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showFreeLimitPremiumSheet()
                        }
                        return@launch
                    }
                    FreeLimitCheckResult.DailyLimitReached -> {
                        if (isScreenAlive()) DailyQuotaFinishedDialog.show(this@PhotoSelectActivity)
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
                viewModel.setOriginalImage(uri, compressed.absolutePath)
                navigateAfterPhotoReady()
            } catch (e: Exception) {
                if (isScreenAlive() && _binding != null) {
                    showSnackbar(e.message ?: getString(R.string.error_image_processing))
                }
            } finally {
                setPhotoProcessingUi(false)
            }
        }
    }

    private fun showFreeLimitPremiumSheet() {
        if (supportFragmentManager.findFragmentByTag(PremiumBottomSheet.TAG) != null) return
        PremiumBottomSheet.newInstance(
            onCancelClick = {
                AppNavigator.openHomeClearTask(this)
            },
            onPremiumClick = {
                AppNavigator.openPremium(this)
            }
        ).show(supportFragmentManager, PremiumBottomSheet.TAG)
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
                FaceDetectionFailedDialog.Action.CHOOSE_ANOTHER -> launchGalleryPicker()
                FaceDetectionFailedDialog.Action.TRY_AGAIN -> retryLastPhotoSource()
            }
        }
    }

    private fun retryLastPhotoSource() {
        when (lastPhotoSource) {
            PhotoAction.GALLERY -> launchGalleryPicker()
            PhotoAction.CAMERA -> launchCamera()
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        const val ARG_PHOTO_FIRST_FLOW = "photo_first_flow"

        fun createIntent(context: Context, photoFirstFlow: Boolean = false): Intent =
            Intent(context, PhotoSelectActivity::class.java).apply {
                putExtra(ARG_PHOTO_FIRST_FLOW, photoFirstFlow)
            }
    }
}
