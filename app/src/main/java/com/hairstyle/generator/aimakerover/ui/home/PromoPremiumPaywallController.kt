package com.hairstyle.generator.aimakerover.ui.home

import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.PagePromoPremiumPaywallBinding
import com.hairstyle.generator.aimakerover.billing.BillingUiState
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.ui.premium.PremiumPlan
import com.hairstyle.generator.aimakerover.ui.premium.PremiumViewModel
import com.hairstyle.generator.aimakerover.utils.AppLinkHelper
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.PromoPremiumMediaHelper
import com.hairstyle.generator.aimakerover.utils.PromoVideoCardSizer
import com.hairstyle.generator.aimakerover.utils.VideoTitleKeys
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class PromoPremiumPaywallController(
    private val binding: PagePromoPremiumPaywallBinding,
    private val fragment: Fragment,
    private val viewModel: PremiumViewModel,
    private val remoteConfigManager: RemoteConfigManager,
    private val okHttpClient: OkHttpClient,
    private val onClose: () -> Unit,
    private val onPurchaseSuccess: () -> Unit
) {
    private var selectedPlan = PremiumPlan.YEARLY
    private var bound = false
    private var videoStarted = false

    fun bindIfNeeded() {
        if (bound) return
        bound = true

        applyTitleGradient()
        applyFooterUnderline()
        updatePlanSelection(selectedPlan)
        observeBillingState()
        observeStorePrices()
        loadPremiumPlansAndRefresh()

        binding.btnClose.setOnClickListener { onClose() }
        binding.cardYearly.setOnClickListener { updatePlanSelection(PremiumPlan.YEARLY) }
        binding.cardMonthly.setOnClickListener { updatePlanSelection(PremiumPlan.MONTHLY) }
        binding.cardWeekly.setOnClickListener { updatePlanSelection(PremiumPlan.WEEKLY) }
        binding.btnPay.setOnClickListener {
            viewModel.purchase(requireNotNull(fragment.activity), selectedPlan)
        }

        PromoVideoCardSizer.applyFallbackRatio(binding.cardPremiumVideo)

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            PromoPremiumMediaHelper.preload(
                fragment.requireContext(),
                okHttpClient,
                VideoTitleKeys.PREMIUM_INTRO
            )
        }
    }

    fun startVideoIfNeeded() {
        if (videoStarted) {
            PromoPremiumMediaHelper.resume(binding.premiumWebpView)
            return
        }
        videoStarted = true
        PromoPremiumMediaHelper.play(
            context = fragment.requireContext(),
            webpView = binding.premiumWebpView,
            shimmerLayout = binding.shimmerPremiumVideo,
            videoTitleKey = VideoTitleKeys.PREMIUM_INTRO,
            videoCard = binding.cardPremiumVideo
        )
    }

    fun pauseVideo() {
        PromoPremiumMediaHelper.pause(binding.premiumWebpView)
    }

    fun releaseVideo() {
        PromoPremiumMediaHelper.release(binding.premiumWebpView)
        videoStarted = false
    }

    private fun observeBillingState() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.billingState.collect { state ->
                    when (state) {
                        BillingUiState.Idle -> setPurchaseLoading(false)
                        BillingUiState.Loading -> setPurchaseLoading(true)
                        is BillingUiState.Success -> {
                            setPurchaseLoading(false)
                            fragment.showSnackbar(fragment.getString(R.string.billing_purchase_success))
                            onPurchaseSuccess()
                            viewModel.resetBillingState()
                        }
                        BillingUiState.Restored -> {
                            setPurchaseLoading(false)
                            fragment.showSnackbar(fragment.getString(R.string.billing_restore_success))
                            onPurchaseSuccess()
                            viewModel.resetBillingState()
                        }
                        BillingUiState.NothingToRestore -> {
                            setPurchaseLoading(false)
                            fragment.showSnackbar(fragment.getString(R.string.billing_nothing_to_restore))
                            viewModel.resetBillingState()
                        }
                        BillingUiState.Cancelled -> {
                            setPurchaseLoading(false)
                            viewModel.resetBillingState()
                        }
                        is BillingUiState.Error -> {
                            setPurchaseLoading(false)
                            fragment.showSnackbar(resolveBillingError(state))
                            viewModel.resetBillingState()
                        }
                    }
                }
            }
        }
    }

    private fun loadPremiumPlansAndRefresh() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            if (!PremiumPlanManager.hasRemotePlanDetails()) {
                runCatching { remoteConfigManager.fetchAndSaveAll() }
            }
            refreshAllPlanPrices()
        }
    }

    private fun observeStorePrices() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productsLoaded.collect {
                    if (it > 0) {
                        refreshAllPlanPrices()
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
            binding.tvYearlyWeeklyCost.text =
                fragment.getString(R.string.premium_just_per_week, weekly)
        }
    }

    private fun getDisplayPrice(plan: PremiumPlan): String {
        viewModel.getStorePrice(plan)?.takeIf { it.isNotBlank() }?.let { return it }
        val config = PremiumPlanManager.getPlan(plan)
        return config.displayPrice().ifBlank { PremiumPlanManager.formatPrice(config.priceValue()) }
    }

    private fun updatePlanSelection(plan: PremiumPlan) {
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

        val gold = ContextCompat.getColor(fragment.requireContext(), R.color.premium_gold)
        val white = ContextCompat.getColor(fragment.requireContext(), R.color.white)
        binding.tvYearlyPrice.setTextColor(if (plan == PremiumPlan.YEARLY) gold else white)
        binding.tvMonthlyPrice.setTextColor(if (plan == PremiumPlan.MONTHLY) gold else white)
        binding.tvWeeklyPrice.setTextColor(if (plan == PremiumPlan.WEEKLY) gold else white)
    }

    private fun applyTitleGradient() {
        val textView = binding.tvTitleLine2
        textView.post {
            val width = textView.width.toFloat().takeIf { it > 0f }
                ?: textView.paint.measureText(textView.text.toString())
            val shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    ContextCompat.getColor(fragment.requireContext(), R.color.premium_title_gradient_start),
                    ContextCompat.getColor(fragment.requireContext(), R.color.premium_title_gradient_end)
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
        binding.tvFooter.setOnClickListener {
            AppLinkHelper.openPrivacyPolicy(fragment.requireContext())
        }
        val terms = "Terms & Conditions"
        binding.termsAndConditions.text = SpannableString(terms).apply {
            setSpan(UnderlineSpan(), 0, terms.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.termsAndConditions.setOnClickListener {
            AppLinkHelper.openTermsOfService(fragment.requireContext())
        }
    }

    private fun setPurchaseLoading(loading: Boolean) {
        binding.btnPay.isEnabled = !loading
        binding.btnPay.alpha = if (loading) 0.6f else 1f
    }

    private fun resolveBillingError(error: BillingUiState.Error): String {
        return when (error.type) {
            BillingUiState.Error.Type.PRODUCT_NOT_FOUND -> fragment.getString(
                R.string.billing_product_not_available,
                error.productId.ifBlank { "unknown" }
            )
            BillingUiState.Error.Type.NO_OFFER -> fragment.getString(
                R.string.billing_no_offer_available,
                error.productId.ifBlank { "unknown" }
            )
            BillingUiState.Error.Type.GENERIC -> error.message.ifBlank {
                fragment.getString(R.string.billing_purchase_failed)
            }
        }
    }
}
