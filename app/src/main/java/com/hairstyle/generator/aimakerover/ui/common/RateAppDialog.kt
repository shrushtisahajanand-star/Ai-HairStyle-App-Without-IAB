package com.hairstyle.generator.aimakerover.ui.common

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.DialogRateAppBinding
import com.hairstyle.generator.aimakerover.utils.InAppReviewHelper
import com.hairstyle.generator.aimakerover.utils.RateAppManager

object RateAppDialog {

    private const val AFTER_SAVE_DELAY_MS = 700L

    /** After gallery save toast — slight delay so toast is visible first. */
    fun showAfterSave(fragment: Fragment) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded || !RateAppManager.shouldShowAfterSave()) return
        val view = fragment.view ?: return
        view.postDelayed({
            if (!fragment.isAdded || fragment.view == null) return@postDelayed
            if (!RateAppManager.shouldShowAfterSave()) return@postDelayed
            show(fragment)
        }, AFTER_SAVE_DELAY_MS)
    }

    fun showAfterSave(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed || !RateAppManager.shouldShowAfterSave()) return
        val view = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        view.postDelayed({
            if (activity.isFinishing || activity.isDestroyed) return@postDelayed
            if (!RateAppManager.shouldShowAfterSave()) return@postDelayed
            show(activity)
        }, AFTER_SAVE_DELAY_MS)
    }

    /**
     * 4th / 8th / 12th app open — first Home / Fun / Beard / Select screen.
     * Skipped forever after user has submitted a rating.
     */
    fun showIfNeededForAppOpen(
        fragment: Fragment,
        onDismissedOrSkipped: (() -> Unit)? = null
    ): Boolean {
        if (!fragment.isAdded) {
            onDismissedOrSkipped?.invoke()
            return false
        }
        if (!RateAppManager.shouldShowAppOpenPrompt() || !RateAppManager.canShowDialog()) {
            onDismissedOrSkipped?.invoke()
            return false
        }
        return show(
            fragment = fragment,
            markAppOpenHandled = true,
            onDismissed = onDismissedOrSkipped
        )
    }

    fun showIfNeededForAppOpen(
        activity: Activity,
        onDismissedOrSkipped: (() -> Unit)? = null
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) {
            onDismissedOrSkipped?.invoke()
            return false
        }
        if (!RateAppManager.shouldShowAppOpenPrompt() || !RateAppManager.canShowDialog()) {
            onDismissedOrSkipped?.invoke()
            return false
        }
        return show(
            activity = activity,
            markAppOpenHandled = true,
            onDismissed = onDismissedOrSkipped
        )
    }

    fun show(
        fragment: Fragment,
        markAppOpenHandled: Boolean = false,
        onDismissed: (() -> Unit)? = null
    ): Boolean {
        if (!fragment.isAdded) {
            onDismissed?.invoke()
            return false
        }
        if (!RateAppManager.canShowDialog()) {
            onDismissed?.invoke()
            return false
        }

        val context = fragment.requireContext()
        val binding = DialogRateAppBinding.inflate(fragment.layoutInflater)
        var selectedRating = 0
        var dismissedCallbackFired = false

        fun fireDismissed() {
            if (dismissedCallbackFired) return
            dismissedCallbackFired = true
            onDismissed?.invoke()
        }

        val stars = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )

        fun updateStars(rating: Int) {
            selectedRating = rating
            stars.forEachIndexed { index, star ->
                star.setImageResource(
                    if (index < rating) R.drawable.ic_rate_star_filled
                    else R.drawable.ic_rate_star_empty
                )
            }
        }

        val dialog = Dialog(context, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnDismissListener {
                RateAppManager.markDialogVisible(false)
                fireDismissed()
            }
        }

        stars.forEachIndexed { index, star ->
            star.setOnClickListener { updateStars(index + 1) }
        }

        binding.btnClose.setOnClickListener { dialog.dismiss() }
        binding.btnMaybeLater.setOnClickListener { dialog.dismiss() }
        binding.btnSubmit.setOnClickListener {
            if (selectedRating <= 0) {
                Toast.makeText(
                    context,
                    R.string.rate_dialog_select_stars,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            // Persist: Play cannot tell us if they reviewed — never ask again after Submit.
            RateAppManager.markUserRated()
            dialog.dismiss()
            val activity = fragment.activity
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                InAppReviewHelper.launch(activity)
            }
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes?.apply { dimAmount = 0.72f }
        }

        val activity = fragment.activity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onDismissed?.invoke()
            return false
        }
        if (markAppOpenHandled) {
            RateAppManager.markAppOpenPromptHandled()
        }
        RateAppManager.markDialogVisible(true)
        dialog.show()
        return true
    }

    fun show(
        activity: Activity,
        markAppOpenHandled: Boolean = false,
        onDismissed: (() -> Unit)? = null
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) {
            onDismissed?.invoke()
            return false
        }
        if (!RateAppManager.canShowDialog()) {
            onDismissed?.invoke()
            return false
        }

        val binding = DialogRateAppBinding.inflate(activity.layoutInflater)
        var selectedRating = 0
        var dismissedCallbackFired = false

        fun fireDismissed() {
            if (dismissedCallbackFired) return
            dismissedCallbackFired = true
            onDismissed?.invoke()
        }

        val stars = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )

        fun updateStars(rating: Int) {
            selectedRating = rating
            stars.forEachIndexed { index, star ->
                star.setImageResource(
                    if (index < rating) R.drawable.ic_rate_star_filled
                    else R.drawable.ic_rate_star_empty
                )
            }
        }

        val dialog = Dialog(activity, R.style.ThemeOverlay_AIHairstyleStudio_PermissionDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnDismissListener {
                RateAppManager.markDialogVisible(false)
                fireDismissed()
            }
        }

        stars.forEachIndexed { index, star ->
            star.setOnClickListener { updateStars(index + 1) }
        }

        binding.btnClose.setOnClickListener { dialog.dismiss() }
        binding.btnMaybeLater.setOnClickListener { dialog.dismiss() }
        binding.btnSubmit.setOnClickListener {
            if (selectedRating <= 0) {
                Toast.makeText(
                    activity,
                    R.string.rate_dialog_select_stars,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            RateAppManager.markUserRated()
            dialog.dismiss()
            if (!activity.isFinishing && !activity.isDestroyed) {
                InAppReviewHelper.launch(activity)
            }
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes?.apply { dimAmount = 0.72f }
        }

        if (markAppOpenHandled) {
            RateAppManager.markAppOpenPromptHandled()
        }
        RateAppManager.markDialogVisible(true)
        dialog.show()
        return true
    }
}
