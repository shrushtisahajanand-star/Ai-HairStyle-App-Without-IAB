package com.hairstyle.changer.aitryon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.changer.aitryon.model.PagedCategoryImages
import com.hairstyle.changer.aitryon.repository.CategoryImagesRepository
import com.hairstyle.changer.aitryon.repository.VideosRepository
import com.hairstyle.changer.aitryon.domain.model.CategoryHairstyle
import com.hairstyle.changer.aitryon.domain.model.HomeHeroSlide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeHairstyleUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<CategoryHairstyle> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true
)

data class HomeHeroUiState(
    val slides: List<HomeHeroSlide> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryImagesRepository: CategoryImagesRepository,
    private val videosRepository: VideosRepository
) : ViewModel() {

    private val menPager = HairstylePager { page -> categoryImagesRepository.loadMenHairstyles(page) }
    private val womenPager = HairstylePager { page -> categoryImagesRepository.loadWomenHairstyles(page) }
    private val childrenPager = HairstylePager { page -> categoryImagesRepository.loadChildrenHairstyles(page) }

    val menHairstyles: StateFlow<HomeHairstyleUiState> = menPager.state
    val womenHairstyles: StateFlow<HomeHairstyleUiState> = womenPager.state
    val childrenHairstyles: StateFlow<HomeHairstyleUiState> = childrenPager.state

    private val _heroSlides = MutableStateFlow(HomeHeroUiState())
    val heroSlides: StateFlow<HomeHeroUiState> = _heroSlides.asStateFlow()

    init {
        loadHeroSlides()
        menPager.loadInitial(viewModelScope)
        womenPager.loadInitial(viewModelScope)
        childrenPager.loadInitial(viewModelScope)
    }

    fun loadMoreMenHairstyles() = menPager.loadMore(viewModelScope)
    fun loadMoreWomenHairstyles() = womenPager.loadMore(viewModelScope)
    fun loadMoreChildrenHairstyles() = childrenPager.loadMore(viewModelScope)

    fun loadHeroSlides() {
        _heroSlides.value = HomeHeroUiState(slides = videosRepository.getHomeHeroSlides())
    }
}

private class HairstylePager(
    private val fetchPage: suspend (Int) -> PagedCategoryImages
) {
    private val _state = MutableStateFlow(HomeHairstyleUiState(isLoading = true))
    val state: StateFlow<HomeHairstyleUiState> = _state.asStateFlow()

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
