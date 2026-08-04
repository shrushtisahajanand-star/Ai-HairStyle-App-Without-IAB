package com.hairstyle.generator.aimakerover.utils

import android.view.View
import androidx.core.view.isVisible
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout

fun shouldShowListShimmer(isLoading: Boolean, hasItems: Boolean, hasError: Boolean): Boolean {
    return isLoading && !hasItems && !hasError
}

/**
 * Apply a clearly-visible left-to-right sweep (not a static gray block).
 * Uses alpha highlight — more reliable than color tint on dark themes.
 */
fun ShimmerFrameLayout.applyAppShimmer() {
    val shimmer = Shimmer.AlphaHighlightBuilder()
        .setBaseAlpha(0.25f)
        .setHighlightAlpha(1f)
        .setDuration(1_100L)
        .setDropoff(0.35f)
        .setIntensity(0.55f)
        .setTilt(0f)
        .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
        .setAutoStart(true)
        .build()
    setShimmer(shimmer)
}

fun ShimmerFrameLayout.showListShimmer() {
    isVisible = true
    applyAppShimmer()
    // NestedScrollView / first layout: start after measured so sweep actually draws
    post {
        if (isVisible) {
            startShimmer()
        }
    }
}

fun ShimmerFrameLayout.hideListShimmer() {
    stopShimmer()
    isVisible = false
}

fun ShimmerFrameLayout.bindListLoading(
    isLoading: Boolean,
    hasItems: Boolean,
    hasError: Boolean,
    contentView: View
) {
    val showShimmer = shouldShowListShimmer(isLoading, hasItems, hasError)
    contentView.isVisible = !showShimmer
    if (showShimmer) {
        showListShimmer()
    } else {
        hideListShimmer()
    }
}
