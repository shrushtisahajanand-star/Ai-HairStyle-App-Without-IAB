package com.hairstyle.changer.aitryon.ui.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeHeroAutoScroller(
    private val viewPager: ViewPager2,
    private val lifecycleOwner: LifecycleOwner,
    private val intervalMs: Long = 3500L
) {
    private var job: Job? = null
    private var pageCount: Int = 0

    fun start(count: Int) {
        pageCount = count
        if (pageCount <= 1) {
            stop()
            return
        }
        stop()
        job = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(intervalMs)
                    if (pageCount <= 1) return@repeatOnLifecycle
                    val next = (viewPager.currentItem + 1) % pageCount
                    viewPager.setCurrentItem(next, true)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
