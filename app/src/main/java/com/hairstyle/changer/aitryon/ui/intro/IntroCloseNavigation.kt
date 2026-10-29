package com.hairstyle.changer.aitryon.ui.intro

import android.app.Activity
import android.view.View
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.utils.IntroManager

internal fun Activity.setupIntroCloseButton(closeView: View) {
    closeView.setOnClickListener { handleIntroCloseClick() }
}

internal fun Activity.handleIntroCloseClick() {
    when (this) {
        is Intro2Activity, is Intro3Activity -> completeIntroAndGoHome()
        else -> Unit
    }
}

internal fun Activity.completeIntroAndGoHome() {
    IntroManager.setIntroCompleted()
    AppNavigator.openHomeClearTask(this)
}
