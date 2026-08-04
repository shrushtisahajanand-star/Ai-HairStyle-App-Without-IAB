package com.hairstyle.generator.aimakerover.ui.home

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import com.hairstyle.generator.aimakeover.R

class HeroGradientDotsIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dots = mutableListOf<View>()
    private var dotCount = 0
    private var selectedIndex = 0
    private var widthAnimator: ValueAnimator? = null

    private val inactiveWidthPx = dpToPx(INACTIVE_WIDTH_DP)
    private val activeWidthPx = dpToPx(ACTIVE_WIDTH_DP)
    private val dotHeightPx = dpToPx(DOT_HEIGHT_DP)
    private val dotSpacingPx = dpToPx(DOT_SPACING_DP)

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }

    fun setDotCount(count: Int) {
        if (count == dotCount) return
        dotCount = count
        selectedIndex = 0
        removeAllViews()
        dots.clear()
        widthAnimator?.cancel()

        if (count <= 1) {
            visibility = GONE
            return
        }
        visibility = VISIBLE

        repeat(count) { index ->
            val dot = View(context).apply {
                layoutParams = LayoutParams(inactiveWidthPx, dotHeightPx).apply {
                    if (index < count - 1) marginEnd = dotSpacingPx
                }
                setBackgroundResource(
                    if (index == selectedIndex) R.drawable.bg_hero_dot_active
                    else R.drawable.bg_hero_dot_inactive
                )
            }
            addView(dot)
            dots.add(dot)
        }
        applySelection(selectedIndex, animate = false)
    }

    fun setSelectedIndex(index: Int, animate: Boolean = true) {
        if (dots.isEmpty() || index == selectedIndex) return
        if (index !in dots.indices) return
        selectedIndex = index
        applySelection(index, animate)
    }

    private fun applySelection(index: Int, animate: Boolean) {
        widthAnimator?.cancel()

        dots.forEachIndexed { dotIndex, dot ->
            val isSelected = dotIndex == index
            val targetWidth = if (isSelected) activeWidthPx else inactiveWidthPx
            dot.setBackgroundResource(
                if (isSelected) R.drawable.bg_hero_dot_active
                else R.drawable.bg_hero_dot_inactive
            )

            val params = dot.layoutParams as LayoutParams
            if (!animate) {
                params.width = targetWidth
                dot.layoutParams = params
                return@forEachIndexed
            }

            ValueAnimator.ofInt(params.width, targetWidth).apply {
                duration = ANIMATION_DURATION_MS
                addUpdateListener { animator ->
                    params.width = animator.animatedValue as Int
                    dot.layoutParams = params
                }
                if (dotIndex == index) {
                    widthAnimator = this
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        widthAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        private const val ACTIVE_WIDTH_DP = 22
        private const val INACTIVE_WIDTH_DP = 8
        private const val DOT_HEIGHT_DP = 4
        private const val DOT_SPACING_DP = 6
        private const val ANIMATION_DURATION_MS = 220L
    }
}
