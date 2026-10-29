package com.hairstyle.changer.aitryon.ui.gallery

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.DialogPermissionRequiredBinding

object PermissionSettingsDialog {

    private var showingDialog: Dialog? = null

    fun show(
        fragment: Fragment,
        message: String = fragment.getString(R.string.permission_required_message),
        onAllow: () -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        show(activity, message, onAllow, onDismiss)
    }

    fun show(
        activity: Activity,
        message: String = activity.getString(R.string.permission_required_message),
        onAllow: () -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        showingDialog?.dismiss()

        val binding = DialogPermissionRequiredBinding.inflate(activity.layoutInflater)
//        binding.tvMessage.text = message

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
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
            val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
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
