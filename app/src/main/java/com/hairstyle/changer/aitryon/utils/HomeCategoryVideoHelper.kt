package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.widget.ImageView
import androidx.core.view.isVisible
import com.facebook.shimmer.ShimmerFrameLayout
import okhttp3.OkHttpClient

object HomeCategoryVideoHelper {

    class Handle(
        private val webpView: ImageView,
        private val shimmerLayout: ShimmerFrameLayout
    ) {
        private var started = false

        fun preload(context: Context, okHttpClient: OkHttpClient, videoTitleKey: String) {
            val remoteUrl = VideoUrlManager.getVideoUrl(videoTitleKey) ?: return
            VideoCacheManager.cacheVideo(context, okHttpClient, videoTitleKey, remoteUrl)
        }

        fun play(context: Context, videoTitleKey: String) {
            val remoteUrl = VideoUrlManager.getVideoUrl(videoTitleKey)
            if (remoteUrl.isNullOrBlank()) {
                showShimmer()
                return
            }

            if (started) {
                SplashAnimationHelper.resume(webpView)
                hideShimmer()
                return
            }

            showShimmer()
            webpView.isVisible = true
            val cachedFile = VideoCacheManager.getCachedMediaFile(context, videoTitleKey, remoteUrl)
            val source: Any = cachedFile ?: remoteUrl
            SplashAnimationHelper.playWebp(
                imageView = webpView,
                data = source,
                onReady = {
                    started = true
                    hideShimmer()
                },
                onError = {
                    showShimmer()
                }
            )
        }

        fun pause() {
            SplashAnimationHelper.pause(webpView)
        }

        fun release() {
            SplashAnimationHelper.release(webpView)
            started = false
        }

        private fun showShimmer() {
            shimmerLayout.isVisible = true
            shimmerLayout.startShimmer()
        }

        private fun hideShimmer() {
            shimmerLayout.stopShimmer()
            shimmerLayout.isVisible = false
        }
    }
}
