package com.hairstyle.changer.aitryon.ui.common

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.DialogDailyQuotaFinishedBinding

/** Shown when the server daily free limit is exhausted. */
object DailyQuotaFinishedDialog {

    fun show(fragment: Fragment, onDismiss: (() -> Unit)? = null) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        show(activity, onDismiss)
    }

    fun show(activity: Activity, onDismiss: (() -> Unit)? = null) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogDailyQuotaFinishedBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnDismissListener { onDismiss?.invoke() }
        }

        binding.btnOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes?.apply { dimAmount = 0.72f }
        }

        dialog.show()
    }
}
