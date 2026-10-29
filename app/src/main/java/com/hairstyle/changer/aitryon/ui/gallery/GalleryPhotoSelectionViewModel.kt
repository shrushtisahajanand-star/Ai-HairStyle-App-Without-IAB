package com.hairstyle.changer.aitryon.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hairstyle.changer.aitryon.repository.DeviceGalleryRepository
import com.hairstyle.changer.aitryon.domain.model.GalleryAlbum
import com.hairstyle.changer.aitryon.domain.model.GalleryPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GallerySelectionUiState(
    val isLoading: Boolean = true,
    val albums: List<GalleryAlbum> = emptyList(),
    val photos: List<GalleryPhoto> = emptyList(),
    val selectedAlbumId: String = DeviceGalleryRepository.ALBUM_ALL_ID,
    val selectedPhotoId: Long? = null,
    val error: String? = null
) {
    val selectedAlbumName: String
        get() = albums.firstOrNull { it.id == selectedAlbumId }?.name ?: "All"

    val selectedPhoto: GalleryPhoto?
        get() = photos.firstOrNull { it.id == selectedPhotoId }
}

@HiltViewModel
class GalleryPhotoSelectionViewModel @Inject constructor(
    private val deviceGalleryRepository: DeviceGalleryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GallerySelectionUiState())
    val state: StateFlow<GallerySelectionUiState> = _state.asStateFlow()

    fun loadGallery() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val albums = deviceGalleryRepository.loadAlbums()
                    val photos = deviceGalleryRepository.loadPhotos(DeviceGalleryRepository.ALBUM_ALL_ID)
                    albums to photos
                }
            }.onSuccess { (albums, photos) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        albums = albums,
                        photos = photos,
                        selectedAlbumId = DeviceGalleryRepository.ALBUM_ALL_ID,
                        error = null
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load gallery"
                    )
                }
            }
        }
    }

    fun selectAlbum(albumId: String) {
        if (_state.value.selectedAlbumId == albumId) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, selectedAlbumId = albumId, selectedPhotoId = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    deviceGalleryRepository.loadPhotos(albumId)
                }
            }.onSuccess { photos ->
                _state.update {
                    it.copy(isLoading = false, photos = photos, error = null)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load photos")
                }
            }
        }
    }

    fun selectPhoto(photoId: Long) {
        _state.update { it.copy(selectedPhotoId = photoId) }
    }

    fun clearPhotoSelection() {
        _state.update { it.copy(selectedPhotoId = null) }
    }
}
