package com.deepak.statusvault.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepak.statusvault.R
import com.deepak.statusvault.data.AppContainer
import com.deepak.statusvault.data.ThemeMode
import com.deepak.statusvault.domain.PreviewItem
import com.deepak.statusvault.ui.preview.MediaPreviewScreen
import com.deepak.statusvault.ui.saved.SavedScreen
import com.deepak.statusvault.ui.saved.SavedViewModel
import com.deepak.statusvault.ui.theme.StatusVaultTheme
import com.deepak.statusvault.ui.theme.WhatsAppGreen
import com.deepak.statusvault.ui.theme.whatsAppNavigationBarContainerColor
import com.deepak.statusvault.ui.theme.whatsAppNavigationBarItemColors
import com.deepak.statusvault.ui.whatsapp.WhatsAppScreen
import com.deepak.statusvault.ui.whatsapp.WhatsAppViewModel

private object Routes {
    const val WhatsApp = "whatsapp"
    const val Saved = "saved"
    const val Preview = "preview/{mode}/{index}"
}

private const val PreviewEnterMillis = 320
private const val PreviewExitMillis = 260

private data class PreviewSession(
    val items: List<PreviewItem>,
    val initialIndex: Int,
    val canSave: Boolean,
    val canDelete: Boolean,
)

@Composable
fun StatusVaultApp(container: AppContainer) {
    val themeMode by container.themeStore.mode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    StatusVaultTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val whatsAppViewModel: WhatsAppViewModel = viewModel(
            factory = WhatsAppViewModel.factory(container),
        )
        val savedViewModel: SavedViewModel = viewModel(
            factory = SavedViewModel.factory(container),
        )
        var previewSession by remember { mutableStateOf<PreviewSession?>(null) }

        fun openPreview(
            items: List<PreviewItem>,
            index: Int,
            canSave: Boolean,
            canDelete: Boolean,
        ) {
            previewSession = PreviewSession(items, index, canSave, canDelete)
            navController.navigate("preview/${if (canDelete) "saved" else "status"}/$index")
        }

        NavHost(
            navController = navController,
            startDestination = Routes.WhatsApp,
            modifier = Modifier.fillMaxSize(),
        ) {
                composable(
                    route = Routes.WhatsApp,
                    exitTransition = { tabExitTransition() },
                    popEnterTransition = { tabPopEnterTransition() },
                ) {
                    TabScaffold(navController = navController, selectedRoute = Routes.WhatsApp) {
                        WhatsAppScreen(
                            viewModel = whatsAppViewModel,
                            themeStore = container.themeStore,
                            onOpenPreview = { items, index ->
                                openPreview(items, index, canSave = true, canDelete = false)
                            },
                        )
                    }
                }
                composable(
                    route = Routes.Saved,
                    exitTransition = { tabExitTransition() },
                    popEnterTransition = { tabPopEnterTransition() },
                ) {
                    TabScaffold(navController = navController, selectedRoute = Routes.Saved) {
                        SavedScreen(
                            viewModel = savedViewModel,
                            themeStore = container.themeStore,
                            onOpenPreview = { items, index ->
                                openPreview(items, index, canSave = false, canDelete = true)
                            },
                        )
                    }
                }
                composable(
                    route = Routes.Preview,
                    arguments = listOf(
                        navArgument("mode") { type = NavType.StringType },
                        navArgument("index") { type = NavType.IntType },
                    ),
                    enterTransition = {
                        slideInVertically(
                            animationSpec = tween(PreviewEnterMillis),
                            initialOffsetY = { fullHeight -> fullHeight },
                        ) + fadeIn(animationSpec = tween(PreviewEnterMillis))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            animationSpec = tween(PreviewExitMillis),
                            targetOffsetY = { fullHeight -> fullHeight },
                        ) + fadeOut(animationSpec = tween(PreviewExitMillis))
                    },
                ) {
                    val session = previewSession
                    if (session == null) {
                        // Preview state does not survive process death. Leave once,
                        // from an effect, so this cannot pop the tab underneath.
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    } else {
                        MediaPreviewScreen(
                            items = session.items,
                            initialIndex = session.initialIndex,
                            canSave = session.canSave,
                            canDelete = session.canDelete,
                            container = container,
                            onBack = { navController.popBackStack() },
                            onDeleted = { savedViewModel.refresh(showLoading = false) },
                            onSaved = { savedViewModel.refresh(showLoading = false) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun TabScaffold(
    navController: NavHostController,
    selectedRoute: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
        bottomBar = {
            Box {
                AppNavigationBar(navController, selectedRoute)
                val context = LocalContext.current
                FloatingActionButton(
                    onClick = { shareAppDownloadLink(context) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-28).dp),
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White,
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_app),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            content()
        }
    }
}

@Composable
private fun AppNavigationBar(
    navController: NavHostController,
    selectedRoute: String,
) {
    NavigationBar(
        containerColor = whatsAppNavigationBarContainerColor(),
    ) {
        val tabColors = whatsAppNavigationBarItemColors()
        NavigationBarItem(
            selected = selectedRoute == Routes.WhatsApp,
            colors = tabColors,
            onClick = {
                navController.navigate(Routes.WhatsApp) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Icon(
                    if (selectedRoute == Routes.WhatsApp) {
                        Icons.Filled.ChatBubble
                    } else {
                        Icons.Outlined.ChatBubbleOutline
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_whatsapp)) },
        )
        NavigationBarItem(
            selected = selectedRoute == Routes.Saved,
            colors = tabColors,
            onClick = {
                navController.navigate(Routes.Saved) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Icon(
                    if (selectedRoute == Routes.Saved) {
                        Icons.Filled.Collections
                    } else {
                        Icons.Outlined.Collections
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_saved)) },
        )
    }
}

private fun shareAppDownloadLink(context: Context) {
    val link = "https://github.com/deepaksinghbora/statusvault/releases/latest/download/app-release.apk"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text, link))
    }
    context.startActivity(
        Intent.createChooser(send, context.getString(R.string.share_via)),
    )
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.tabExitTransition(): ExitTransition {
    return if (targetState.destination.route == Routes.Preview) {
        fadeOut(
            animationSpec = tween(PreviewEnterMillis),
            targetAlpha = 1f,
        )
    } else {
        fadeOut(tween(220))
    }
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.tabPopEnterTransition(): EnterTransition {
    return if (initialState.destination.route == Routes.Preview) {
        fadeIn(
            animationSpec = tween(PreviewExitMillis),
            initialAlpha = 1f,
        )
    } else {
        fadeIn(tween(220))
    }
}
