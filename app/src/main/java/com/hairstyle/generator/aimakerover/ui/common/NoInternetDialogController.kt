package com.hairstyle.generator.aimakerover.ui.common

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogNoInternetBinding
import com.hairstyle.generator.aimakerover.utils.NetworkMonitor

/**
 * Non-cancelable no-internet dialog. Stays until network is restored
 * (auto via [NetworkMonitor] or manual Try Again).
 */
class NoInternetDialogController(
    private val activity: Activity,
    private val networkMonitor: NetworkMonitor
) {
    private var dialog: Dialog? = null

    fun showIfNeeded() {
        if (activity.isFinishing || activity.isDestroyed) return

        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread

            if (networkMonitor.isNetworkAvailable()) {
                dismissInternal()
                return@runOnUiThread
            }
            if (dialog?.isShowing == true) return@runOnUiThread

            // Log.d(TAG, "Showing no-internet dialog")
            val binding = DialogNoInternetBinding.inflate(LayoutInflater.from(activity))
            val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
                setContentView(binding.root)
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                setOnKeyListener { _, keyCode, event ->
                    keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
                }
            }

            binding.btnTryAgain.setOnClickListener { button ->
                playClickEffect(button)
                networkMonitor.refresh()
                if (networkMonitor.isNetworkAvailable()) {
                    dismissInternal()
                } else {
                    Toast.makeText(
                        activity,
                        R.string.no_internet_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
                setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes?.apply {
                    dimAmount = 0.72f
                }
            }

            this.dialog = dialog
            dialog.show()
        }
    }

    fun dismiss() {
        activity.runOnUiThread { dismissInternal() }
    }

    private fun dismissInternal() {
        dialog?.takeIf { it.isShowing }?.dismiss()
        dialog = null
    }

    fun onConnectivityChanged(isOnline: Boolean) {
        // Log.d(TAG, "onConnectivityChanged online=$isOnline")
        if (isOnline) {
            dismiss()
        } else {
            showIfNeeded()
        }
    }

    fun release() {
        dismissInternal()
    }

    private fun playClickEffect(view: View) {
        view.animate().cancel()
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80L)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80L)
                    .start()
            }
            .start()
    }

    companion object {
        private const val TAG = "NoInternetDialog"
    }
}
