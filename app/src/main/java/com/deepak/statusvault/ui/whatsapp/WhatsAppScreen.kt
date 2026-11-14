package com.deepak.statusvault.ui.whatsapp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.statusvault.R
import com.deepak.statusvault.data.ThemeStore
import com.deepak.statusvault.domain.DateSort
import com.deepak.statusvault.domain.MediaFilter
import com.deepak.statusvault.domain.PreviewItem
import com.deepak.statusvault.domain.StatusItem
import com.deepak.statusvault.ui.components.EmptyState
import com.deepak.statusvault.ui.components.MediaThumb
import com.deepak.statusvault.ui.components.ShimmerGrid
import com.deepak.statusvault.ui.theme.ThemeModeButton
import com.deepak.statusvault.ui.theme.whatsAppTopAppBarColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppScreen(
    viewModel: WhatsAppViewModel,
    themeStore: ThemeStore,
    onOpenPreview: (items: List<PreviewItem>, initialIndex: Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFolderDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        viewModel.onFolderPicked(uri)
    }

    val folderGranted = stringResource(R.string.folder_granted)
    val folderCancelled = stringResource(R.string.folder_cancelled)
    val errorGeneric = stringResource(R.string.error_generic)

    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            val text = when (it) {
                "folder_granted" -> folderGranted
                "folder_cancelled" -> folderCancelled
                else -> it
            }
            snackbar.showSnackbar(text)
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
                title = { Text(stringResource(R.string.tab_whatsapp)) },
                colors = whatsAppTopAppBarColors(),
                windowInsets = WindowInsets.statusBars,
                actions = {
                    ThemeModeButton(themeStore)
                    if (state.hasFolder) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                        }
                        IconButton(onClick = {
                            refreshing = true
                            viewModel.refresh()
                            refreshing = false
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                        IconButton(onClick = { showFolderDialog = true }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.change_folder))
                        }
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
                !state.hasFolder -> EmptyState(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = stringResource(R.string.whatsapp_empty_title),
                    body = stringResource(R.string.whatsapp_empty_body),
                    action = {
                        FilledTonalButton(onClick = { showFolderDialog = true }) {
                            Text(stringResource(R.string.choose_folder))
                        }
                    },
                )
                state.visibleItems.isEmpty() -> EmptyState(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = stringResource(R.string.whatsapp_empty_title),
                    body = stringResource(R.string.whatsapp_empty_body),
                    action = {
                        FilledTonalButton(onClick = { showFolderDialog = true }) {
                            Text(stringResource(R.string.change_folder))
                        }
                    },
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
                        StatusGrid(
                            items = state.visibleItems,
                            thumbnails = state.thumbnails,
                            durationsMs = state.durationsMs,
                            onClick = { clicked ->
                                scope.launch {
                                    val previewItems = state.visibleItems.map { item ->
                                        PreviewItem(
                                            path = null,
                                            uri = item.uri,
                                            name = item.name,
                                            isVideo = item.isVideo,
                                            title = item.name,
                                        )
                                    }
                                    val index = state.visibleItems.indexOfFirst { it.uri == clicked.uri }
                                        .coerceAtLeast(0)
                                    onOpenPreview(previewItems, index)
                                }
                            },
                        )
                    }
                }
            }

            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(stringResource(R.string.allow_folder_title)) },
            text = { Text(stringResource(R.string.whatsapp_empty_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFolderDialog = false
                        folderLauncher.launch(null)
                    },
                ) {
                    Text(stringResource(R.string.choose_folder))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.filter),
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filter == MediaFilter.ALL,
                        onClick = {
                            viewModel.setFilter(MediaFilter.ALL)
                            showFilterSheet = false
                        },
                        label = { Text(stringResource(R.string.filter_all)) },
                    )
                    FilterChip(
                        selected = state.filter == MediaFilter.IMAGES,
                        onClick = {
                            viewModel.setFilter(MediaFilter.IMAGES)
                            showFilterSheet = false
                        },
                        label = { Text(stringResource(R.string.filter_images)) },
                    )
                    FilterChip(
                        selected = state.filter == MediaFilter.VIDEOS,
                        onClick = {
                            viewModel.setFilter(MediaFilter.VIDEOS)
                            showFilterSheet = false
                        },
                        label = { Text(stringResource(R.string.filter_videos)) },
                    )
                }
                Text(
                    text = stringResource(R.string.sort_by_date),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.sort == DateSort.NEWEST,
                        onClick = {
                            viewModel.setSort(DateSort.NEWEST)
                            showFilterSheet = false
                        },
                        label = { Text(stringResource(R.string.sort_newest)) },
                    )
                    FilterChip(
                        selected = state.sort == DateSort.OLDEST,
                        onClick = {
                            viewModel.setSort(DateSort.OLDEST)
                            showFilterSheet = false
                        },
                        label = { Text(stringResource(R.string.sort_oldest)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusGrid(
    items: List<StatusItem>,
    thumbnails: Map<String, String>,
    durationsMs: Map<String, Long>,
    onClick: (StatusItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.uri }) { item ->
            MediaThumb(
                imageModel = thumbnails[item.uri],
                isVideo = item.isVideo,
                durationMs = durationsMs[item.uri],
                onClick = { onClick(item) },
            )
        }
    }
}
