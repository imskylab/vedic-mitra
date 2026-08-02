/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Alarm screen entry point. Collects [AlarmViewModel] state and renders the (placeholder) content.
 * The stateless [AlarmContent] takes state as parameters for easy preview/testing.
 */
@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AlarmContent(uiState = uiState, modifier = modifier)
}

/** Stateless alarm content. Real UI is built in a later phase. */
@Composable
private fun AlarmContent(
    uiState: AlarmUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = if (uiState.alarms.isEmpty()) "No alarms" else "Alarms: ${uiState.alarms.size}")
    }
}

@Preview
@Composable
private fun AlarmContentPreview() {
    VedicMitraTheme {
        AlarmContent(uiState = AlarmUiState())
    }
}
