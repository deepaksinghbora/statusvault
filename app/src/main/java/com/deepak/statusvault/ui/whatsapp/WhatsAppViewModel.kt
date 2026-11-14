package com.deepak.statusvault.ui.whatsapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.statusvault.data.AppContainer
import com.deepak.statusvault.domain.DateSort
import com.deepak.statusvault.domain.MediaFilter
import com.deepak.statusvault.domain.StatusItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WhatsAppUiState(
    val items: List<StatusItem> = emptyList(),
    val thumbnails: Map<String, String> = emptyMap(),
    val durationsMs: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val hasFolder: Boolean = false,
    val filter: MediaFilter = MediaFilter.ALL,
    val sort: DateSort = DateSort.NEWEST,
    val error: String? = null,
    val message: String? = null,
) {
    val visibleItems: List<StatusItem>
        get() {
            val filtered = when (filter) {
                MediaFilter.ALL -> items
                MediaFilter.IMAGES -> items.filter { !it.isVideo }
                MediaFilter.VIDEOS -> items.filter { it.isVideo }
            }
            return when (sort) {
                DateSort.NEWEST -> filtered.sortedByDescending { it.lastModifiedMs }
                DateSort.OLDEST -> filtered.sortedBy { it.lastModifiedMs }
            }
        }
}

class WhatsAppViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(WhatsAppUiState())
    val state: StateFlow<WhatsAppUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val hasFolder = container.whatsAppRepository.hasFolder()
                if (!hasFolder) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasFolder = false,
                            items = emptyList(),
                            thumbnails = emptyMap(),
                            durationsMs = emptyMap(),
                        )
                    }
                    return@launch
                }
                val items = container.whatsAppRepository.listStatuses()
                _state.update {
                    it.copy(
                        isLoading = false,
                        hasFolder = true,
                        items = items,
                        thumbnails = emptyMap(),
                        durationsMs = emptyMap(),
                    )
                }
                items.forEach { loadThumbnail(it) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        hasFolder = false,
                        items = emptyList(),
                        error = e.message,
                    )
                }
            }
        }
    }

    fun onFolderPicked(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                _state.update { it.copy(message = "folder_cancelled") }
                return@launch
            }
            _state.update { it.copy(isBusy = true) }
            try {
                container.whatsAppRepository.persistTreeUri(uri)
                _state.update { it.copy(isBusy = false, message = "folder_granted") }
                refresh()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isBusy = false, error = e.message ?: "error_generic")
                }
            }
        }
    }

    fun setFilter(filter: MediaFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setSort(sort: DateSort) {
        _state.update { it.copy(sort = sort) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }

    suspend fun resolvePath(item: StatusItem): String =
        container.whatsAppRepository.copyStatusToCache(item)

    private fun loadThumbnail(item: StatusItem) {
        viewModelScope.launch {
            val thumb = container.whatsAppRepository.thumbnailFor(item) ?: return@launch
            _state.update { state ->
                state.copy(
                    thumbnails = state.thumbnails + (item.uri to thumb.imagePath),
                    durationsMs = thumb.durationMs?.let { state.durationsMs + (item.uri to it) }
                        ?: state.durationsMs,
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WhatsAppViewModel(container) as T
                }
            }
    }
}
