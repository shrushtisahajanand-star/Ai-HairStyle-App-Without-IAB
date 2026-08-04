package com.hairstyle.generator.aimakerover.ui.intro

import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper

/**
 * System back on Language / Premium / Intro.
 *
 * Intro only: when Firebase `intro_back_send_to_next` is true,
 * back advances to the next intro screen (1 → 1b → 1c → 1d → 2 → 3 → premium/home).
 * When false, normal back (with back interstitial) as before.
 */
internal fun Fragment.setupSystemBackNavigation() {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        if (ADSMainClass.getIntroBackSendToNext() && navigateIntroBackToNextIfPossible()) {
            return@addCallback
        }
        InterstitialNavHelper.navigateUpWithBackInter(this@setupSystemBackNavigation)
    }
}

/**
 * @return true if this is an intro screen and navigation to next was performed.
 */
private fun Fragment.navigateIntroBackToNextIfPossible(): Boolean {
    val nav = findNavController()
    return when (this) {
        is Intro1Fragment -> {
            nav.navigate(R.id.action_intro1_to_intro1b)
            true
        }
        is Intro1bFragment -> {
            nav.navigate(R.id.action_intro1b_to_intro1c)
            true
        }
        is Intro1cFragment -> {
            nav.navigate(R.id.action_intro1c_to_intro1d)
            true
        }
        is Intro1dFragment -> {
            nav.navigate(R.id.action_intro1d_to_intro2)
            true
        }
        is Intro2Fragment -> {
            nav.navigate(R.id.action_intro2_to_intro3)
            true
        }
        is Intro3Fragment -> {
            navigateIntroToPremiumOrHome()
            true
        }
        else -> false
    }
}
