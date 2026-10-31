package com.hairstyle.changer.aitryon.ui.common

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.DialogUnlockItemBinding

/** "Watch ads to unlock this style" — shown when a locked item is tapped. */
object UnlockItemDialog {

    /** Only one unlock dialog at a time (fast double taps must not stack them). */
    private var current: Dialog? = null

    fun show(activity: Activity, onWatchAds: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (current?.isShowing == true) return

        val binding = DialogUnlockItemBinding.inflate(activity.layoutInflater)
        binding.tvUnlockMessage.setText(R.string.unlock_dialog_message_one)

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

        binding.btnUnlockWatch.setOnClickListener {
            dialog.dismiss()
            onWatchAds()
        }
        binding.btnUnlockCancel.setOnClickListener { dialog.dismiss() }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        current = dialog
        dialog.setOnDismissListener { if (current === dialog) current = null }
        dialog.show()
    }
}
