package com.hairstyle.generator.aimakerover.ui.intro

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.R

class IntroItemSpacingDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val spacing = view.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < state.itemCount - 1) {
            outRect.bottom = spacing
        }
    }
}
