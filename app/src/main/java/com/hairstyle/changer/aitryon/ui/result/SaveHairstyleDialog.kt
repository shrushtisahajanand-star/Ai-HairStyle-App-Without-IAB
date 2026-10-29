package com.hairstyle.changer.aitryon.ui.result

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import android.app.Activity
import androidx.fragment.app.Fragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.DialogSaveHairstyleBinding
import com.hairstyle.changer.aitryon.utils.PremiumPlanManager

object SaveHairstyleDialog {

    enum class Mode {
        SAVE_PHOTO,
        SAVE_HD
    }

    fun show(
        fragment: Fragment,
        mode: Mode,
        onFreeAdClick: () -> Unit = {},
        onPremiumClick: () -> Unit = {}
    ) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        show(activity, mode, onFreeAdClick, onPremiumClick)
    }

    fun show(
        activity: Activity,
        mode: Mode,
        onFreeAdClick: () -> Unit = {},
        onPremiumClick: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogSaveHairstyleBinding.inflate(activity.layoutInflater)

        // Firebase free limit (e.g. image_access = 3):
        // After last free generate, remaining == 0 → hide Watch Ad & Save.
        // Only Premium can download that result.
        // TEMP: client image_access disabled — always show Watch Ad for free users until
        // backend free-limit API is ready.
        // val freeQuotaLeft = PremiumPlanManager.getRemainingImageAccess() > 0
        val freeQuotaLeft = true
        val showWatchAdSave =
            !PremiumPlanManager.isPremiumActive() &&
                mode == Mode.SAVE_PHOTO &&
                freeQuotaLeft

        binding.btnFreeAd.isVisible = showWatchAdSave
        binding.tvMessage.setText(
            if (showWatchAdSave) {
                R.string.save_hairstyle_subtitle
            } else {
                R.string.save_hairstyle_subtitle_upgrade
            }
        )
        binding.btnUpgradePremium.layoutParams =
            (binding.btnUpgradePremium.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = if (showWatchAdSave) {
                    activity.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                } else {
                    0
                }
            }

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

        binding.btnUpgradePremium.setOnClickListener {
            dialog.dismiss()
            onPremiumClick()
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
