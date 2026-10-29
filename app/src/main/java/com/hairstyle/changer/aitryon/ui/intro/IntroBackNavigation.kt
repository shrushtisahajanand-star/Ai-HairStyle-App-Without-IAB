package com.hairstyle.changer.aitryon.ui.intro

import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.ui.AppNavigator

/**
 * System back on Language / Intro.
 *
 * Intro only: when Firebase `intro_back_send_to_next` is true,
 * back advances to the next intro screen (1 → 1b → 1c → 1d → home).
 * When false, normal back (with back interstitial) as before.
 */
internal fun ComponentActivity.setupSystemBackNavigation() {
    onBackPressedDispatcher.addCallback(this) {
        if (ADSMainClass.getIntroBackSendToNext() && navigateIntroBackToNextIfPossible()) {
            return@addCallback
        }
        InterstitialNavHelper.navigateUpWithBackInter(this@setupSystemBackNavigation)
    }
}

/**
 * @return true if this is an intro screen and navigation to next was performed.
 */
private fun ComponentActivity.navigateIntroBackToNextIfPossible(): Boolean {
    return when (this) {
        is Intro1Activity -> {
            if (ADSMainClass.isIntroSingleScreen()) {
                false
            } else {
                AppNavigator.openIntro1b(this)
                true
            }
        }
        is Intro1bActivity -> {
            AppNavigator.openIntro1c(this)
            true
        }
        is Intro1cActivity -> {
            AppNavigator.openIntro1d(this)
            true
        }
        is Intro1dActivity -> {
            completeIntroAndGoHome()
            true
        }
        is Intro2Activity, is Intro3Activity -> {
            completeIntroAndGoHome()
            true
        }
        else -> false
    }
}
