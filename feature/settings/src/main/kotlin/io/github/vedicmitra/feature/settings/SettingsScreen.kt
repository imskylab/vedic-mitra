/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Settings screen entry point. Collects [SettingsViewModel] state and renders the (placeholder)
 * content. The stateless [SettingsContent] takes state as parameters for easy preview/testing.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState = uiState, modifier = modifier)
}

/** Stateless settings content. Real UI is built in a later phase. */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Settings")
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    VedicMitraTheme {
        SettingsContent(uiState = SettingsUiState())
    }
}
