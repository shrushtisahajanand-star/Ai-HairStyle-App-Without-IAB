package com.hairstyle.generator.aimakerover.ui.progress

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentProgressBinding
import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.RewardedAdHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.data.repository.LocalHairstyleRepository
import com.hairstyle.generator.aimakerover.data.repository.UserCreationRepository
import com.hairstyle.generator.aimakerover.domain.model.GenerationType
import com.hairstyle.generator.aimakerover.ui.premium.PremiumPlan
import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModel
import com.hairstyle.generator.aimakerover.utils.DeviceIdProvider
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager.ImageAccessBlockReason
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GenerationViewModel by activityViewModels()

    @Inject lateinit var localHairstyleRepository: LocalHairstyleRepository
    @Inject lateinit var userCreationRepository: UserCreationRepository

    private var progressJob: Job? = null
    private var apiJob: Job? = null
    private var generationStarted = false
    private var navigatedToResult = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()
        binding.cardAreaContainer.doOnLayout { fitSelectedImageToAvailableSpace() }
        generationStarted = savedInstanceState?.getBoolean(KEY_GENERATION_STARTED) ?: false

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.progress())

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            }
        )

        // Close uses normal forward inter count (not back-click counter).
        binding.btnClose.setOnClickListener {
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.progressClose()) {
                findNavController().navigateUp()
            }
        }

        val styleName = PrefManager.getSelectedHairstyleTitle()
            .ifBlank { getString(R.string.selected_style) }
        binding.tvStyleName.text = styleName
        binding.progressBar.progress = 0
        binding.tvPercent.text = getString(R.string.percent_format, 0)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.session.collect { session ->
                    session.originalImageUri?.let { uri ->
                        binding.ivSelectedImage.load(uri) { crossfade(true) }
                    } ?: run {
                        val styleImage = PrefManager.getSelectedHairstyleImage()
                        if (styleImage.isNotBlank()) {
                            binding.ivSelectedImage.load(styleImage) { crossfade(true) }
                        }
                    }
                }
            }
        }

        setupChoiceButtons()

        if (generationStarted) {
            showLoadingState()
            startGenerateFlow()
        } else {
            showChoiceState()
        }
    }

    private fun setupChoiceButtons() {
        // PremiumPlanManager.refreshPremiumQuotaIfNeeded() // OLD image_access quota — disabled
        val isPremium = PremiumPlanManager.isPremiumActive()
        val showAds = PremiumPlanManager.shouldShowAds()
        // TEMP: client image_access disabled — always allow generate until free-limit API is ready.
        val canGenerate = true
        // val canGenerate = PremiumPlanManager.canGenerateImage()
        logQuotaState(isPremium, canGenerate)
        binding.btnFreeAd.isVisible = canGenerate && (showAds || isPremium)
        // Out of quota → keep Upgrade as the only action so the screen is never empty.
        binding.btnUpgradePremium.isVisible = !isPremium // || !canGenerate
        binding.ivAdIcon.isVisible = !isPremium
        binding.tvLimitMessage.isVisible = false
        // binding.tvLimitMessage.isVisible = !canGenerate
        // if (!canGenerate) {
        //     binding.tvLimitMessage.setText(imageLimitMessageRes())
        // }

        binding.tvFreeAdAction.text = if (isPremium && !showAds) {
            getString(R.string.generate_now)
        } else {
            getString(R.string.generate_with_free_ad)
        }

        binding.btnFreeAd.setOnClickListener {
            // OLD client image_access gate — disabled until backend free-limit API is ready.
            // if (!PremiumPlanManager.canGenerateImage() ||
            //     PremiumPlanManager.getRemainingImageAccess() <= 0
            // ) {
            //     showImageLimitToast()
            //     setupChoiceButtons()
            //     return@setOnClickListener
            // }

            // Firebase ads.temporary_skip_generate_api — skip API, open Result with user photo
            if (ADSMainClass.getTemporarySkipGenerateApi()) {
                // if (!PremiumPlanManager.consumeImageAccess()) {
                //     showImageLimitToast()
                //     setupChoiceButtons()
                //     return@setOnClickListener
                // }
                navigateToResultWithoutApi()
                return@setOnClickListener
            }

            // Premium (no ads) → generate directly
            if (isPremium && !showAds) {
                startGenerateAfterReward()
                return@setOnClickListener
            }

            // Free / ads path → must watch Rewarded Ad completely
            RewardedAdHelper.showForReward(
                fragment = this,
                onRewardEarned = { startGenerateAfterReward() },
                onFailedOrSkipped = {
                    generationStarted = false
                    if (isAdded && _binding != null) showChoiceState()
                }
            )
        }

        binding.btnUpgradePremium.setOnClickListener {
            findNavController().navigate(R.id.action_progress_to_premium)
        }
    }

    /** Prints the exact quota inputs so Firebase values can be verified on-device. */
    private fun logQuotaState(isPremium: Boolean, canGenerate: Boolean) {
        val plans = PremiumPlanManager.getActivePlans()
            .joinToString { "${it.planKey}(used=${it.periodImagesUsed})" }
            .ifBlank { "none" }
        Log.d(
            TAG,
            "QUOTA → premium=$isPremium activePlans=[$plans] canGenerate=$canGenerate " +
                "device_id=${DeviceIdProvider.getDeviceId(requireContext().applicationContext)} " +
                "weekPrice=${PremiumPlanManager.getPlan(PremiumPlan.WEEKLY).price} " +
                "monthPrice=${PremiumPlanManager.getPlan(PremiumPlan.MONTHLY).price} " +
                "yearPrice=${PremiumPlanManager.getPlan(PremiumPlan.YEARLY).price} " +
                "freePrice=${PremiumPlanManager.getFreeUserPlan().price} " +
                "freeUsed=${PrefManager.getFreeImagesUsed()} " +
                "remoteDetails=${PremiumPlanManager.hasRemotePlanDetails()}"
        )
        // Log.d(TAG, "QUOTA perDay → ${PremiumPlanManager.quotaBreakdown()}")
        Log.d(TAG, "QUOTA rawPlanDetails = ${PrefManager.getPremiumPlanDetailsJson()}")
    }

    private fun imageLimitMessageRes(): Int {
        return when (PremiumPlanManager.getImageAccessBlockReason()) {
            ImageAccessBlockReason.DAILY_LIMIT -> R.string.premium_daily_limit_reached
            ImageAccessBlockReason.PERIOD_LIMIT -> {
                if (PremiumPlanManager.isPremiumActive()) {
                    R.string.premium_image_limit_reached
                } else {
                    R.string.free_image_limit_reached
                }
            }
            ImageAccessBlockReason.NONE -> R.string.premium_image_limit_reached
        }
    }

    private fun showImageLimitToast() {
        Toast.makeText(requireContext(), getString(imageLimitMessageRes()), Toast.LENGTH_SHORT)
            .show()
    }

    /** TEMPORARY: skip generate API and open Result with the user's photo as mock output. */
    private fun navigateToResultWithoutApi() {
        if (navigatedToResult) return

        val compressedPath = viewModel.session.value.compressedImagePath
        if (compressedPath.isNullOrBlank() || !File(compressedPath).exists()) {
            showSnackbar(getString(R.string.error_no_result))
            return
        }

        navigatedToResult = true
        viewModel.updateResultImagePath(compressedPath)
        viewLifecycleOwner.lifecycleScope.launch {
            persistUserCreation(imageUrl = null, tempLocalPath = compressedPath)
            if (!isAdded) return@launch
            findNavController().navigate(R.id.action_progress_to_result)
        }
    }

    private fun showChoiceState() {
        binding.bottomChoiceSection.isVisible = true
        binding.loadingSection.isVisible = false
        requestFitSelectedImage()
    }

    private fun showLoadingState() {
        binding.bottomChoiceSection.isVisible = false
        binding.loadingSection.isVisible = true
        requestFitSelectedImage()
    }

    private fun requestFitSelectedImage() {
        binding.cardAreaContainer.post { fitSelectedImageToAvailableSpace() }
    }

    /** Keeps image square and tight under SELECTED STYLE; shrinks on short screens / ads. */
    private fun fitSelectedImageToAvailableSpace() {
        val b = _binding ?: return
        val container = b.cardAreaContainer
        if (container.width <= 0 || container.height <= 0) return

        val pad = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        val sideMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
        val preferred = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._190sdp)

        val styleBlock = b.styleInfoContainer.height.let { h ->
            if (h > 0) h + pad else resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._40sdp)
        }
        val maxByHeight = (container.height - pad * 2 - styleBlock).coerceAtLeast(0)
        val maxByWidth = (container.width - sideMargin * 2 - pad * 2).coerceAtLeast(0)
        val side = minOf(preferred, maxByHeight, maxByWidth)
        if (side <= 0) return

        b.ivSelectedImage.updateLayoutParams {
            width = side
            height = side
        }
    }

    private fun startGenerateAfterReward() {
        generationStarted = true
        showLoadingState()
        startGenerateFlow()
    }

    private fun startGenerateFlow() {
        startProgressAnimation()
        callGenerateApi()
    }

    private fun startProgressAnimation() {
        if (progressJob?.isActive == true) return
        binding.progressBar.progress = 0
        binding.tvPercent.text = getString(R.string.percent_format, 0)
        progressJob = viewLifecycleOwner.lifecycleScope.launch {
            for (p in 1..90) {
                delay(50)
                if (_binding == null) return@launch
                if (binding.progressBar.progress >= 100) return@launch
                binding.progressBar.progress = p
                binding.tvPercent.text = getString(R.string.percent_format, p)
            }
        }
    }

    private fun callGenerateApi() {
        if (apiJob?.isActive == true) return
        apiJob = viewLifecycleOwner.lifecycleScope.launch {
            val start = System.currentTimeMillis()
            try {
                val imagePath = viewModel.session.value.compressedImagePath
                val imageFile = imagePath?.let { File(it) }?.takeIf { it.exists() }

                val response = when (PrefManager.getGenerationType()) {
                    GenerationType.BEARD -> {
                        val beardStyle = PrefManager.getSelectedHairstyleApiValue()
                        require(beardStyle.isNotBlank()) { "Selected beard style value missing from API" }

                         Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ START BEARD GENERATE â”€â”€â”€â”€â”€â”€â”€â”€")
                         Log.d(TAG, "beardStyle  = $beardStyle  (display=${PrefManager.getSelectedHairstyleTitle()})")
                         Log.d(TAG, "imagePath   = $imagePath")
                         Log.d(TAG, "imageExists = ${imageFile != null}")

                        localHairstyleRepository.generateBeard(
                            imageFile = imageFile,
                            beardStyle = beardStyle
                        )
                    }
                    GenerationType.HAIRSTYLE -> {
                        val hairStyle = PrefManager.getSelectedHairstyleApiValue()
                        require(hairStyle.isNotBlank()) { "Selected hairstyle value missing from API" }

                        val color = PrefManager.getSelectedHairColorApiValue()
                        require(color.isNotBlank()) { "Selected hair color value missing from API" }

                         Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ START GENERATE â”€â”€â”€â”€â”€â”€â”€â”€")
                         Log.d(TAG, "hairStyle   = $hairStyle  (display=${PrefManager.getSelectedHairstyleTitle()})")
                         Log.d(TAG, "color       = $color  (display=${PrefManager.getSelectedHairColorTitle()})")
                         Log.d(TAG, "imagePath   = $imagePath")
                         Log.d(TAG, "imageExists = ${imageFile != null}")

                        localHairstyleRepository.generate(
                            imageFile = imageFile,
                            hairStyle = hairStyle,
                            color = color
                        )
                    }
                }

                 Log.d(TAG, "apiSuccess  = ${response.success}")
                 Log.d(TAG, "apiImageUrl = ${response.image}")

                require(response.success) {
                    "Generate API returned success=false (${response.failureReason()})"
                }
                require(!response.image.isNullOrBlank()) { "No result image URL in API response" }

                // OLD client image_access hard-stop — disabled until free-limit API is ready.
                // if (!PremiumPlanManager.canGenerateImage() ||
                //     PremiumPlanManager.getRemainingImageAccess() <= 0 ||
                //     !PremiumPlanManager.consumeImageAccess()
                // ) {
                //     throw IllegalStateException(getString(R.string.premium_image_limit_reached))
                // }

                val resultUrl = localHairstyleRepository.resolveResultImageUrl(response.image)
                require(resultUrl.isNotBlank()) { "No result image available" }

                 Log.d(TAG, "resultUrl   = $resultUrl  (shown on Result screen)")

                viewModel.setLocalGenerateResult(
                    resultImageUrl = resultUrl,
                    generationTimeMs = System.currentTimeMillis() - start
                )
                persistUserCreation(
                    imageUrl = resultUrl,
                    tempLocalPath = viewModel.session.value.compressedImagePath
                )
                completeProgressAndGoToResult()
            } catch (e: Exception) {
                 Log.e(TAG, "Generate error: ${e.message}", e)
                progressJob?.cancel()
                if (!isAdded || _binding == null) return@launch

                val limitMessage = getString(R.string.premium_image_limit_reached)
                if (e.message == limitMessage) {
                    showImageLimitToast()
                    generationStarted = false
                    showChoiceState()
                    return@launch
                }

                showGenerateFailedDialog()
            }
        }
    }

    private fun showGenerateFailedDialog() {
        SomethingWentWrongDialog.show(this) {
            navigateToHomeAfterGenerateFail()
        }
    }

    private fun navigateToHomeAfterGenerateFail() {
        if (!isAdded) return
        runCatching {
            findNavController().navigate(R.id.action_progress_to_home)
        }.onFailure {
            findNavController().popBackStack(R.id.homePageFragment, false)
        }
    }

    private suspend fun persistUserCreation(imageUrl: String?, tempLocalPath: String?) {
        runCatching {
            userCreationRepository.saveGeneratedImage(
                imageUrl = imageUrl,
                tempLocalPath = tempLocalPath
            )
        }.onFailure { error ->
             Log.w(TAG, "Failed to save user creation: ${error.message}")
        }
    }

    private suspend fun completeProgressAndGoToResult() {
        progressJob?.cancel()
        if (_binding != null) {
            binding.progressBar.progress = 100
            binding.tvPercent.text = getString(R.string.percent_format, 100)
        }
        delay(300)
        if (!isAdded || navigatedToResult) return
        navigatedToResult = true
        findNavController().navigate(R.id.action_progress_to_result)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_GENERATION_STARTED, generationStarted)
    }

    override fun onDestroyView() {
        progressJob?.cancel()
        apiJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_GENERATION_STARTED = "generation_started"
        private const val TAG = LocalHairstyleRepository.TAG
    }
}
