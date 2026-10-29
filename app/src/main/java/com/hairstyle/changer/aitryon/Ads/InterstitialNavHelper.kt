package com.hairstyle.changer.aitryon.Ads

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

object InterstitialNavHelper {

    fun forwardAdId(): String =
        ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME)

    fun backAdId(): String {
        val back = ADSMainClass.getStringValue(ADSMainClass.INTER_BACK_AD_ID)
        return back.ifBlank { forwardAdId() }
    }

    fun preload(activity: Activity?) {
        if (activity == null || activity.isFinishing) return
        ADSInterDisplayClick.preloadIfNeeded(activity, forwardAdId())
    }

    /** Forward click counter — {@code inter_ads_click}. */
    fun showOnClick(fragment: Fragment, onContinue: () -> Unit) {
        val activity = fragment.activity ?: run {
            onContinue()
            return
        }
        showOnClick(activity, onContinue)
    }

    fun showOnClick(activity: Activity, onContinue: () -> Unit) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            activity,
            forwardAdId(),
            ADSInterDisplayClick.OnFinishAds { onContinue() }
        )
    }

    /**
     * Same as [showOnClick] when [enabled] is true; otherwise runs [onContinue] immediately
     * (Firebase per-action `*_show_inter` flags).
     */
    fun showOnClick(fragment: Fragment, enabled: Boolean, onContinue: () -> Unit) {
        if (!enabled) {
            onContinue()
            return
        }
        showOnClick(fragment, onContinue)
    }

    fun showOnClick(activity: Activity, enabled: Boolean, onContinue: () -> Unit) {
        if (!enabled) {
            onContinue()
            return
        }
        showOnClick(activity, onContinue)
    }

    /**
     * Intro1–3 only — uses Firebase {@code intro_inter_ads_click_count}
     * (separate from global {@code inter_ads_click}).
     */
    fun showOnIntroClick(fragment: Fragment, enabled: Boolean, onContinue: () -> Unit) {
        val activity = fragment.activity ?: run {
            onContinue()
            return
        }
        showOnIntroClick(activity, enabled, onContinue)
    }

    fun showOnIntroClick(activity: Activity, enabled: Boolean, onContinue: () -> Unit) {
        if (!enabled) {
            onContinue()
            return
        }
        ADSInterDisplayClick.ADSIntroInterstitialShowing(
            activity,
            forwardAdId(),
            ADSInterDisplayClick.OnFinishAds { onContinue() }
        )
    }

    fun showOnBottomNav(fragment: Fragment, onContinue: () -> Unit) {
        val activity = fragment.activity ?: run {
            onContinue()
            return
        }
        showOnBottomNav(activity, onContinue)
    }

    fun showOnBottomNav(activity: Activity, onContinue: () -> Unit) {
        ADSInterDisplayClick.ADSBottomNavInterstitial(
            activity,
            forwardAdId(),
            ADSInterDisplayClick.OnFinishAds { onContinue() }
        )
    }

    /** Back click counter — {@code inter_back_ads_click}. */
    fun showOnBack(fragment: Fragment, onContinue: () -> Unit) {
        val activity = fragment.activity ?: run {
            onContinue()
            return
        }
        showOnBack(activity, onContinue)
    }

    fun showOnBack(activity: Activity, onContinue: () -> Unit) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            activity,
            backAdId(),
            ADSInterDisplayClick.OnFinishAds { onContinue() }
        )
    }

    fun navigateUpWithBackInter(fragment: Fragment) {
        showOnBack(fragment) {
            fragment.activity?.finish()
        }
    }

    fun navigateUpWithBackInter(activity: Activity) {
        showOnBack(activity) {
            activity.finish()
        }
    }

    /** System back → same as toolbar back (back-click counter). */
    fun installSystemBackInter(fragment: Fragment) {
        fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment.viewLifecycleOwner) {
            navigateUpWithBackInter(fragment)
        }
    }

    fun installSystemBackInter(activity: ComponentActivity, owner: LifecycleOwner = activity) {
        activity.onBackPressedDispatcher.addCallback(owner) {
            navigateUpWithBackInter(activity)
        }
    }
}
