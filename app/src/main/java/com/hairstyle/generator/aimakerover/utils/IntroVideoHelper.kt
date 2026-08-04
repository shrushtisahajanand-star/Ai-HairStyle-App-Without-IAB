package com.hairstyle.generator.aimakerover.utils

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.facebook.shimmer.ShimmerFrameLayout
import okhttp3.OkHttpClient

object IntroVideoHelper {

    fun preload(context: Context, okHttpClient: OkHttpClient, videoTitleKey: String) {
        val remoteUrl = VideoUrlManager.getVideoUrl(videoTitleKey) ?: return
        VideoCacheManager.cacheVideo(context, okHttpClient, videoTitleKey, remoteUrl)
    }

    fun play(
        webpView: ImageView,
        shimmerLayout: ShimmerFrameLayout?,
        videoTitleKey: String,
        @DrawableRes fallbackImageRes: Int,
        onPrepared: (width: Int, height: Int) -> Unit = { _, _ -> }
    ) {
        val remoteUrl = VideoUrlManager.getVideoUrl(videoTitleKey)
        if (remoteUrl.isNullOrBlank()) {
            showFallback(webpView, shimmerLayout, fallbackImageRes)
            onPrepared(0, 0)
            return
        }

        webpView.isVisible = true
        showShimmer(shimmerLayout)

        val cachedFile = VideoCacheManager.getCachedMediaFile(webpView.context, videoTitleKey, remoteUrl)
        val source: Any = cachedFile ?: remoteUrl
        SplashAnimationHelper.playWebp(
            imageView = webpView,
            data = source,
            onReady = {
                val drawable = webpView.drawable
                val width = drawable?.intrinsicWidth ?: 0
                val height = drawable?.intrinsicHeight ?: 0
                hideShimmer(shimmerLayout)
                onPrepared(width, height)
            },
            onError = {
                showFallback(webpView, shimmerLayout, fallbackImageRes)
                onPrepared(0, 0)
            }
        )
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

    private fun showFallback(
        webpView: ImageView,
        shimmerLayout: ShimmerFrameLayout?,
        @DrawableRes fallbackImageRes: Int
    ) {
        webpView.setImageResource(fallbackImageRes)
        webpView.isVisible = true
        hideShimmer(shimmerLayout)
    }

    private fun showShimmer(shimmerLayout: ShimmerFrameLayout?) {
        shimmerLayout?.isVisible = true
        shimmerLayout?.startShimmer()
    }

    private fun hideShimmer(shimmerLayout: ShimmerFrameLayout?) {
        shimmerLayout?.stopShimmer()
        shimmerLayout?.isVisible = false
    }
}
