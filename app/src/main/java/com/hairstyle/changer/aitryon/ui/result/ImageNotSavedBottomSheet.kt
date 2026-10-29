package com.hairstyle.changer.aitryon.ui.result

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.BottomSheetImageNotSavedBinding

class ImageNotSavedBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetImageNotSavedBinding? = null
    private val binding get() = _binding!!

    private var onSaveClick: (() -> Unit)? = null
    private var onLeaveClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetImageNotSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSave.setOnClickListener {
            dismissAllowingStateLoss()
            onSaveClick?.invoke()
        }

        binding.btnLeave.setOnClickListener {
            dismissAllowingStateLoss()
            onLeaveClick?.invoke()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener { dialogInterface ->
                val bottomSheet = (dialogInterface as BottomSheetDialog)
                    .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

                bottomSheet.setBackgroundResource(R.drawable.bg_photo_guide_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ImageNotSavedBottomSheet"

        fun newInstance(
            onSaveClick: () -> Unit,
            onLeaveClick: () -> Unit
        ): ImageNotSavedBottomSheet {
            return ImageNotSavedBottomSheet().apply {
                this.onSaveClick = onSaveClick
                this.onLeaveClick = onLeaveClick
            }
        }
    }
}
