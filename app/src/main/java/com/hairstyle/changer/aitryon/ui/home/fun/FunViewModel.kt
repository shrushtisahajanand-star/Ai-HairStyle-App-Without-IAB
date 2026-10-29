package com.hairstyle.changer.aitryon.ui.home.`fun`

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.changer.aitryon.repository.CategoryImagesRepository
import com.hairstyle.changer.aitryon.domain.model.FunStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FunUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<FunStyle> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class FunViewModel @Inject constructor(
    private val categoryImagesRepository: CategoryImagesRepository
) : ViewModel() {

    private val tabStates = List(FunTab.COUNT) {
        MutableStateFlow(FunUiState(isLoading = true))
    }
    private val currentPages = IntArray(FunTab.COUNT)
    private val isTabLoaded = BooleanArray(FunTab.COUNT)

    private val _activeTab = MutableStateFlow(FunTab.MEN.position)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    init {
        loadForTab(FunTab.MEN.position)
    }

    fun tabState(tab: Int): StateFlow<FunUiState> = tabStates[tab].asStateFlow()

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
        loadForTab(tab)
    }

    fun loadForTab(tabPosition: Int) {
        if (isTabLoaded[tabPosition] && tabStates[tabPosition].value.items.isNotEmpty()) {
            return
        }
        currentPages[tabPosition] = 0

        viewModelScope.launch {
            tabStates[tabPosition].update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    items = emptyList(),
                    hasMore = true
                )
            }
            loadFunPage(tabPosition, page = 1, append = false)
        }
    }

    fun loadMoreForTab(tab: Int) {
        val state = tabStates[tab].value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            loadFunPage(tab, page = currentPages[tab] + 1, append = true)
        }
    }

    private suspend fun loadFunPage(tab: Int, page: Int, append: Boolean) {
        if (append) {
            tabStates[tab].update { it.copy(isLoadingMore = true, error = null) }
        }

        runCatching {
            when (tab) {
                FunTab.MEN.position -> categoryImagesRepository.loadFunMenStyles(page)
                FunTab.WOMEN.position -> categoryImagesRepository.loadFunWomenStyles(page)
                else -> categoryImagesRepository.loadFunBabyStyles(page)
            }
        }.onSuccess { result ->
            currentPages[tab] = result.page
            isTabLoaded[tab] = true
            tabStates[tab].update { current ->
                current.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    items = if (append) current.items + result.items else result.items,
                    hasMore = result.hasMore,
                    error = null
                )
            }
        }.onFailure { e ->
            tabStates[tab].update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load"
                )
            }
        }
    }
}

enum class FunTab(val position: Int) {
    MEN(0),
    WOMEN(1),
    BABY(2);

    companion object {
        const val COUNT = 3
    }
}
