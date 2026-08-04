package com.hairstyle.generator.aimakerover.ui.intro

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IntroReviewAutoScroller(
    private val recyclerView: RecyclerView
) {

    private var running = false
    private var cycleWidth = 0
    private var scrollAccumulator = 0f

    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (!running) return

            scrollAccumulator += SCROLL_SPEED_PX_PER_FRAME
            if (scrollAccumulator >= 1f) {
                val deltaPx = scrollAccumulator.toInt()
                scrollAccumulator -= deltaPx
                recyclerView.scrollBy(deltaPx, 0)

                if (cycleWidth > 0) {
                    val offset = recyclerView.computeHorizontalScrollOffset()
                    if (offset >= cycleWidth) {
                        recyclerView.scrollBy(-cycleWidth, 0)
                    }
                }
            }

            recyclerView.postOnAnimation(this)
        }
    }

    fun start() {
        stop()
        val adapter = recyclerView.adapter as? IntroReviewAdapter ?: return
        if (adapter.realItemCount <= 1) return

        running = true
        scrollAccumulator = 0f
        recyclerView.scrollToPosition(0)
        recyclerView.post {
            if (!running) return@post
            cycleWidth = measureCycleWidth(adapter.realItemCount)
            recyclerView.postOnAnimation(scrollRunnable)
        }
    }

    fun stop() {
        running = false
        recyclerView.removeCallbacks(scrollRunnable)
        recyclerView.stopScroll()
        scrollAccumulator = 0f
    }

    private fun measureCycleWidth(realCount: Int): Int {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return 0
        if (realCount <= 0) return 0

        val anchor = layoutManager.findViewByPosition(layoutManager.findFirstVisibleItemPosition())
            ?: return 0
        val itemWidth = layoutManager.getDecoratedMeasuredWidth(anchor)
        return itemWidth * realCount
    }

    companion object {
        private const val SCROLL_SPEED_PX_PER_FRAME = 1.4f
    }
}
