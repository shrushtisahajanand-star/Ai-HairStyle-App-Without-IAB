package com.hairstyle.changer.aitryon.utils

import android.util.Log
import android.widget.ImageView
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import com.facebook.shimmer.ShimmerFrameLayout
import com.hairstyle.changer.aitryon.repository.CategoryImagesLogger

/**
 * Fun / Beard style: keep shimmer visible until Coil actually paints the image.
 */
fun ImageView.loadWithShimmer(
    url: String?,
    shimmer: ShimmerFrameLayout
) {
    dispose()

    if (url.isNullOrBlank()) {
        Log.w("CategoryImages", "[Coil] blank url — image not loaded")
        setImageDrawable(null)
        shimmer.hideImageShimmer()
        return
    }

    setImageDrawable(null)
    shimmer.showImageShimmer()
    load(url) {
        crossfade(true)
        listener(
            onSuccess = { _, _ ->
                CategoryImagesLogger.coilLoad(url, success = true)
                shimmer.hideImageShimmer()
            },
            onError = { _, result ->
                CategoryImagesLogger.coilLoad(
                    url,
                    success = false,
                    error = result.throwable.message
                )
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
