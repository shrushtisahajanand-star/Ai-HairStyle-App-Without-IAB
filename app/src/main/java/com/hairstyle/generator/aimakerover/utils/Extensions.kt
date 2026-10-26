package com.hairstyle.generator.aimakerover.utils

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun View.showSnackbar(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    if (actionLabel != null && action != null) {
        snackbar.setAction(actionLabel, { action() })
    }
    snackbar.show()
}

fun Fragment.showSnackbar(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
    view?.showSnackbar(message, actionLabel, action)
}

fun Activity.showSnackbar(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
    findViewById<View>(android.R.id.content)?.showSnackbar(message, actionLabel, action)
}

fun Long.formatGenerationTime(): String {
    val seconds = this / 1000.0
    return if (seconds < 60) {
        String.format(java.util.Locale.US, "%.1fs", seconds)
    } else {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        "${mins}m ${secs}s"
    }
}
