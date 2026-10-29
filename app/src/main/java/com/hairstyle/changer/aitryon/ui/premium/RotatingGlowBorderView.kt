package com.hairstyle.changer.aitryon.ui.premium

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.hairstyle.changer.aitryon.R
import kotlin.math.min

/**
 * Soft white glow that travels continuously around a rounded (pill) border —
 * matches the Premium "Continue" button ring/shadow effect.
 */
class RotatingGlowBorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderRect = RectF()
    private val gradientMatrix = Matrix()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val softPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var angleDegrees = 0f
    private var animator: ValueAnimator? = null
    private var cornerRadius = 0f

    init {
        // Soft blur needs software layer for MaskFilter-like softness via multi-pass draw.
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun start() {
        if (animator?.isRunning == true) return
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = ROTATION_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                angleDegrees = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = dens(6f)
        borderRect.set(inset, inset, w - inset, h - inset)
        cornerRadius = min(borderRect.height() / 2f, dens(20f))
        strokePaint.strokeWidth = dens(3.5f)
        softPaint.strokeWidth = dens(8f)
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val cx = width / 2f
        val cy = height / 2f

        val glowColor = ContextCompat.getColor(context, R.color.white)
        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.TRANSPARENT,
            Color.argb(40, 255, 255, 255),
            Color.argb(180, 255, 255, 255),
            Color.argb(40, 255, 255, 255),
            Color.TRANSPARENT,
            Color.TRANSPARENT
        )
        val positions = floatArrayOf(0f, 0.55f, 0.68f, 0.78f, 0.88f, 0.95f, 1f)

        val shader = SweepGradient(cx, cy, colors, positions)
        gradientMatrix.reset()
        gradientMatrix.postRotate(angleDegrees, cx, cy)
        shader.setLocalMatrix(gradientMatrix)

        softPaint.shader = shader
        softPaint.alpha = 120
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, softPaint)

        strokePaint.shader = shader
        strokePaint.color = glowColor
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, strokePaint)
    }

    private fun dens(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val ROTATION_MS = 2200L
    }
}
