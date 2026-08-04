package com.hairstyle.generator.aimakerover.utils

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import coil.dispose
import coil.load

object SplashAnimationHelper {

    fun isWebpSource(source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        val path = source.substringBefore('?').lowercase()
        return path.endsWith(".webp")
    }

    fun playWebp(
        imageView: ImageView,
        data: Any,
        onReady: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        imageView.visibility = View.VISIBLE
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
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

    fun resume(imageView: ImageView) {
        startAnimation(imageView.drawable)
    }

    fun pause(imageView: ImageView) {
        stopAnimation(imageView.drawable)
    }

    fun release(imageView: ImageView) {
        stopAnimation(imageView.drawable)
        imageView.dispose()
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
    }

    fun localWebpResourceUri(packageName: String, rawResId: Int): Uri {
        return Uri.parse("android.resource://$packageName/$rawResId")
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
}
