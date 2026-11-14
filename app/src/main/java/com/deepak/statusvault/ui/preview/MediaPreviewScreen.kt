package com.deepak.statusvault.ui.preview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.deepak.statusvault.R
import com.deepak.statusvault.ui.showForOneSecond
import com.deepak.statusvault.data.AppContainer
import com.deepak.statusvault.domain.PreviewItem
import com.deepak.statusvault.domain.SavedItem
import com.deepak.statusvault.domain.StatusItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    items: List<PreviewItem>,
    initialIndex: Int,
    canSave: Boolean,
    canDelete: Boolean,
    container: AppContainer,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onSaved: () -> Unit,
) {
    var pages by remember { mutableStateOf(items) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
        pageCount = { pages.size },
    )
    val resolvedPaths = remember { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val current = pages.getOrNull(pagerState.currentPage)

    LaunchedEffect(current?.uri, current?.path) {
        val item = current ?: return@LaunchedEffect
        if (!item.path.isNullOrBlank()) return@LaunchedEffect
        val uri = item.uri ?: return@LaunchedEffect
        if (resolvedPaths.containsKey(uri)) return@LaunchedEffect
        busy = true
        try {
            val path = container.whatsAppRepository.copyStatusToCache(
                StatusItem(
                    uri = uri,
                    name = item.name,
                    mimeType = if (item.isVideo) "video/mp4" else "image/jpeg",
                    lastModifiedMs = 0L,
                ),
            )
            resolvedPaths[uri] = path
        } catch (e: Exception) {
            snackbar.showSnackbar(e.message ?: context.getString(R.string.error_generic))
        } finally {
            busy = false
        }
    }

    fun pathFor(item: PreviewItem): String? {
        if (!item.path.isNullOrBlank()) return item.path
        val uri = item.uri ?: return null
        return resolvedPaths[uri]
    }

    fun deleteCurrent() {
        val item = pages.getOrNull(pagerState.currentPage) ?: return
        val path = pathFor(item) ?: return
        val removedIndex = pagerState.currentPage
        val updated = pages.toMutableList().apply { removeAt(removedIndex) }
        scope.launch {
            try {
                withContext(NonCancellable) {
                    container.savedRepository.delete(
                        SavedItem(
                            path = path,
                            name = item.name,
                            isVideo = item.isVideo,
                        ),
                    )
                    onDeleted()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: context.getString(R.string.error_generic))
            }
        }
        scope.launch {
            snackbar.showForOneSecond(context.getString(R.string.deleted_snackbar))
        }
        if (updated.isEmpty()) {
            onBack()
            return
        }
        if (removedIndex >= updated.size) {
            scope.launch {
                pagerState.scrollToPage(updated.lastIndex)
                pages = updated
            }
        } else {
            pages = updated
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = current?.title ?: stringResource(R.string.app_name),
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    if (canDelete && current != null) {
                        IconButton(onClick = { deleteCurrent() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = Color.White,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (current != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canSave) {
                        FilledTonalIconButton(
                            onClick = {
                                val path = pathFor(current) ?: return@FilledTonalIconButton
                                val isVideo = current.isVideo
                                val name = current.name
                                scope.launch {
                                    try {
                                        withContext(NonCancellable) {
                                            container.savedRepository.saveFromFile(
                                                path,
                                                isVideo,
                                                name,
                                            )
                                            onSaved()
                                        }
                                        snackbar.showForOneSecond(
                                            context.getString(R.string.saved_snackbar),
                                        )
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        snackbar.showSnackbar(
                                            e.message ?: context.getString(R.string.error_generic),
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.save))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val path = pathFor(current) ?: return@FilledTonalIconButton
                            try {
                                container.shareHelper.shareFile(path, current.isVideo)
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        e.message ?: context.getString(R.string.error_generic),
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = {
                            val path = pathFor(current) ?: return@FilledTonalIconButton
                            scope.launch {
                                busy = true
                                try {
                                    container.galleryExporter.saveToGallery(path, current.isVideo)
                                    snackbar.showSnackbar(context.getString(R.string.gallery_snackbar))
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(
                                        e.message ?: context.getString(R.string.error_generic),
                                    )
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.save_to_gallery),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${pagerState.currentPage + 1}/${pages.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
        ) {
            if (pages.isEmpty()) {
                Text(
                    text = stringResource(R.string.error_generic),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize(),
                    key = { page ->
                        val item = pages.getOrNull(page)
                        item?.path ?: item?.uri ?: page
                    },
                ) { page ->
                    val item = pages.getOrNull(page) ?: return@HorizontalPager
                    val path = pathFor(item)
                    val isCurrentPage = pagerState.currentPage == page
                    when {
                        path == null -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        item.isVideo -> VideoPlayer(path = path, isCurrentPage = isCurrentPage)
                        else -> AsyncImage(
                            model = File(path),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun VideoPlayer(
    path: String,
    isCurrentPage: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            prepare()
            playWhenReady = false
        }
    }
    LaunchedEffect(isCurrentPage, player) {
        if (isCurrentPage) player.play() else player.pause()
    }
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.pause()
            player.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
