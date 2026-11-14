package com.deepak.statusvault.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val WhatsAppTeal = Color(0xFF075E54)
val WhatsAppTealDark = Color(0xFF054C44)
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppGreenDark = Color(0xFF00A884)
val WhatsAppBubble = Color(0xFFD9FDD3)
val WhatsAppOutgoingDark = Color(0xFF005C4B)

private val WhatsAppText = Color(0xFF111B21)
private val WhatsAppSecondaryText = Color(0xFF667781)
private val WhatsAppBackground = Color(0xFFF0F2F5)
private val WhatsAppDivider = Color(0xFFE9EDEF)

private val WhatsAppDarkBackground = Color(0xFF0B141A)
private val WhatsAppDarkSurface = Color(0xFF111B21)
private val WhatsAppDarkHeader = Color(0xFF1F2C34)
private val WhatsAppDarkText = Color(0xFFE9EDEF)
private val WhatsAppDarkSecondaryText = Color(0xFF8696A0)

private val LightColors = lightColorScheme(
    primary = WhatsAppTeal,
    onPrimary = Color.White,
    primaryContainer = WhatsAppBubble,
    onPrimaryContainer = WhatsAppTeal,
    secondary = WhatsAppGreen,
    onSecondary = Color.White,
    secondaryContainer = WhatsAppBubble,
    onSecondaryContainer = WhatsAppTeal,
    tertiary = Color(0xFF128C7E),
    onTertiary = Color.White,
    background = WhatsAppBackground,
    onBackground = WhatsAppText,
    surface = Color.White,
    onSurface = WhatsAppText,
    surfaceVariant = WhatsAppDivider,
    onSurfaceVariant = WhatsAppSecondaryText,
    outline = Color(0xFFD1D7DB),
)

private val DarkColors = darkColorScheme(
    primary = WhatsAppGreenDark,
    onPrimary = Color(0xFF00382C),
    primaryContainer = WhatsAppOutgoingDark,
    onPrimaryContainer = WhatsAppBubble,
    secondary = WhatsAppGreen,
    onSecondary = Color(0xFF00382C),
    secondaryContainer = WhatsAppOutgoingDark,
    onSecondaryContainer = WhatsAppBubble,
    tertiary = WhatsAppGreenDark,
    onTertiary = Color(0xFF00382C),
    background = WhatsAppDarkBackground,
    onBackground = WhatsAppDarkText,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppDarkText,
    surfaceVariant = WhatsAppDarkHeader,
    onSurfaceVariant = WhatsAppDarkSecondaryText,
    outline = Color(0xFF2A3942),
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun StatusVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun whatsAppTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = if (LocalDarkTheme.current) WhatsAppDarkHeader else WhatsAppTeal,
    scrolledContainerColor = if (LocalDarkTheme.current) WhatsAppDarkHeader else WhatsAppTealDark,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)

@Composable
fun whatsAppNavigationBarContainerColor(): Color =
    if (LocalDarkTheme.current) WhatsAppDarkHeader else Color.White

@Composable
fun whatsAppNavigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = if (LocalDarkTheme.current) WhatsAppGreenDark else WhatsAppTeal,
    selectedTextColor = if (LocalDarkTheme.current) WhatsAppGreenDark else WhatsAppTeal,
    indicatorColor = if (LocalDarkTheme.current) WhatsAppOutgoingDark else WhatsAppBubble,
    unselectedIconColor = if (LocalDarkTheme.current) WhatsAppDarkSecondaryText else WhatsAppSecondaryText,
    unselectedTextColor = if (LocalDarkTheme.current) WhatsAppDarkSecondaryText else WhatsAppSecondaryText,
)
