package com.hairstyle.changer.aitryon.ui.intro

import android.app.Activity
import android.view.View
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.utils.IntroManager
import com.hairstyle.changer.aitryon.utils.PremiumPlanManager

internal fun Activity.setupIntroCloseButton(closeView: View) {
    closeView.setOnClickListener { handleIntroCloseClick() }
}

internal fun Activity.handleIntroCloseClick() {
    when (this) {
        is Intro2Activity, is Intro3Activity -> navigateIntroToPremiumOrHome()
        else -> Unit
    }
}

/** Onboarding paywall — skip if Play Billing already restored an active premium plan. */
internal fun Activity.navigateIntroToPremiumOrHome() {
    if (PremiumPlanManager.isPremiumActive()) {
        PremiumPlanManager.syncAdsFreeFlag()
        completeIntroAndGoHome()
        return
    }
    AppNavigator.openPremium(this, fromIntro = true)
}

internal fun Activity.completeIntroAndGoHome() {
    IntroManager.setIntroCompleted()
    AppNavigator.openHomeClearTask(this)
}
