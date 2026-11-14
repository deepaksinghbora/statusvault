package com.deepak.statusvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private val Context.statusVaultDataStore by preferencesDataStore(name = "status_vault_prefs")

class ThemeStore(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val mode: Flow<ThemeMode> = context.statusVaultDataStore.data.map { prefs ->
        prefs[themeModeKey]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setMode(mode: ThemeMode) {
        context.statusVaultDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}

class StatusFolderStore(private val context: Context) {
    private val treeUriKey = stringPreferencesKey("status_tree_uri")

    val treeUriFlow: Flow<String?> = context.statusVaultDataStore.data.map { prefs ->
        prefs[treeUriKey]
    }

    suspend fun getTreeUri(): String? =
        context.statusVaultDataStore.data.map { it[treeUriKey] }.first()

    suspend fun setTreeUri(uri: String) {
        context.statusVaultDataStore.edit { prefs ->
            prefs[treeUriKey] = uri
        }
    }

    suspend fun clearTreeUri() {
        context.statusVaultDataStore.edit { prefs ->
            prefs.remove(treeUriKey)
        }
    }
}
