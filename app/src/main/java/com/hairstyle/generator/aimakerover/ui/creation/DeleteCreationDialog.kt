package com.hairstyle.generator.aimakerover.ui.creation

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogDeleteCreationBinding

object DeleteCreationDialog {

    fun show(
        fragment: Fragment,
        onConfirm: () -> Unit
    ) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        show(activity, onConfirm)
    }

    fun show(
        activity: Activity,
        onConfirm: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogDeleteCreationBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

        binding.btnYes.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        binding.btnNo.setOnClickListener {
            dialog.dismiss()
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
