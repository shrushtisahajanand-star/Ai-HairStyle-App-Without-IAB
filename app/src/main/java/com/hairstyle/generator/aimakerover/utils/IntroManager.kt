package com.hairstyle.generator.aimakerover.utils

object IntroManager {

    const val KEY_INTRO_COMPLETED = "intro_completed"

    fun isIntroCompleted(): Boolean {
        if (!PrefManager.isInitialized()) return false
        return PrefManager.getBoolean(KEY_INTRO_COMPLETED, false)
    }

    fun setIntroCompleted() {
        PrefManager.putBoolean(KEY_INTRO_COMPLETED, true)
    }
}
