package com.hairstyle.generator.aimakerover.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.generator.aimakerover.data.repository.CategoryImagesRepository
import com.hairstyle.generator.aimakerover.domain.model.CategoryHairstyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HairColorsUiState(
    val isLoading: Boolean = true,
    val items: List<CategoryHairstyle> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HairColorViewModel @Inject constructor(
    private val categoryImagesRepository: CategoryImagesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HairColorsUiState())
    val state: StateFlow<HairColorsUiState> = _state.asStateFlow()

    fun loadHairColors() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, items = emptyList()) }
            runCatching { categoryImagesRepository.loadHairColors(page = 1) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load hair colors",
                            items = emptyList()
                        )
                    }
                }
        }
    }
}
