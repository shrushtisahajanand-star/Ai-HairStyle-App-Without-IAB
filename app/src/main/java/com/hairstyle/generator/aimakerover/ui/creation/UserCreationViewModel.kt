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

data class UserCreationUiState(
    val items: List<UserCreation> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class UserCreationViewModel @Inject constructor(
    private val userCreationRepository: UserCreationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserCreationUiState())
    val state: StateFlow<UserCreationUiState> = _state.asStateFlow()

    fun loadCreations() {
        viewModelScope.launch {
            _state.value = UserCreationUiState(
                items = userCreationRepository.getAllCreations(),
                isLoading = false
            )
        }
    }
}
