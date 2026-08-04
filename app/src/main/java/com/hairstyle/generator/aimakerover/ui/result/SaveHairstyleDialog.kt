package com.hairstyle.generator.aimakerover.ui.result

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogSaveHairstyleBinding
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager

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
        if (!fragment.isAdded) return

        val context = fragment.requireContext()
        val binding = DialogSaveHairstyleBinding.inflate(fragment.layoutInflater)

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
                    context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                } else {
                    0
                }
            }

        val dialog = Dialog(context, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
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
            val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialog.show()
    }
}
