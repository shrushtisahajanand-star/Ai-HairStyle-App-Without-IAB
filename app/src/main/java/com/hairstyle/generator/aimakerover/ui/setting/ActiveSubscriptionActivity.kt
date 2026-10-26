package com.hairstyle.generator.aimakerover.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentActiveSubscriptionBinding
import com.hairstyle.generator.aimakeover.databinding.ItemActivePlanCardBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.data.model.ActivePlanRecord
import com.hairstyle.generator.aimakerover.ui.BaseActivity
import com.hairstyle.generator.aimakerover.ui.premium.PremiumPlan
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ActiveSubscriptionActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentActiveSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentActiveSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)
        // Re-bind when RC sync updates startedAt / expiresAt.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                billingManager.entitlementRevision.collect {
                    if (!PremiumPlanManager.isPremiumActive()) {
                        finish()
                    } else {
                        bindPlanDetails()
                    }
                }
            }
        }
        bindPlanDetails()
        billingManager.startConnection()
    }

    override fun onResume() {
        super.onResume()
        if (!PremiumPlanManager.isPremiumActive()) {
            finish()
            return
        }
        billingManager.startConnection()
        bindPlanDetails()
    }

    private fun bindPlanDetails() {
        if (!PremiumPlanManager.isPremiumActive()) return

        val plans = PremiumPlanManager.getActivePlans()
        binding.plansContainer.removeAllViews()
        plans.forEach { record ->
            binding.plansContainer.addView(createPlanCard(record))
        }

        // Upgrade available until Yearly is among active plans.
        val canUpgrade = !PremiumPlanManager.isPlanActive(PremiumPlan.YEARLY)
        binding.bottomBar.isVisible = canUpgrade
    }

    private fun createPlanCard(record: ActivePlanRecord): View {
        val card = ItemActivePlanCardBinding.inflate(
            layoutInflater,
            binding.plansContainer,
            false
        )
        val plan = PremiumPlanManager.planFromKey(record.planKey)
        card.tvPlanName.text = when (plan) {
            PremiumPlan.YEARLY -> getString(R.string.plan_name_yearly)
            PremiumPlan.MONTHLY -> getString(R.string.plan_name_monthly)
            PremiumPlan.WEEKLY -> getString(R.string.plan_name_weekly)
            null -> getString(R.string.current_plan)
        }

        // Values come from local ActivePlanRecord (written by BillingManager from RevenueCat).
        card.tvStartedOn.text = if (record.startedAt > 0L) {
            dateFormat.format(Date(record.startedAt))
        } else {
            "—"
        }
        card.tvExpiresOn.text = if (record.expiresAt > 0L) {
            dateFormat.format(Date(record.expiresAt))
        } else {
            "—"
        }

        card.tvRemaining.text = getString(
            R.string.plan_remaining_days,
            remainingDays(record.expiresAt)
        )
        card.tvStatus.text = getString(R.string.plan_active_badge)
        return card.root
    }

    /** Whole days left, rounded up (23h → 1 day). */
    private fun remainingDays(expiresAt: Long): Int {
        val remainingMs = expiresAt - System.currentTimeMillis()
        if (remainingMs <= 0L) return 0
        val days = TimeUnit.MILLISECONDS.toDays(remainingMs)
        val hasPartialDay = remainingMs % TimeUnit.DAYS.toMillis(1) != 0L
        return (days + if (hasPartialDay) 1 else 0).toInt().coerceAtLeast(1)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
