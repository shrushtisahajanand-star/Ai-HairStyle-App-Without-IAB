package com.hairstyle.generator.aimakerover.utils

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.dispose
import coil.load
import java.io.File
import java.util.WeakHashMap

object SplashAnimationHelper {

    private const val OVERLAY_TAG_PREFIX = "loop_video_overlay_"
    private val videoPlayers = WeakHashMap<ImageView, LoopingVideoPlayer>()

    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "m4v", "3gp", "mkv", "avi")

    fun isWebpSource(source: String?): Boolean {
        return extensionOf(source) == "webp"
    }

    fun isVideoSource(source: String?): Boolean {
        return extensionOf(source) in VIDEO_EXTENSIONS
    }

    fun playWebp(
        imageView: ImageView,
        data: Any,
        onReady: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        releaseVideo(imageView)
        imageView.visibility = View.VISIBLE
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        if (isVideoData(data)) {
            playVideo(imageView, data, onReady, onError)
            return
        }

        playImage(
            imageView = imageView,
            data = data,
            onReady = onReady,
            onError = {
                if (shouldTryVideoFallback(data)) {
                    playVideo(imageView, data, onReady, onError)
                } else {
                    onError()
                }
            }
        )
    }

    fun resume(imageView: ImageView) {
        val player = videoPlayers[imageView]
        if (player != null) {
            player.resume()
            return
        }
        startAnimation(imageView.drawable)
    }

    fun pause(imageView: ImageView) {
        val player = videoPlayers[imageView]
        if (player != null) {
            player.pause()
            return
        }
        stopAnimation(imageView.drawable)
    }

    fun release(imageView: ImageView) {
        releaseVideo(imageView)
        stopAnimation(imageView.drawable)
        imageView.dispose()
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
    }

    fun localWebpResourceUri(packageName: String, rawResId: Int): Uri {
        return Uri.parse("android.resource://$packageName/$rawResId")
    }

    private fun playImage(
        imageView: ImageView,
        data: Any,
        onReady: () -> Unit,
        onError: () -> Unit
    ) {
        imageView.load(data) {
            crossfade(false)
            allowHardware(false)
            listener(
                onSuccess = { _, result ->
                    startAnimation(result.drawable)
                    onReady()
                },
                onError = { _, _ ->
                    onError()
                    false
                }
            )
        }
    }

    private fun playVideo(
        imageView: ImageView,
        data: Any,
        onReady: () -> Unit,
        onError: () -> Unit
    ) {
        imageView.post {
            if (!imageView.isAttachedToWindow) {
                onError()
                return@post
            }
            val overlay = ensureVideoOverlay(imageView) ?: run {
                onError()
                return@post
            }
            videoPlayers.remove(imageView)?.release()
            val player = LoopingVideoPlayer(
                context = imageView.context.applicationContext,
                textureView = overlay
            )
            videoPlayers[imageView] = player
            imageView.visibility = View.INVISIBLE
            overlay.visibility = View.VISIBLE
            player.play(
                data = data,
                onReady = onReady,
                onError = {
                    releaseVideo(imageView)
                    imageView.visibility = View.VISIBLE
                    onError()
                }
            )
        }
    }

    private fun releaseVideo(imageView: ImageView) {
        videoPlayers.remove(imageView)?.release()
        val parent = imageView.parent as? ViewGroup ?: return
        val overlay = parent.findViewWithTag<View>(overlayTag(imageView)) ?: return
        overlay.visibility = View.INVISIBLE
        overlay.post {
            val currentParent = overlay.parent as? ViewGroup ?: return@post
            currentParent.removeView(overlay)
        }
        if (imageView.visibility == View.INVISIBLE) {
            imageView.visibility = View.VISIBLE
        }
    }

    private fun ensureVideoOverlay(imageView: ImageView): TextureView? {
        val parent = imageView.parent as? ViewGroup ?: return null
        val tag = overlayTag(imageView)
        val existing = parent.findViewWithTag<TextureView>(tag)
        if (existing != null) return existing

        val overlay = TextureView(imageView.context).apply {
            this.tag = tag
            isOpaque = true
        }
        parent.addView(overlay, parent.indexOfChild(imageView), copyLayoutParams(imageView))
        return overlay
    }

    private fun copyLayoutParams(from: View): ViewGroup.LayoutParams {
        val lp = from.layoutParams
        return when (lp) {
            is ConstraintLayout.LayoutParams -> ConstraintLayout.LayoutParams(lp)
            is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(lp)
            is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(lp)
            else -> ViewGroup.LayoutParams(lp.width, lp.height)
        }
    }

    private fun overlayTag(imageView: ImageView): String {
        val id = if (imageView.id != View.NO_ID) imageView.id else System.identityHashCode(imageView)
        return OVERLAY_TAG_PREFIX + id
    }

    private fun isVideoData(data: Any): Boolean {
        return when (data) {
            is File -> isVideoSource(data.name)
            is String -> isVideoSource(data)
            is Uri -> isVideoSource(data.toString())
            else -> false
        }
    }

    private fun shouldTryVideoFallback(data: Any): Boolean {
        val ext = when (data) {
            is File -> extensionOf(data.name)
            is String -> extensionOf(data)
            is Uri -> extensionOf(data.toString())
            else -> ""
        }
        return ext.isEmpty() || ext in VIDEO_EXTENSIONS
    }

    private fun extensionOf(source: String?): String {
        if (source.isNullOrBlank()) return ""
        val path = source.substringBefore('?').substringAfterLast('/').lowercase()
        val ext = path.substringAfterLast('.', missingDelimiterValue = "")
        return ext.takeIf { it.isNotBlank() && it.length in 2..5 && '/' !in it }.orEmpty()
    }

    private fun startAnimation(drawable: Drawable?) {
        if (drawable is Animatable && !drawable.isRunning) {
            drawable.start()
        }
    }

    private fun stopAnimation(drawable: Drawable?) {
        if (drawable is Animatable && drawable.isRunning) {
            drawable.stop()
        }
    }

    private class LoopingVideoPlayer(
        private val context: Context,
        private val textureView: TextureView
    ) {
        private var exoPlayer: ExoPlayer? = null
        private var released = false

        fun play(data: Any, onReady: () -> Unit, onError: () -> Unit) {
            val uri = toUri(data) ?: run {
                onError()
                return
            }
            val player = ExoPlayer.Builder(context).build()
            exoPlayer = player
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.volume = 0f
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            player.setVideoTextureView(textureView)
            player.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    if (!released) onReady()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (!released) onError()
                }
            })
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
        }

        fun pause() {
            exoPlayer?.pause()
        }

        fun resume() {
            exoPlayer?.play()
        }

        fun release() {
            released = true
            exoPlayer?.clearVideoTextureView(textureView)
            exoPlayer?.release()
            exoPlayer = null
        }

        private fun toUri(source: Any): Uri? {
            return when (source) {
                is File -> Uri.fromFile(source)
                is Uri -> source
                is String -> Uri.parse(source)
                is Int -> Uri.parse("android.resource://${context.packageName}/$source")
                else -> null
            }
        }
    }
}
