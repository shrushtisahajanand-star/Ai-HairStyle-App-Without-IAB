package com.hairstyle.generator.aimakerover.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.generator.aimakerover.data.model.PagedCategoryImages
import com.hairstyle.generator.aimakerover.data.repository.CategoryImagesRepository
import com.hairstyle.generator.aimakerover.domain.model.CategoryHairstyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectCategoryUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<CategoryHairstyle> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class SelectCategoryViewModel @Inject constructor(
    private val categoryImagesRepository: CategoryImagesRepository
) : ViewModel() {

    private val tabStates = List(SelectCategoryPagerAdapter.TAB_COUNT) {
        MutableStateFlow(SelectCategoryUiState(isLoading = true))
    }
    private val selectedByTab = List(SelectCategoryPagerAdapter.TAB_COUNT) {
        MutableStateFlow<CategoryHairstyle?>(null)
    }
    private val currentPages = IntArray(SelectCategoryPagerAdapter.TAB_COUNT)
    private val isTabLoaded = BooleanArray(SelectCategoryPagerAdapter.TAB_COUNT)

    private val _hairColors = MutableStateFlow(SelectCategoryUiState())
    val hairColors: StateFlow<SelectCategoryUiState> = _hairColors.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun tabState(tab: Int): StateFlow<SelectCategoryUiState> = tabStates[tab].asStateFlow()

    fun selectedForTab(tab: Int): StateFlow<CategoryHairstyle?> = selectedByTab[tab].asStateFlow()

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun selectHairstyle(tab: Int, item: CategoryHairstyle?) {
        selectedByTab[tab].value = item
    }

    fun loadForTab(tabPosition: Int) {
        if (isTabLoaded[tabPosition] && tabStates[tabPosition].value.items.isNotEmpty()) {
            tabStates[tabPosition].update {
                it.copy(isLoading = false, isLoadingMore = false, error = null)
            }
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
            loadHairstylesPage(tabPosition, page = 1, append = false)
        }
    }

    fun loadMoreForTab(tab: Int) {
        val state = tabStates[tab].value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            loadHairstylesPage(tab, page = currentPages[tab] + 1, append = true)
        }
    }

    private suspend fun loadHairstylesPage(tabPosition: Int, page: Int, append: Boolean) {
        if (!append) {
            tabStates[tabPosition].update { it.copy(isLoading = true, error = null) }
        } else {
            tabStates[tabPosition].update { it.copy(isLoadingMore = true, error = null) }
        }

        val loader = tabLoader(tabPosition) ?: run {
            tabStates[tabPosition].update {
                it.copy(isLoading = false, isLoadingMore = false, items = emptyList(), hasMore = false)
            }
            return
        }

        runCatching { loader(page) }
            .onSuccess { result ->
                currentPages[tabPosition] = result.page
                isTabLoaded[tabPosition] = true
                val mergedItems = if (append) {
                    tabStates[tabPosition].value.items + result.items
                } else {
                    result.items
                }
                tabStates[tabPosition].update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        items = mergedItems,
                        hasMore = result.hasMore,
                        error = null
                    )
                }
            }
            .onFailure { e ->
                tabStates[tabPosition].update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load",
                        items = if (append) it.items else emptyList()
                    )
                }
            }
    }

    private fun tabLoader(tabPosition: Int): (suspend (Int) -> PagedCategoryImages)? {
        return when (tabPosition) {
            0 -> categoryImagesRepository::loadMenHairstyles
            1 -> categoryImagesRepository::loadWomenHairstyles
            2 -> categoryImagesRepository::loadChildrenHairstyles
            else -> null
        }
    }

    fun loadHairColors() {
        viewModelScope.launch {
            _hairColors.update { it.copy(isLoading = true, error = null, items = emptyList()) }
            runCatching { categoryImagesRepository.loadHairColors(page = 1) }
                .onSuccess { result ->
                    _hairColors.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            hasMore = result.hasMore,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _hairColors.update {
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
