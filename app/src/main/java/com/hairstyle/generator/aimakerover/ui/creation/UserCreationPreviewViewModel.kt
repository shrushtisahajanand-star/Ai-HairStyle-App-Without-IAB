package com.hairstyle.generator.aimakerover.ui.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.generator.aimakerover.data.repository.UserCreationRepository
import com.hairstyle.generator.aimakerover.domain.model.UserCreation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserCreationPreviewUiState(
    val creation: UserCreation? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false
)

@HiltViewModel
class UserCreationPreviewViewModel @Inject constructor(
    private val userCreationRepository: UserCreationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserCreationPreviewUiState())
    val state: StateFlow<UserCreationPreviewUiState> = _state.asStateFlow()

    fun load(creationId: String) {
        viewModelScope.launch {
            _state.value = UserCreationPreviewUiState(
                creation = userCreationRepository.getCreation(creationId),
                isLoading = false
            )
        }
    }

    fun delete(creationId: String) {
        viewModelScope.launch {
            val deleted = userCreationRepository.deleteCreation(creationId)
            _state.value = _state.value.copy(deleted = deleted)
        }
    }
}
