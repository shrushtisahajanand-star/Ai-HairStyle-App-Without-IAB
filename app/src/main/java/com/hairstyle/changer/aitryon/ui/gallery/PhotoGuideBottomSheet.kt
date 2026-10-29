package com.hairstyle.changer.aitryon.ui.gallery

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.BottomSheetPhotoGuideBinding
import com.hairstyle.changer.aitryon.utils.PhotoGuideManager

class PhotoGuideBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPhotoGuideBinding? = null
    private val binding get() = _binding!!

    private val photoAction: PhotoAction by lazy {
        val value = arguments?.getString(ARG_ACTION).orEmpty()
        runCatching { PhotoAction.valueOf(value) }.getOrElse { PhotoAction.CAMERA }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPhotoGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnGotIt.setOnClickListener {
            PhotoGuideManager.incrementShowCount()
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(ARG_ACTION to photoAction.name)
            )
            dismiss()
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
        const val TAG = "PhotoGuideBottomSheet"
        const val RESULT_KEY = "photo_guide_result"
        const val ARG_ACTION = "photo_action"

        fun newInstance(action: PhotoAction): PhotoGuideBottomSheet {
            return PhotoGuideBottomSheet().apply {
                arguments = bundleOf(ARG_ACTION to action.name)
            }
        }
    }
}
