package com.hairstyle.changer.aitryon.utils

import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView

object PromoVideoCardSizer {

    /**
     * Height is driven by leftover space; width is derived from media aspect ratio
     * so the card does not stretch full-width ("H,w:h").
     */
    private const val FALLBACK_RATIO = "H,3:4"

    fun applyFallbackRatio(card: MaterialCardView) {
        card.post { updateRatio(card, FALLBACK_RATIO) }
    }

    fun applyAspectRatio(card: MaterialCardView, mediaWidth: Int, mediaHeight: Int) {
        if (mediaWidth <= 0 || mediaHeight <= 0) return
        val ratio = "H,$mediaWidth:$mediaHeight"
        card.post { updateRatio(card, ratio) }
    }

    private fun updateRatio(card: MaterialCardView, ratio: String) {
        val params = card.layoutParams as? ConstraintLayout.LayoutParams ?: return
        if (params.dimensionRatio == ratio) return
        params.width = 0
        params.height = 0
        params.dimensionRatio = ratio
        params.horizontalBias = 0.5f
        card.layoutParams = params
    }
}