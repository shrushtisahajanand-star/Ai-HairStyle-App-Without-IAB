package com.hairstyle.generator.aimakerover.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentActiveSubscriptionBinding
import com.hairstyle.generator.aimakeover.databinding.ItemActivePlanCardBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.data.model.ActivePlanRecord
import com.hairstyle.generator.aimakerover.ui.premium.PremiumPlan
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ActiveSubscriptionFragment : Fragment() {

    private var _binding: FragmentActiveSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)
        binding.btnUpgradePremium.setOnClickListener {
            findNavController().navigate(R.id.action_activeSubscription_to_premium)
        }
        bindPlanDetails()
    }

    override fun onResume() {
        super.onResume()
        if (!PremiumPlanManager.isPremiumActive()) {
            findNavController().navigateUp()
            return
        }
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
        binding.btnUpgradePremium.isVisible = canUpgrade
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

        card.tvStartedOn.text = dateFormat.format(Date(record.startedAt))
        card.tvExpiresOn.text = if (record.expiresAt > 0L) {
            dateFormat.format(Date(record.expiresAt))
        } else {
            "—"
        }

        val remainingDays = if (record.expiresAt > System.currentTimeMillis()) {
            TimeUnit.MILLISECONDS.toDays(record.expiresAt - System.currentTimeMillis())
                .toInt()
                .coerceAtLeast(1)
        } else {
            0
        }
        card.tvRemaining.text = getString(R.string.plan_remaining_days, remainingDays)
        card.tvStatus.text = getString(R.string.plan_active_badge)
        return card.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
