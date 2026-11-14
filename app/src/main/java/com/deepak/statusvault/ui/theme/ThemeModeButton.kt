package com.deepak.statusvault.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.statusvault.R
import com.deepak.statusvault.data.ThemeMode
import com.deepak.statusvault.data.ThemeStore
import kotlinx.coroutines.launch

@Composable
fun ThemeModeButton(themeStore: ThemeStore) {
    val mode by themeStore.mode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val icon = when (mode) {
        ThemeMode.LIGHT -> Icons.Filled.LightMode
        ThemeMode.DARK -> Icons.Filled.DarkMode
        ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
    }

    IconButton(onClick = { expanded = true }) {
        Icon(icon, contentDescription = stringResource(R.string.theme))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ThemeMenuItem(stringResource(R.string.theme_system), mode == ThemeMode.SYSTEM) {
                scope.launch { themeStore.setMode(ThemeMode.SYSTEM) }
                expanded = false
            }
            ThemeMenuItem(stringResource(R.string.theme_light), mode == ThemeMode.LIGHT) {
                scope.launch { themeStore.setMode(ThemeMode.LIGHT) }
                expanded = false
            }
            ThemeMenuItem(stringResource(R.string.theme_dark), mode == ThemeMode.DARK) {
                scope.launch { themeStore.setMode(ThemeMode.DARK) }
                expanded = false
            }
        }
    }
}

@Composable
private fun ThemeMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(if (selected) "$label  ✓" else label) },
        onClick = onClick,
    )
}
