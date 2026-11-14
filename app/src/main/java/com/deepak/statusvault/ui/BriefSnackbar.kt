package com.deepak.statusvault.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal suspend fun SnackbarHostState.showForOneSecond(message: String) {
    coroutineScope {
        val showing = launch {
            showSnackbar(message = message, duration = SnackbarDuration.Indefinite)
        }
        delay(1_000)
        currentSnackbarData?.dismiss()
        showing.join()
    }
}
