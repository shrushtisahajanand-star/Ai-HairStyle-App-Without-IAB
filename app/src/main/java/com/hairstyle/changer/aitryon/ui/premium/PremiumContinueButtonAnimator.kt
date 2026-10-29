package com.hairstyle.changer.aitryon.ui.premium

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Rotating glow around Continue button; only the arrow icon swings left–right.
 * Button body + "Continue" text stay still.
 */
class PremiumContinueButtonAnimator(
    private val arrow: View,
    private val glowBorder: RotatingGlowBorderView?
) {

    private var swingSet: AnimatorSet? = null
    private var running = false

    fun start() {
        if (running) return
        running = true
        glowBorder?.start()
        startArrowSwingLoop()
    }

    fun stop() {
        running = false
        swingSet?.cancel()
        swingSet = null
        glowBorder?.stop()
        arrow.translationX = 0f
    }

    private fun startArrowSwingLoop() {
        if (!running) return
        swingSet?.cancel()

        val density = arrow.resources.displayMetrics.density
        val amplitude = 5f * density

        val swing = ObjectAnimator.ofFloat(
            arrow,
            View.TRANSLATION_X,
            0f,
            -amplitude,
            amplitude,
            -amplitude * 0.6f,
            amplitude * 0.6f,
            0f
        ).apply {
            duration = SWING_MS
            interpolator = AccelerateDecelerateInterpolator()
        }

        val pause = ObjectAnimator.ofFloat(arrow, View.TRANSLATION_X, 0f, 0f).apply {
            duration = PAUSE_MS
        }

        swingSet = AnimatorSet().apply {
            playSequentially(swing, pause)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (running) startArrowSwingLoop()
                }
            })
            start()
        }
    }

    companion object {
        private const val SWING_MS = 650L
        private const val PAUSE_MS = 900L
    }
}
