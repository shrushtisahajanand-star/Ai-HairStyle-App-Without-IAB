package com.hairstyle.generator.aimakerover.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.generator.aimakerover.data.local.PreferencesManager
import com.hairstyle.generator.aimakerover.domain.model.GenerationSession
import com.hairstyle.generator.aimakerover.domain.model.HairColor
import com.hairstyle.generator.aimakerover.domain.model.Hairstyle
import com.hairstyle.generator.aimakerover.domain.model.LoadingStage
import com.hairstyle.generator.aimakerover.domain.model.LoadingStages
import com.hairstyle.generator.aimakerover.domain.model.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoadingUiState(
    val progress: Int = 0,
    val currentStage: LoadingStage = LoadingStages.stages.first(),
    val stageIndex: Int = 0,
    val isAnimating: Boolean = false
)

@HiltViewModel
class GenerationViewModel @Inject constructor(
    // Old ailabapi path (BuildConfig.API_KEY) — unused; generate uses LocalHairstyleRepository.
    // private val generateHairstyleUseCase: GenerateHairstyleUseCase,
    // private val repository: HairstyleRepository,
    // private val resultImageHelper: ResultImageHelper,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _session = MutableStateFlow(GenerationSession())
    val session: StateFlow<GenerationSession> = _session.asStateFlow()

    private val _generationState = MutableStateFlow<ResultState<String>>(ResultState.Idle)
    val generationState: StateFlow<ResultState<String>> = _generationState.asStateFlow()

    private val _loadingState = MutableStateFlow(LoadingUiState())
    val loadingState: StateFlow<LoadingUiState> = _loadingState.asStateFlow()

    private val _recentPhotos = MutableStateFlow<List<String>>(emptyList())
    val recentPhotos: StateFlow<List<String>> = _recentPhotos.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.getRecentPhotos().collect { photos ->
                _recentPhotos.value = photos
            }
        }
    }

    fun setOriginalImage(uri: Uri, compressedPath: String) {
        _session.update {
            it.copy(
                originalImageUri = uri,
                compressedImagePath = compressedPath
            )
        }
        viewModelScope.launch {
            preferencesManager.addRecentPhoto(uri.toString())
        }
    }

    fun setHairstyle(hairstyle: Hairstyle) {
        _session.update { it.copy(selectedHairstyle = hairstyle) }
    }

    fun setHairColor(color: HairColor) {
        _session.update { it.copy(selectedColor = color) }
    }

    fun updateResultImagePath(path: String) {
        _session.update { it.copy(resultImagePath = path) }
    }

    fun setLocalGenerateResult(resultImageUrl: String, generationTimeMs: Long) {
        _session.update {
            it.copy(
                resultImageUrl = resultImageUrl,
                generationTimeMs = generationTimeMs
            )
        }
        _generationState.value = ResultState.Success(resultImageUrl)
    }

    fun resetGenerationState() {
        _generationState.value = ResultState.Idle
        _loadingState.value = LoadingUiState()
    }

    fun clearSession() {
        _session.value = GenerationSession()
        _generationState.value = ResultState.Idle
        _loadingState.value = LoadingUiState()
    }

    // companion object {
    //     private const val STAGE_DURATION_MS = 2_500L
    // }
}
