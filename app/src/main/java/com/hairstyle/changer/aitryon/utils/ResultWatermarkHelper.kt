package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.hairstyle.changer.aitryon.R
import kotlin.math.roundToInt

/**
 * Draws the free-user watermark (icon + Magic/Hair) onto a result bitmap before save/share.
 */
object ResultWatermarkHelper {

    private const val WATERMARK_ALPHA = 0.65f
    private const val MARGIN_RATIO = 0.035f
    /** Slightly larger so [ic_watermark] stays readable next to text. */
    private const val ICON_RATIO = 0.08f
    private const val TEXT_SIZE_RATIO = 0.038f
    private const val GAP_RATIO = 0.012f
    private const val LABEL_GAP_PX = 2f

    /**
     * Returns a new bitmap with the watermark drawn on, or [source] unchanged if it cannot be copied.
     * Caller must recycle the returned bitmap when it is different from [source].
     */
    fun applyWatermark(context: Context, source: Bitmap): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
        val canvas = Canvas(mutable)
        val alpha = (WATERMARK_ALPHA * 255).roundToInt().coerceIn(0, 255)
        val textSize = (source.width * TEXT_SIZE_RATIO).coerceAtLeast(18f)

        val iconSize = (source.width * ICON_RATIO).roundToInt().coerceAtLeast(48)
        val gap = (source.width * GAP_RATIO).roundToInt().coerceAtLeast(8)
        val margin = (source.width * MARGIN_RATIO).roundToInt().coerceAtLeast(12)

        val icon = loadIconBitmap(context, iconSize)
        val label = "Magic"
        val label2 = "Hair"

        val medium = loadTypeface(context, R.font.saans_medium)
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val regular = loadTypeface(context, R.font.saans_regular)
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        val paintMagic = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            this.alpha = alpha
            this.textSize = textSize
            typeface = medium
        }
        val paintHair = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            this.alpha = alpha
            this.textSize = textSize
            typeface = regular
        }

        val magicWidth = paintMagic.measureText(label)
        val hairWidth = paintHair.measureText(label2)
        val textWidth = magicWidth + LABEL_GAP_PX + hairWidth
        val textHeight = maxOf(
            paintMagic.fontMetrics.let { it.descent - it.ascent },
            paintHair.fontMetrics.let { it.descent - it.ascent }
        )

        val contentWidth = (icon?.width ?: 0) + (if (icon != null) gap else 0) + textWidth
        val contentHeight = maxOf(icon?.height?.toFloat() ?: 0f, textHeight)

        val left = source.width - margin - contentWidth
        val top = source.height - margin - contentHeight

        if (icon != null) {
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.alpha = alpha
            }
            val iconTop = top + (contentHeight - icon.height) / 2f
            canvas.drawBitmap(icon, left, iconTop, iconPaint)
            icon.recycle()
        }

        val textX = left + (icon?.width ?: 0) + (if (icon != null) gap else 0)
        val textY = top + (contentHeight + textHeight) / 2f - paintMagic.fontMetrics.descent
        canvas.drawText(label, textX, textY, paintMagic)
        canvas.drawText(label2, textX + magicWidth + LABEL_GAP_PX, textY, paintHair)

        return mutable
    }

    private fun loadTypeface(context: Context, fontRes: Int): Typeface? =
        runCatching { ResourcesCompat.getFont(context, fontRes) }.getOrNull()

    /**
     * Always rasterizes [R.drawable.ic_watermark] into a fresh bitmap.
     * Avoids BitmapDrawable.getBitmap() null / shared-recycle issues with PNG.
     */
    private fun loadIconBitmap(context: Context, sizePx: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_watermark)
            ?.mutate()
            ?: return null

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }
}
