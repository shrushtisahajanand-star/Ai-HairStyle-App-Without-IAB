package com.hairstyle.generator.aimakerover.ui.home.beard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.generator.aimakerover.data.model.PagedBeardStyles
import com.hairstyle.generator.aimakerover.data.repository.CategoryImagesRepository
import com.hairstyle.generator.aimakerover.domain.model.BeardStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeardUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<BeardStyle> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class BeardViewModel @Inject constructor(
    private val categoryImagesRepository: CategoryImagesRepository
) : ViewModel() {

    private val pager = BeardPager { page -> categoryImagesRepository.loadBeardStyles(page) }

    val beardStyles: StateFlow<BeardUiState> = pager.state

    init {
        pager.loadInitial(viewModelScope)
    }

    fun loadMoreBeardStyles() = pager.loadMore(viewModelScope)
}

private class BeardPager(
    private val fetchPage: suspend (Int) -> PagedBeardStyles
) {
    private val _state = MutableStateFlow(BeardUiState(isLoading = true))
    val state: StateFlow<BeardUiState> = _state.asStateFlow()

    private var currentPage = 0

    fun loadInitial(scope: CoroutineScope) {
        currentPage = 0
        scope.launch {
            _state.update {
                it.copy(isLoading = true, isLoadingMore = false, error = null, items = emptyList())
            }
            runCatching { fetchPage(1) }
                .onSuccess { page ->
                    currentPage = page.page
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = page.items,
                            hasMore = page.hasMore,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to load")
                    }
                }
        }
    }

    fun loadMore(scope: CoroutineScope) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.hasMore) return

        scope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            val nextPage = currentPage + 1
            runCatching { fetchPage(nextPage) }
                .onSuccess { page ->
                    currentPage = page.page
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + page.items,
                            hasMore = page.hasMore,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            error = e.message ?: "Failed to load more"
                        )
                    }
                }
        }
    }
}
