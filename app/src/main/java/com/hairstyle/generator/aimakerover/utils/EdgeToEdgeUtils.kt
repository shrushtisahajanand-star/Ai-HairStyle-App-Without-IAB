package com.hairstyle.generator.aimakerover.utils

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.hairstyle.generator.aimakeover.R

/**
 * Edge-to-edge setup: content draws behind transparent status bar,
 * bottom system navigation bar stays visible (not hidden).
 * Works on API 26+.
 */
fun Activity.setupEdgeToEdge(contentView: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = getColor(R.color.background)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        isAppearanceLightNavigationBars = true
    }
}

/**
 * Fullscreen ad activities (inter / app-open) temporarily hide the system bars, and on
 * Android 15+ the host window then gets 0 insets. Using the "ignoring visibility" value keeps
 * paddings stable so content does not jump up while an ad is in front.
 */
fun WindowInsetsCompat.stableStatusBarTop(): Int =
    maxOf(
        getInsets(WindowInsetsCompat.Type.statusBars()).top,
        getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top
    )

fun WindowInsetsCompat.stableNavigationBarBottom(): Int =
    maxOf(
        getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
        getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom,
        getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
        getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).bottom,
        getInsets(WindowInsetsCompat.Type.tappableElement()).bottom
    )

fun WindowInsetsCompat.contentBottomInset(): Int =
    maxOf(getInsets(WindowInsetsCompat.Type.ime()).bottom, stableNavigationBarBottom())

fun View.fallbackNavigationBarHeight(): Int {
    val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resId != 0) resources.getDimensionPixelSize(resId) else 0
}

fun WindowInsetsCompat.contentBottomInsetOrFallback(view: View): Int {
    val inset = contentBottomInset()
    return if (inset > 0) inset else view.fallbackNavigationBarHeight()
}

fun Activity.setStatusBarLightIcons(lightIcons: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightIcons
    }
}

/** Use on headers/toolbars over hero images so content clears the status bar. */
fun View.applyStatusBarPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val lpHeight = view.layoutParams?.height
        val isScreenRoot = lpHeight == ViewGroup.LayoutParams.MATCH_PARENT ||
            lpHeight == 0
        view.updatePadding(
            top = insets.stableStatusBarTop(),
            bottom = if (isScreenRoot) {
                insets.contentBottomInsetOrFallback(view)
            } else {
                view.paddingBottom
            }
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Prefer this for fixed-size buttons (close/back) under a transparent status bar.
 * Padding on a small ImageView clips/hides the icon; margin keeps the icon size intact.
 */
fun View.applyStatusBarTopMargin() {
    val initialTopMargin = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return@setOnApplyWindowInsetsListener insets
        lp.topMargin = initialTopMargin + insets.stableStatusBarTop()
        view.layoutParams = lp
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
