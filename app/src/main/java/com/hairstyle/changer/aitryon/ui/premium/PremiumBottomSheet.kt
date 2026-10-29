package com.hairstyle.changer.aitryon.ui.premium

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Outline
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.BottomSheetPremiumBinding
import com.hairstyle.changer.aitryon.utils.IntroVideoHelper
import com.hairstyle.changer.aitryon.utils.VideoTitleKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class PremiumBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPremiumBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var okHttpClient: OkHttpClient

    private var onCancelClick: (() -> Unit)? = null
    private var onPremiumClick: (() -> Unit)? = null
    private var videoPrepared = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnUpgradePremium.setOnClickListener {
            val cb = onPremiumClick
            dismissAllowingStateLoss()
            cb?.invoke()
        }
        binding.btnCancel.setOnClickListener {
            val cb = onCancelClick
            dismissAllowingStateLoss()
            cb?.invoke()
        }
        binding.collageContainer.post { applyTopRoundedClip(binding.collageContainer) }
        loadSheetVideo()
    }

    private fun loadSheetVideo() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            IntroVideoHelper.preload(
                requireContext().applicationContext,
                okHttpClient,
                VideoTitleKeys.PREMIUM_SHEET
            )
        }
        playSheetVideo()
    }

    private fun playSheetVideo() {
        if (videoPrepared) {
            IntroVideoHelper.resume(binding.ivPremiumSheetVideo)
            return
        }
        IntroVideoHelper.play(
            webpView = binding.ivPremiumSheetVideo,
            shimmerLayout = binding.shimmerPremiumSheetVideo,
            videoTitleKey = VideoTitleKeys.PREMIUM_SHEET,
            fallbackImageRes = R.drawable.home_image,
            onPrepared = { _, _ -> videoPrepared = true }
        )
    }

    private fun applyTopRoundedClip(view: View) {
        val radius = resources.getDimension(com.intuit.sdp.R.dimen._24sdp)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(outlineView: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    outlineView.width,
                    outlineView.height + radius.toInt(),
                    radius
                )
            }
        }
        view.clipToOutline = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                // Block back dismiss
            }
        }.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            }
            setOnShowListener { dialogInterface ->
                val bottomSheet = (dialogInterface as BottomSheetDialog)
                    .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

                val sheetHeight = (resources.displayMetrics.heightPixels * SHEET_HEIGHT_FRACTION).toInt()
                bottomSheet.layoutParams.height = sheetHeight
                bottomSheet.setBackgroundResource(R.drawable.bg_photo_guide_bottom_sheet)
                applyTopRoundedClip(bottomSheet)

                BottomSheetBehavior.from(bottomSheet).apply {
                    peekHeight = sheetHeight
                    maxHeight = sheetHeight
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isDraggable = false
                    isHideable = false
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) = Unit

    override fun onPause() {
        _binding?.ivPremiumSheetVideo?.let { IntroVideoHelper.pause(it) }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (videoPrepared) {
            _binding?.ivPremiumSheetVideo?.let { IntroVideoHelper.resume(it) }
        }
    }

    override fun onDestroyView() {
        _binding?.ivPremiumSheetVideo?.let { IntroVideoHelper.release(it) }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PremiumBottomSheet"
        private const val SHEET_HEIGHT_FRACTION = 0.75f

        fun newInstance(
            onCancelClick: () -> Unit,
            onPremiumClick: () -> Unit = {}
        ): PremiumBottomSheet {
            return PremiumBottomSheet().apply {
                this.onCancelClick = onCancelClick
                this.onPremiumClick = onPremiumClick
            }
        }
    }
}
