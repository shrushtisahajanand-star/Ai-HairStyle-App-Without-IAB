package com.hairstyle.generator.aimakerover.ui.premium

import android.content.Context
import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentPremiumBinding
import com.hairstyle.generator.aimakerover.billing.BillingUiState
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.data.repository.VideosRepository
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import com.hairstyle.generator.aimakerover.ui.intro.completeIntroAndGoHome
import com.hairstyle.generator.aimakerover.utils.AppLinkHelper
import com.hairstyle.generator.aimakerover.utils.IntroVideoHelper
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.VideoTitleKeys
import com.hairstyle.generator.aimakerover.utils.applyStatusBarTopMargin
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PremiumActivity : BaseActivity() {

    override val useLightStatusBarIcons = false

    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PremiumViewModel by viewModels()

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var videosRepository: VideosRepository

    private var selectedPlan: PremiumPlan = PremiumPlan.YEARLY
    private var fromIntro: Boolean = false
    private var videoPrepared = false
    private var continueButtonAnimator: PremiumContinueButtonAnimator? = null
    private var closeRevealJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromIntro = intent.getBooleanExtra(ARG_FROM_INTRO, false)

        _binding = FragmentPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        // Onboarding: if premium already restored/active, skip paywall → Home.
        if (fromIntro && PremiumPlanManager.isPremiumActive()) {
            PremiumPlanManager.syncAdsFreeFlag()
            completeIntroAndGoHome()
            return
        }

        binding.btnClose.applyStatusBarTopMargin()
        binding.btnClose.setImageResource(R.drawable.ic_close_icon)
        if (fromIntro) {
            // First-time onboarding paywall — reveal Close after delay.
            binding.btnClose.visibility = View.INVISIBLE
            scheduleCloseButtonReveal()
        } else {
            binding.btnClose.visibility = View.VISIBLE
        }
        applyTitleGradient()
        applyFooterUnderline()
        binding.tvFooter.setOnClickListener {
            AppLinkHelper.openPrivacyPolicy(this)
        }
        binding.termsAndConditions.setOnClickListener {
            AppLinkHelper.openTermsOfService(this)
        }
        applyUpgradePlanVisibility()
        updatePlanSelection(selectedPlan)
        observeBillingState()
        observeStorePrices()
        loadPremiumPlansAndRefresh()

        binding.btnClose.contentDescription = getString(
            if (fromIntro) R.string.next else R.string.close
        )
        binding.btnClose.setOnClickListener {
            if (fromIntro) {
                completeIntroAndGoHome()
            } else {
                finish()
            }
        }
        // System / gesture back disabled — leave only via Close button.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            }
        )

        binding.cardYearly.setOnClickListener { updatePlanSelection(PremiumPlan.YEARLY) }
        binding.cardMonthly.setOnClickListener { updatePlanSelection(PremiumPlan.MONTHLY) }
        binding.cardWeekly.setOnClickListener { updatePlanSelection(PremiumPlan.WEEKLY) }
        binding.btnContinue.setOnClickListener {
//            Log.d("TAG", "onViewCreated: " + selectedPlan)
            viewModel.purchase(this, selectedPlan)
        }
        binding.tvRestore.setOnClickListener {
            viewModel.restorePurchases()
        }

        continueButtonAnimator = PremiumContinueButtonAnimator(
            arrow = binding.ivContinueArrow,
            glowBorder = binding.continueGlowBorder
        ).also { it.start() }

        loadPremiumVideo()
    }

    private fun scheduleCloseButtonReveal() {
        closeRevealJob?.cancel()
        closeRevealJob = lifecycleScope.launch {
            delay(CLOSE_BUTTON_REVEAL_DELAY_MS)
            if (_binding == null || !isScreenAlive()) return@launch
            binding.btnClose.visibility = View.VISIBLE
        }
    }

    private fun loadPremiumVideo() {
        playPremiumVideo()
        lifecycleScope.launch(Dispatchers.IO) {
            videosRepository.ensureCached(VideoTitleKeys.PREMIUM_SCREEN)
            withContext(Dispatchers.Main) {
                if (!isScreenAlive()) return@withContext
                if (!videoPrepared) playPremiumVideo()
            }
        }
    }

    private fun playPremiumVideo() {
        if (videoPrepared) {
            IntroVideoHelper.resume(binding.ivPremiumVideo)
            return
        }

        IntroVideoHelper.play(
            webpView = binding.ivPremiumVideo,
            shimmerLayout = binding.shimmerPremiumVideo,
            videoTitleKey = VideoTitleKeys.PREMIUM_SCREEN,
            fallbackImageRes = R.drawable.home_image,
            onPrepared = { _, _ -> videoPrepared = true }
        )
    }

    private fun observeBillingState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.billingState.collect { state ->
                    when (state) {
                        BillingUiState.Idle -> setPurchaseLoading(false)
                        BillingUiState.Loading -> setPurchaseLoading(true)
                        is BillingUiState.Success -> {
                            setPurchaseLoading(false)
                            PremiumPlanManager.syncAdsFreeFlag()
                            showSnackbar(getString(R.string.billing_purchase_success))
                            navigateAfterPurchase()
                            viewModel.resetBillingState()
                        }
                        BillingUiState.Restored -> {
                            setPurchaseLoading(false)
                            PremiumPlanManager.syncAdsFreeFlag()
                            applyUpgradePlanVisibility()
                            updatePlanSelection(selectedPlan)
                            showSnackbar(getString(R.string.billing_restore_success))
                            navigateAfterPurchase()
                            viewModel.resetBillingState()
                        }
                        BillingUiState.NothingToRestore -> {
                            setPurchaseLoading(false)
                            showSnackbar(getString(R.string.billing_nothing_to_restore))
                            viewModel.resetBillingState()
                        }
                        BillingUiState.Cancelled -> {
                            setPurchaseLoading(false)
                            viewModel.resetBillingState()
                        }
                        is BillingUiState.Error -> {
                            setPurchaseLoading(false)
                            showSnackbar(resolveBillingError(state))
                            viewModel.resetBillingState()
                        }
                    }
                }
            }
        }
    }

    private fun loadPremiumPlansAndRefresh() {
        lifecycleScope.launch {
            if (!PremiumPlanManager.hasRemotePlanDetails()) {
                runCatching { remoteConfigManager.fetchAndSaveAll() }
            }
            refreshAllPlanPrices()
            updateSelectedPlanSummary()
        }
    }

    private fun observeStorePrices() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productsLoaded.collect {
                    if (it > 0) {
                        refreshAllPlanPrices()
                        updateSelectedPlanSummary()
                    }
                }
            }
        }
    }

    private fun refreshAllPlanPrices() {
        bindPlanCard(PremiumPlan.YEARLY, binding.tvYearlyDays, binding.tvYearlyPrice)
        bindPlanCard(PremiumPlan.MONTHLY, binding.tvMonthlyDays, binding.tvMonthlyPrice)
        bindPlanCard(PremiumPlan.WEEKLY, binding.tvWeeklyDays, binding.tvWeeklyPrice)
        bindYearlyWeeklyCost()
    }

    private fun bindPlanCard(
        plan: PremiumPlan,
        daysView: android.widget.TextView,
        priceView: android.widget.TextView
    ) {
        daysView.text = PremiumPlanManager.formatPlanDays(plan)
        priceView.text = getDisplayPrice(plan)
    }

    private fun bindYearlyWeeklyCost() {
        val yearlyPrice = getDisplayPrice(PremiumPlan.YEARLY)
        val store = viewModel.getStorePriceInfo(PremiumPlan.YEARLY)
        val weekly = PremiumPlanManager.formatYearlyAsWeeklyPrice(
            yearlyFormattedPrice = yearlyPrice,
            amountMicros = store?.amountMicros,
            currencyCode = store?.currencyCode
        )
        binding.tvYearlyWeeklyCost.isVisible = weekly.isNotBlank()
        if (weekly.isNotBlank()) {
            binding.tvYearlyWeeklyCost.text = getString(R.string.premium_just_per_week, weekly)
        }
    }

    private fun getDisplayPrice(plan: PremiumPlan): String {
        // Prefer Play Console price when available (especially for yearly weekly calc).
        viewModel.getStorePrice(plan)?.takeIf { it.isNotBlank() }?.let { return it }
        val config = PremiumPlanManager.getPlan(plan)
        return config.displayPrice().ifBlank { PremiumPlanManager.formatPrice(config.priceValue()) }
    }

    private fun updateSelectedPlanSummary() {
//        val price = getDisplayPrice(selectedPlan)
//        binding.tvContinueText.text = getString(R.string.premium_continue_with_price, price)
    }

    private fun resolveBillingError(error: BillingUiState.Error): String {
        return when (error.type) {
            BillingUiState.Error.Type.PRODUCT_NOT_FOUND -> getString(
                R.string.billing_product_not_available,
                error.productId.ifBlank { "unknown" }
            )
            BillingUiState.Error.Type.NO_OFFER -> getString(
                R.string.billing_no_offer_available,
                error.productId.ifBlank { "unknown" }
            )
            BillingUiState.Error.Type.GENERIC -> error.message.ifBlank {
                getString(R.string.billing_purchase_failed)
            }
        }
    }

    private fun setPurchaseLoading(loading: Boolean) {
        binding.btnContinue.isEnabled = !loading
        binding.btnContinue.alpha = if (loading) 0.6f else 1f
        binding.continueGlowBorder.alpha = if (loading) 0.35f else 1f
        binding.tvRestore.isEnabled = !loading
        binding.tvRestore.alpha = if (loading) 0.6f else 1f
        if (loading) {
            continueButtonAnimator?.stop()
        } else {
            continueButtonAnimator?.start()
        }
    }

    private fun navigateAfterPurchase() {
        if (fromIntro) {
            completeIntroAndGoHome()
        } else {
            finish()
        }
    }

    private fun applyTitleGradient() {
        val textView = binding.tvTitleLine2
        textView.post {
            val width = textView.width.toFloat().takeIf { it > 0f }
                ?: textView.paint.measureText(textView.text.toString())
            val shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    ContextCompat.getColor(this, R.color.premium_title_gradient_start),
                    ContextCompat.getColor(this, R.color.premium_title_gradient_end)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            textView.paint.shader = shader
            textView.invalidate()
        }
    }

    private fun applyFooterUnderline() {
        val privacy = "Privacy Policy"
        binding.tvFooter.text = SpannableString(privacy).apply {
            setSpan(UnderlineSpan(), 0, privacy.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val restore = "Terms & conditions"
        binding.termsAndConditions.text = SpannableString(restore).apply {
            setSpan(UnderlineSpan(), 0, restore.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyUpgradePlanVisibility() {
        // Stacked plans: hide tiers already active; keep others buyable so both stay active.
        val showWeekly = !PremiumPlanManager.isPlanActive(PremiumPlan.WEEKLY)
        val showMonthly = !PremiumPlanManager.isPlanActive(PremiumPlan.MONTHLY)
        val showYearly = !PremiumPlanManager.isPlanActive(PremiumPlan.YEARLY)

        setPlanCardVisible(binding.cardWeekly, showWeekly)
        setPlanCardVisible(binding.cardMonthly, showMonthly)
        setPlanCardVisible(binding.cardYearly, showYearly)

        selectedPlan = when {
            showYearly -> PremiumPlan.YEARLY
            showMonthly -> PremiumPlan.MONTHLY
            showWeekly -> PremiumPlan.WEEKLY
            else -> PremiumPlan.YEARLY
        }
    }

    private fun setPlanCardVisible(card: View, visible: Boolean) {
        card.isVisible = visible
        (card.parent as? View)?.isVisible = visible
    }

    private fun updatePlanSelection(plan: PremiumPlan) {
        if (!isPlanVisible(plan)) return
        selectedPlan = plan

        binding.cardYearly.setBackgroundResource(
            if (plan == PremiumPlan.YEARLY) R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected
        )
        binding.cardMonthly.setBackgroundResource(
            if (plan == PremiumPlan.MONTHLY) R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected
        )
        binding.cardWeekly.setBackgroundResource(
            if (plan == PremiumPlan.WEEKLY) R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected
        )

        val gold = ContextCompat.getColor(this, R.color.premium_gold)
        val white = ContextCompat.getColor(this, R.color.white)
        binding.tvYearlyPrice.setTextColor(if (plan == PremiumPlan.YEARLY) gold else white)
        binding.tvMonthlyPrice.setTextColor(if (plan == PremiumPlan.MONTHLY) gold else white)
        binding.tvWeeklyPrice.setTextColor(if (plan == PremiumPlan.WEEKLY) gold else white)

        updateSelectedPlanSummary()
    }

    private fun isPlanVisible(plan: PremiumPlan): Boolean = when (plan) {
        PremiumPlan.YEARLY -> binding.cardYearly.isVisible
        PremiumPlan.MONTHLY -> binding.cardMonthly.isVisible
        PremiumPlan.WEEKLY -> binding.cardWeekly.isVisible
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            applyUpgradePlanVisibility()
            updatePlanSelection(selectedPlan)
            playPremiumVideo()
            continueButtonAnimator?.start()
        }
    }

    override fun onPause() {
        continueButtonAnimator?.stop()
        if (_binding != null) {
            IntroVideoHelper.pause(binding.ivPremiumVideo)
        }
        super.onPause()
    }

    override fun onDestroy() {
        closeRevealJob?.cancel()
        closeRevealJob = null
        continueButtonAnimator?.stop()
        continueButtonAnimator = null
        _binding?.let { IntroVideoHelper.release(it.ivPremiumVideo) }
        videoPrepared = false
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val ARG_FROM_INTRO = "from_intro"
        private const val CLOSE_BUTTON_REVEAL_DELAY_MS = 3_000L

        fun createIntent(context: Context, fromIntro: Boolean): Intent {
            return Intent(context, PremiumActivity::class.java).apply {
                putExtra(ARG_FROM_INTRO, fromIntro)
            }
        }
    }
}
