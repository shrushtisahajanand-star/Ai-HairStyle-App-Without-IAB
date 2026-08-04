package com.hairstyle.generator.aimakerover.ui.progress

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogSomethingWentWrongBinding

/**
 * Shown when generate API fails / times out / success=false.
 * Only Cancel — navigates back to Home.
 */
object SomethingWentWrongDialog {

    fun show(fragment: Fragment, onCancel: () -> Unit) {
        if (!fragment.isAdded) return

        val context = fragment.requireContext()
        val binding = DialogSomethingWentWrongBinding.inflate(fragment.layoutInflater)

        val dialog = Dialog(context, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            }
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes?.apply { dimAmount = 0.72f }
        }

        dialog.show()
    }
}
