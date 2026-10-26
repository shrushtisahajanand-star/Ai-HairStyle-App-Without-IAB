package com.hairstyle.generator.aimakerover.ui.gallery

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogFaceDetectionFailedBinding

object FaceDetectionFailedDialog {

    enum class Action {
        CHOOSE_ANOTHER,
        TRY_AGAIN
    }

    fun show(
        fragment: Fragment,
        onAction: (Action) -> Unit
    ) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        show(activity, onAction)
    }

    fun show(
        activity: Activity,
        onAction: (Action) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogFaceDetectionFailedBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

        binding.btnChooseAnother.setOnClickListener {
            dialog.dismiss()
            onAction(Action.CHOOSE_ANOTHER)
        }

        binding.btnTryAgain.setOnClickListener {
            dialog.dismiss()
            onAction(Action.TRY_AGAIN)
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
