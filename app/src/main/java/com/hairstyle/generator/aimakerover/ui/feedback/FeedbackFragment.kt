package com.hairstyle.generator.aimakerover.ui.feedback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentFeedbackBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import com.hairstyle.generator.aimakerover.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FeedbackFragment : Fragment() {

    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedbackViewModel by viewModels()

    private var attachmentUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        setAttachment(uri)
    }

    /**
     * Opening the IME resizes the form and the scroll view auto-scrolls to the focused field.
     * Only a real finger drag should dismiss the keyboard, never that programmatic scroll.
     */
    private var userDraggingScroll = false

    private val hideKeyboardOnScrollListener =
        NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            if (userDraggingScroll) hideKeyboard()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.feedback())
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        binding.etMessage.isVerticalScrollBarEnabled = true
        binding.etMessage.setOnTouchListener { v, event ->
            // Allow message field to scroll its own content without stealing parent scroll.
            v.parent?.requestDisallowInterceptTouchEvent(true)
            if (event.action == MotionEvent.ACTION_UP) {
                v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
        binding.formScroll.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> userDraggingScroll = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> userDraggingScroll = false
            }
            false
        }
        binding.formScroll.setOnScrollChangeListener(hideKeyboardOnScrollListener)

        observeState()

        binding.btnAddImage.setOnClickListener {
            pickImage.launch("image/*")
        }
        binding.btnRemoveAttachment.setOnClickListener { clearAttachment() }
        binding.btnSubmit.setOnClickListener { onSubmitClicked() }
    }

    private fun hideKeyboard() {
        val focused = activity?.currentFocus ?: return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focused.windowToken, 0)
        focused.clearFocus()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.submitted) {
                        showSubmittedState()
                        return@collect
                    }
                    showFormState()
                    setSubmitting(state.isSubmitting)
                    state.errorMessage?.let { message ->
                        showSnackbar(message)
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun onSubmitClicked() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val message = binding.etMessage.text?.toString()?.trim().orEmpty()

        when {
            name.isBlank() -> {
                showSnackbar(getString(R.string.feedback_name_required))
                return
            }
            email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showSnackbar(getString(R.string.feedback_email_invalid))
                binding.etEmail.requestFocus()
                return
            }
            message.isBlank() -> {
                showSnackbar(getString(R.string.feedback_message_required))
                return
            }
        }

        viewModel.submit(
            title = name,
            description = message,
            mailId = email.takeIf { it.isNotBlank() },
            imageUri = attachmentUri
        )
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        binding.btnSubmit.isEnabled = !isSubmitting
        binding.btnSubmit.alpha = if (isSubmitting) 0.6f else 1f
        binding.etName.isEnabled = !isSubmitting
        binding.etEmail.isEnabled = !isSubmitting
        binding.etMessage.isEnabled = !isSubmitting
        binding.btnAddImage.isEnabled = !isSubmitting
        binding.btnRemoveAttachment.isEnabled = !isSubmitting
        if (!isSubmitting) {
            binding.btnAddImage.alpha = 1f
        }
    }

    private fun setAttachment(uri: Uri) {
        attachmentUri = uri
        binding.attachmentPreview.isVisible = true
        binding.ivAttachment.load(uri) { crossfade(true) }
        binding.tvAttachmentCount.setText(R.string.feedback_replace)
        binding.btnAddImage.isEnabled = true
        binding.btnAddImage.alpha = 1f
    }

    private fun clearAttachment() {
        attachmentUri = null
        binding.attachmentPreview.isVisible = false
        binding.ivAttachment.setImageDrawable(null)
        binding.tvAttachmentCount.setText(R.string.feedback_attachment_count)
        binding.btnAddImage.isEnabled = true
        binding.btnAddImage.alpha = 1f
    }

    private fun showFormState() {
        binding.formScroll.isVisible = true
        binding.btnSubmit.isVisible = true
        binding.submittedContainer.isVisible = false
    }

    private fun showSubmittedState() {
        binding.formScroll.isVisible = false
        binding.btnSubmit.isVisible = false
        binding.submittedContainer.isVisible = true
    }

    override fun onDestroyView() {
        binding.formScroll.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        binding.formScroll.setOnTouchListener(null)
        binding.etMessage.setOnTouchListener(null)
        userDraggingScroll = false
        super.onDestroyView()
        attachmentUri = null
        _binding = null
    }
}
