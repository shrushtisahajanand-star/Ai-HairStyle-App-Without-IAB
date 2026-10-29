package com.hairstyle.changer.aitryon.ui.result

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.DialogSaveHairstyleBinding

object SaveHairstyleDialog {

    enum class Mode {
        SAVE_PHOTO,
        SAVE_HD
    }

    fun show(
        fragment: Fragment,
        mode: Mode,
        onFreeAdClick: () -> Unit = {}
    ) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        show(activity, mode, onFreeAdClick)
    }

    fun show(
        activity: Activity,
        mode: Mode,
        onFreeAdClick: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogSaveHairstyleBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        binding.btnFreeAd.setOnClickListener {
            dialog.dismiss()
            onFreeAdClick()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialog.show()
    }
}
