package com.hairstyle.changer.aitryon.utils

object PhotoGuideManager {

    private const val KEY_SHOW_COUNT = "photo_guide_show_count"
    private const val MAX_SHOW_COUNT = 3

    fun shouldShowGuide(): Boolean {
        return PrefManager.getInt(KEY_SHOW_COUNT, 0) < MAX_SHOW_COUNT
    }

    fun incrementShowCount() {
        val current = PrefManager.getInt(KEY_SHOW_COUNT, 0)
        if (current < MAX_SHOW_COUNT) {
            PrefManager.putInt(KEY_SHOW_COUNT, current + 1)
        }
    }
}
