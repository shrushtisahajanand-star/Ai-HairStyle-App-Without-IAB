package com.hairstyle.generator.aimakerover.ui.gallery

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogPermissionRequiredBinding

object PermissionSettingsDialog {

    private var showingDialog: Dialog? = null

    fun show(
        fragment: Fragment,
        message: String = fragment.getString(R.string.permission_required_message),
        onAllow: () -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        if (!fragment.isAdded) return

        showingDialog?.dismiss()

        val context = fragment.requireContext()
        val binding = DialogPermissionRequiredBinding.inflate(fragment.layoutInflater)
//        binding.tvMessage.text = message

        val dialog = Dialog(context, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnAllow.setOnClickListener {
            dialog.dismiss()
            onAllow()
        }

        dialog.setOnDismissListener {
            if (showingDialog === dialog) {
                showingDialog = null
            }
            onDismiss()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        showingDialog = dialog
        dialog.show()
    }

    fun dismissIfShowing() {
        showingDialog?.dismiss()
        showingDialog = null
    }
}
