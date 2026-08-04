package com.hairstyle.generator.aimakerover.ui.home

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogExitAppBinding

object ExitAppDialog {

    fun show(fragment: Fragment, onExit: () -> Unit) {
        if (!fragment.isAdded) return

        val context = fragment.requireContext()
        val binding = DialogExitAppBinding.inflate(fragment.layoutInflater)

        val dialog = Dialog(context, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnExit.setOnClickListener {
            dialog.dismiss()
            onExit()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialog.show()
    }
}
