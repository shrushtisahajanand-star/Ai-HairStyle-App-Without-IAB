package com.hairstyle.generator.aimakerover.utils

import android.widget.ImageView
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * Fun / Beard style: keep shimmer visible until Coil actually paints the image.
 */
fun ImageView.loadWithShimmer(
    url: String?,
    shimmer: ShimmerFrameLayout
) {
    dispose()

    if (url.isNullOrBlank()) {
        setImageDrawable(null)
        shimmer.hideImageShimmer()
        return
    }

    setImageDrawable(null)
    shimmer.showImageShimmer()
    load(url) {
        crossfade(true)
        listener(
            onSuccess = { _, _ -> shimmer.hideImageShimmer() },
            onError = { _, _ ->
                // Keep shimmer placeholder rather than blank card
                shimmer.showImageShimmer()
            }
        )
    }
}

fun ShimmerFrameLayout.showImageShimmer() {
    isVisible = true
    applyAppShimmer()
    post {
        if (isVisible) startShimmer()
    }
}

fun ShimmerFrameLayout.hideImageShimmer() {
    stopShimmer()
    isVisible = false
}
