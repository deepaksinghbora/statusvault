package com.deepak.statusvault.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.statusvault.R
import com.deepak.statusvault.data.ThemeStore
import com.deepak.statusvault.ui.showForOneSecond
import com.deepak.statusvault.domain.PreviewItem
import com.deepak.statusvault.ui.components.EmptyState
import com.deepak.statusvault.ui.components.MediaThumb
import com.deepak.statusvault.ui.components.ShimmerGrid
import com.deepak.statusvault.ui.theme.ThemeModeButton
import com.deepak.statusvault.ui.theme.whatsAppTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    themeStore: ThemeStore,
    onOpenPreview: (items: List<PreviewItem>, initialIndex: Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var refreshing by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh(showLoading = false)
        }
    }

    val savedMsg = stringResource(R.string.saved_snackbar)
    val deletedMsg = stringResource(R.string.deleted_snackbar)
    val galleryMsg = stringResource(R.string.gallery_snackbar)
    val errorGeneric = stringResource(R.string.error_generic)

    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            val text = when (it) {
                "saved" -> savedMsg
                "deleted" -> deletedMsg
                "gallery" -> galleryMsg
                else -> it
            }
            when (it) {
                "saved", "deleted" -> snackbar.showForOneSecond(text)
                else -> snackbar.showSnackbar(text)
            }
            viewModel.consumeMessage()
        }
        state.error?.let {
            snackbar.showSnackbar(it.ifBlank { errorGeneric })
            viewModel.consumeError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text("${stringResource(R.string.tab_saved)} (${state.items.size})")
                },
                colors = whatsAppTopAppBarColors(),
                windowInsets = WindowInsets.statusBars,
                actions = {
                    ThemeModeButton(themeStore)
                    IconButton(onClick = {
                        refreshing = true
                        viewModel.refresh()
                        refreshing = false
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> ShimmerGrid()
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Default.Collections,
                    title = stringResource(R.string.saved_empty_title),
                    body = stringResource(R.string.saved_empty_body),
                )
                else -> {
                    PullToRefreshBox(
                        isRefreshing = refreshing || state.isLoading,
                        onRefresh = {
                            refreshing = true
                            viewModel.refresh()
                            refreshing = false
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.items, key = { it.path }) { item ->
                                MediaThumb(
                                    imageModel = state.thumbnails[item.path] ?: item.path.takeUnless { item.isVideo },
                                    isVideo = item.isVideo,
                                    durationMs = state.durationsMs[item.path],
                                    onClick = {
                                        val previewItems = state.items.map {
                                            PreviewItem(
                                                path = it.path,
                                                name = it.name,
                                                isVideo = it.isVideo,
                                                title = it.name,
                                            )
                                        }
                                        val index = state.items.indexOfFirst { it.path == item.path }
                                            .coerceAtLeast(0)
                                        onOpenPreview(previewItems, index)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
