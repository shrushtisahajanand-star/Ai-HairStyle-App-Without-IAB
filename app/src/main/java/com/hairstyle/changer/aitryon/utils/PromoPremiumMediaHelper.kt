package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.widget.ImageView
import androidx.core.view.isVisible
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView

object PromoPremiumMediaHelper {

    fun preload(context: Context, okHttpClient: okhttp3.OkHttpClient, videoTitleKey: String) {
        val remoteUrl = VideoUrlManager.getVideoUrl(videoTitleKey) ?: return
        VideoCacheManager.cacheVideo(context, okHttpClient, videoTitleKey, remoteUrl)
    }

    fun play(
        context: Context,
        webpView: ImageView,
        shimmerLayout: ShimmerFrameLayout,
        videoTitleKey: String,
        videoCard: MaterialCardView? = null,
        onReady: () -> Unit = {},
        onMediaSizeReady: (width: Int, height: Int) -> Unit = { _, _ -> }
    ) {
        val remoteUrl = VideoUrlManager.getVideoUrl(videoTitleKey)
        if (remoteUrl.isNullOrBlank()) {
            showShimmer(shimmerLayout)
            return
        }

        videoCard?.let { PromoVideoCardSizer.applyFallbackRatio(it) }
        showShimmer(shimmerLayout)
        webpView.isVisible = true

        val cachedFile = VideoCacheManager.getCachedMediaFile(context, videoTitleKey, remoteUrl)
        val source: Any = cachedFile ?: remoteUrl
        SplashAnimationHelper.playWebp(
            imageView = webpView,
            data = source,
            onReady = {
                applyDrawableSize(webpView, videoCard, onMediaSizeReady)
                hideShimmer(shimmerLayout)
                onReady()
            },
            onError = {
                showShimmer(shimmerLayout)
            }
        )
    }

    private fun applyDrawableSize(
        webpView: ImageView,
        videoCard: MaterialCardView?,
        onMediaSizeReady: (width: Int, height: Int) -> Unit
    ) {
        val drawable = webpView.drawable ?: return
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        if (width <= 0 || height <= 0) return
        onMediaSizeReady(width, height)
        videoCard?.let { PromoVideoCardSizer.applyAspectRatio(it, width, height) }
    }

    fun pause(webpView: ImageView) {
        SplashAnimationHelper.pause(webpView)
    }

    fun resume(webpView: ImageView) {
        SplashAnimationHelper.resume(webpView)
    }

    fun release(webpView: ImageView) {
        SplashAnimationHelper.release(webpView)
    }

    private fun showShimmer(shimmerLayout: ShimmerFrameLayout) {
        shimmerLayout.isVisible = true
        shimmerLayout.startShimmer()
    }

    private fun hideShimmer(shimmerLayout: ShimmerFrameLayout) {
        shimmerLayout.stopShimmer()
        shimmerLayout.isVisible = false
    }
}
