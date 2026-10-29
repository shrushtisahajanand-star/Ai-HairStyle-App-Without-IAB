package com.hairstyle.changer.aitryon.ui.feedback

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.changer.aitryon.repository.FeedbackRepository
import com.hairstyle.changer.aitryon.utils.PrefManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isSubmitting: Boolean = false,
    val submitted: Boolean = PrefManager.isFeedbackSubmitted(),
    val errorMessage: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun submit(title: String, description: String, mailId: String?, imageUri: Uri?) {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, errorMessage = null)
            val result = feedbackRepository.submitFeedback(
                title = title,
                description = description,
                mailId = mailId,
                imageUri = imageUri
            )
            result.fold(
                onSuccess = {
                    PrefManager.setFeedbackSubmitted(true)
                    _state.value = FeedbackUiState(isSubmitting = false, submitted = true)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to submit feedback"
                    )
                }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
