package com.hairstyle.changer.aitryon.ui.result

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.DialogImageSavedBinding

object ImageSavedDialog {

    fun show(activity: Activity, onDismiss: () -> Unit = {}) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogImageSavedBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnDismissListener { onDismiss() }
        }

        binding.btnSavedOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialog.show()
    }
}
