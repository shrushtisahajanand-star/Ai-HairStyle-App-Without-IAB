package com.hairstyle.changer.aitryon.ui.intro

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import com.hairstyle.changer.aitryon.R

class IntroDotsIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dots = mutableListOf<View>()
    private var dotCount = 0
    private var selectedIndex = 0

    private val dotSizePx = dpToPx(DOT_SIZE_DP)
    private val dotSpacingPx = dpToPx(DOT_SPACING_DP)

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER
    }

    fun setDotCount(count: Int) {
        if (count == dotCount) return
        dotCount = count
        selectedIndex = 0
        removeAllViews()
        dots.clear()

        if (count <= 1) {
            visibility = GONE
            return
        }
        visibility = VISIBLE

        repeat(count) { index ->
            val dot = View(context).apply {
                layoutParams = LayoutParams(dotSizePx, dotSizePx).apply {
                    if (index < count - 1) marginEnd = dotSpacingPx
                }
                setBackgroundResource(
                    if (index == selectedIndex) R.drawable.bg_intro_dot_active
                    else R.drawable.bg_intro_dot_inactive
                )
            }
            addView(dot)
            dots.add(dot)
        }
    }

    fun setSelectedIndex(index: Int) {
        if (dots.isEmpty() || index !in dots.indices || index == selectedIndex) return
        selectedIndex = index
        dots.forEachIndexed { dotIndex, dot ->
            dot.setBackgroundResource(
                if (dotIndex == index) R.drawable.bg_intro_dot_active
                else R.drawable.bg_intro_dot_inactive
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        private const val DOT_SIZE_DP = 6
        private const val DOT_SPACING_DP = 8
    }
}
