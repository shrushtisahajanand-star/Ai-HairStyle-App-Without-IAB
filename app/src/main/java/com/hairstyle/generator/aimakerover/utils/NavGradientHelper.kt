package com.hairstyle.generator.aimakerover.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hairstyle.generator.aimakeover.R

object NavGradientHelper {

    fun activeStartColor(view: android.view.View): Int =
        ContextCompat.getColor(view.context, R.color.nav_gradient_start)

    fun activeCenterColor(view: android.view.View): Int =
        ContextCompat.getColor(view.context, R.color.nav_gradient_center)

    fun activeEndColor(view: android.view.View): Int =
        ContextCompat.getColor(view.context, R.color.nav_gradient_end)

    fun inactiveColor(view: android.view.View): Int =
        ContextCompat.getColor(view.context, R.color.nav_inactive)

    fun ImageView.setNavIcon(active: Boolean, iconTag: String) {
        tag = iconTag
        if (active) {
            colorFilter = null
            applyGradientTint(
                activeStartColor(this),
                activeCenterColor(this),
                activeEndColor(this),
                iconTag
            )
        } else {
            setImageResource(iconRes(iconTag))
            colorFilter = PorterDuffColorFilter(inactiveColor(this), PorterDuff.Mode.SRC_IN)
        }
    }

    fun TextView.setNavLabel(active: Boolean) {
        tag = if (active) TAG_SELECTED else TAG_UNSELECTED

        if (active) {
            fun applyGradient() {
                if (tag != TAG_SELECTED) return
                paint.shader = createThreeColorGradient(
                    height = height.toFloat().coerceAtLeast(1f),
                    startColor = activeStartColor(this),
                    centerColor = activeCenterColor(this),
                    endColor = activeEndColor(this)
                )
                invalidate()
            }

            if (height > 0) {
                applyGradient()
            } else {
                post { applyGradient() }
            }
        } else {
            paint.shader = null
            setTextColor(inactiveColor(this))
            invalidate()
        }
    }

    fun TextView.applyBrandTextGradient() {
        post {
            val width = width.toFloat().takeIf { it > 0f }
                ?: paint.measureText(text.toString())
            paint.shader = LinearGradient(
                0f,
                0f,
                width,
                0f,
                intArrayOf(
                    activeStartColor(this),
                    activeCenterColor(this),
                    activeEndColor(this)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
    }

    private const val TAG_SELECTED = "nav_selected"
    private const val TAG_UNSELECTED = "nav_unselected"

    private fun createThreeColorGradient(
        height: Float,
        startColor: Int,
        centerColor: Int,
        endColor: Int
    ): LinearGradient {
        return LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(startColor, centerColor, endColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun iconRes(tag: String): Int = when (tag) {
        "fun" -> R.drawable.ic_fun_menu
        "beard" -> R.drawable.ic_beard_menu
        else -> R.drawable.ic_fun_menu
    }

    private fun ImageView.applyGradientTint(
        startColor: Int,
        centerColor: Int,
        endColor: Int,
        iconTag: String
    ) {
        val drawable = ContextCompat.getDrawable(context, iconRes(iconTag))?.mutate() ?: return
        val size = (26 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = createThreeColorGradient(
                height = size.toFloat(),
                startColor = startColor,
                centerColor = centerColor,
                endColor = endColor
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        setImageBitmap(bitmap)
    }
}
