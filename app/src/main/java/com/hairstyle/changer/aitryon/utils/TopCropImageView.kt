package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class TopCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private enum class CropMode {
        TOP,
        FIT
    }

    private var cropMode = CropMode.TOP

    init {
        val typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.scaleType))
        cropMode = when (typedArray.getInt(0, ScaleType.CENTER_CROP.ordinal)) {
            ScaleType.FIT_CENTER.ordinal,
            ScaleType.FIT_START.ordinal,
            ScaleType.FIT_END.ordinal -> CropMode.FIT
            else -> CropMode.TOP
        }
        typedArray.recycle()
        scaleType = ScaleType.MATRIX
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        computeImageMatrix()
        return changed
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeImageMatrix()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        scheduleMatrixUpdate()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        scheduleMatrixUpdate()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        scheduleMatrixUpdate()
    }

    private fun scheduleMatrixUpdate() {
        if (width > 0 && height > 0) {
            computeImageMatrix()
            invalidate()
        } else {
            post { computeImageMatrix() }
        }
    }

    private fun computeImageMatrix() {
        val d = drawable ?: return

        val drawableWidth = d.intrinsicWidth.toFloat()
        val drawableHeight = d.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val matrix = Matrix()
        val scale = when (cropMode) {
            CropMode.FIT -> minOf(
                viewWidth / drawableWidth,
                viewHeight / drawableHeight
            )
            CropMode.TOP -> maxOf(
                viewWidth / drawableWidth,
                viewHeight / drawableHeight
            )
        }

        matrix.setScale(scale, scale)

        val dx = (viewWidth - drawableWidth * scale) / 2f
        val dy = when (cropMode) {
            CropMode.TOP -> 0f
            CropMode.FIT -> (viewHeight - drawableHeight * scale) / 2f
        }

        matrix.postTranslate(dx, dy)
        imageMatrix = matrix
    }
}
