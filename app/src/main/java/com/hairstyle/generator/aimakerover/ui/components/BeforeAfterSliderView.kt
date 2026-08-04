package com.hairstyle.generator.aimakerover.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.hairstyle.generator.aimakeover.R
import kotlin.math.abs

class BeforeAfterSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var beforeBitmap: Bitmap? = null
    private var afterBitmap: Bitmap? = null
    private var scaledBefore: Bitmap? = null
    private var scaledAfter: Bitmap? = null

    private var sliderPosition = 0.5f
    private var isDragging = false
    private var downX = 0f
    private var downY = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val imageRect = RectF()
    private val srcRect = Rect()
    private val dstRect = Rect()

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = dp(3f)
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.FILL
    }
    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider_light)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }

    private val leftArrow: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_left)
    private val rightArrow: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_right)

    init {
        isClickable = true
        isFocusable = true
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setImages(before: Bitmap, after: Bitmap) {
        beforeBitmap = before
        afterBitmap = after
        recycleScaledBitmaps()
        if (width > 0 && height > 0) {
            rebuildScaledBitmaps(width, height)
        }
        invalidate()
    }

    fun setSliderPosition(position: Float) {
        sliderPosition = position.coerceIn(MIN_POSITION, MAX_POSITION)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            rebuildScaledBitmaps(w, h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val before = scaledBefore ?: return
        val after = scaledAfter ?: return
        if (width == 0 || height == 0) return

        imageRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(after, null, imageRect, imagePaint)

        val dividerX = width * sliderPosition
        val saveCount = canvas.save()
        canvas.clipRect(0f, 0f, dividerX, height.toFloat())
        canvas.drawBitmap(before, null, imageRect, imagePaint)
        canvas.restoreToCount(saveCount)

        canvas.drawLine(dividerX, 0f, dividerX, height.toFloat(), dividerPaint)

        val handleRadius = dp(16f)
        val centerY = height / 2f
        canvas.drawCircle(dividerX, centerY, handleRadius, handleFillPaint)
        canvas.drawCircle(dividerX, centerY, handleRadius, handleBorderPaint)

        drawHandleArrows(canvas, dividerX, centerY)
    }

    private fun drawHandleArrows(canvas: Canvas, centerX: Float, centerY: Float) {
        val arrowSize = dp(12f).toInt()
        val gap = dp(1f)
        val half = arrowSize / 2f

        leftArrow?.let { drawable ->
            val left = (centerX - gap - arrowSize).toInt()
            val top = (centerY - half).toInt()
            drawable.setBounds(left, top, left + arrowSize, top + arrowSize)
            drawable.draw(canvas)
        }
        rightArrow?.let { drawable ->
            val left = (centerX + gap).toInt()
            val top = (centerY - half).toInt()
            drawable.setBounds(left, top, left + arrowSize, top + arrowSize)
            drawable.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (width == 0) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                updateSliderFromX(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - downX)
                val dy = abs(event.y - downY)
                if (!isDragging && dx > touchSlop && dx >= dy) {
                    isDragging = true
                }
                if (isDragging || dx > touchSlop) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    updateSliderFromX(event.x)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging || abs(event.x - downX) > touchSlop) {
                    updateSliderFromX(event.x)
                }
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateSliderFromX(x: Float) {
        sliderPosition = (x / width).coerceIn(MIN_POSITION, MAX_POSITION)
        invalidate()
    }

    private fun rebuildScaledBitmaps(viewWidth: Int, viewHeight: Int) {
        val before = beforeBitmap ?: return
        val after = afterBitmap ?: return

        recycleScaledBitmaps()
        scaledBefore = createCenterCropBitmap(before, viewWidth, viewHeight)
        scaledAfter = createCenterCropBitmap(after, viewWidth, viewHeight)
    }

    private fun createCenterCropBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) {
            return source
        }

        val sourceAspect = source.width.toFloat() / source.height
        val targetAspect = targetWidth.toFloat() / targetHeight

        val srcWidth: Int
        val srcHeight: Int
        val srcX: Int
        val srcY: Int

        if (sourceAspect > targetAspect) {
            srcHeight = source.height
            srcWidth = (source.height * targetAspect).toInt()
            srcX = (source.width - srcWidth) / 2
            srcY = 0
        } else {
            srcWidth = source.width
            srcHeight = (source.width / targetAspect).toInt()
            srcX = 0
            srcY = (source.height - srcHeight) / 2
        }

        srcRect.set(srcX, srcY, srcX + srcWidth, srcY + srcHeight)
        dstRect.set(0, 0, targetWidth, targetHeight)
        return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawBitmap(source, srcRect, dstRect, imagePaint)
        }
    }

    private fun recycleScaledBitmaps() {
        scaledBefore?.let { if (it !== beforeBitmap) it.recycle() }
        scaledAfter?.let { if (it !== afterBitmap) it.recycle() }
        scaledBefore = null
        scaledAfter = null
    }

    override fun onDetachedFromWindow() {
        recycleScaledBitmaps()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    companion object {
        private const val MIN_POSITION = 0.02f
        private const val MAX_POSITION = 0.98f
    }
}
