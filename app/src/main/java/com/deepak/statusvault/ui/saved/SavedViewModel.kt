package com.deepak.statusvault.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.statusvault.data.AppContainer
import com.deepak.statusvault.domain.SavedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedUiState(
    val items: List<SavedItem> = emptyList(),
    val thumbnails: Map<String, String> = emptyMap(),
    val durationsMs: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

class SavedViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedUiState())
    val state: StateFlow<SavedUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            _state.update {
                if (showLoading) it.copy(isLoading = true, error = null) else it.copy(error = null)
            }
            try {
                val items = container.savedRepository.listSaved()
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        items = items,
                        thumbnails = current.thumbnails.filterKeys { path ->
                            items.any { it.path == path }
                        },
                        durationsMs = current.durationsMs.filterKeys { path ->
                            items.any { it.path == path }
                        },
                    )
                }
                items.filter { it.isVideo && it.path !in _state.value.thumbnails }
                    .forEach { loadThumbnail(it) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    suspend fun delete(item: SavedItem): Boolean {
        return try {
            container.savedRepository.delete(item)
            _state.update { it.copy(message = "deleted") }
            refresh()
            true
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
            false
        }
    }

    fun share(item: SavedItem) {
        try {
            container.shareHelper.shareFile(item.path, item.isVideo)
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    fun sharePath(path: String, isVideo: Boolean) {
        try {
            container.shareHelper.shareFile(path, isVideo)
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    suspend fun saveToGallery(path: String, isVideo: Boolean): Boolean {
        return try {
            container.galleryExporter.saveToGallery(path, isVideo)
            _state.update { it.copy(message = "gallery") }
            true
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
            false
        }
    }

    private fun loadThumbnail(item: SavedItem) {
        viewModelScope.launch {
            val thumb = container.thumbnailCache.getOrCreateFromFile(item.path, item.isVideo)
                ?: return@launch
            _state.update { state ->
                state.copy(
                    thumbnails = state.thumbnails + (item.path to thumb.imagePath),
                    durationsMs = thumb.durationMs?.let { state.durationsMs + (item.path to it) }
                        ?: state.durationsMs,
                )
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SavedViewModel(container) as T
                }
            }
    }
}
