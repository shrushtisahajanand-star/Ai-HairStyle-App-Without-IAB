package com.hairstyle.generator.aimakerover.ui.intro

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.ui.premium.PremiumFragment
import com.hairstyle.generator.aimakerover.utils.IntroManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager

internal fun Fragment.setupIntroCloseButton(closeView: View) {
    closeView.setOnClickListener { handleIntroCloseClick() }
}

internal fun Fragment.handleIntroCloseClick() {
    when (this) {
        is Intro2Fragment -> findNavController().navigate(R.id.action_intro2_to_intro3)
        is Intro3Fragment -> navigateIntroToPremiumOrHome()
        else -> Unit
    }
}

/** Onboarding paywall — skip if Play Billing already restored an active premium plan. */
internal fun Fragment.navigateIntroToPremiumOrHome() {
    if (PremiumPlanManager.isPremiumActive()) {
        PremiumPlanManager.syncAdsFreeFlag()
        completeIntroAndGoHome()
        return
    }
    findNavController().navigate(
        R.id.action_intro3_to_premium,
        bundleOf(PremiumFragment.ARG_FROM_INTRO to true)
    )
}

internal fun Fragment.completeIntroAndGoHome() {
    IntroManager.setIntroCompleted()
    val nav = findNavController()
    when (nav.currentDestination?.id) {
        R.id.premiumFragment -> nav.navigate(R.id.action_premium_to_home)
        R.id.intro3Fragment -> nav.navigate(R.id.action_intro3_to_home)
        else -> nav.navigate(R.id.action_intro3_to_home)
    }
}
