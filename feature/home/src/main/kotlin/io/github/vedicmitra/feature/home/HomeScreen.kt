/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.home

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
 * Home screen entry point. Collects [HomeViewModel] state and renders the (placeholder) content.
 * The stateful composable resolves its ViewModel via Hilt; the stateless [HomeContent] takes state
 * as parameters so it is trivially previewable and testable.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState = uiState, modifier = modifier)
}

/** Stateless home content. Real UI is built in a later phase. */
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = if (uiState.isLoading) "Loading…" else "Home")
    }
}

@Preview
@Composable
private fun HomeContentPreview() {
    VedicMitraTheme {
        HomeContent(uiState = HomeUiState())
    }
}
