/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.location.GeocodeResult

/**
 * Add-by-city screen. Searches for a place by name through the geocoder and, on tapping a result,
 * saves it (with a best-guess time zone) and selects it, then returns via [onDone].
 */
@Composable
fun AddCityScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddLocationViewModel = hiltViewModel(),
) {
    val state by viewModel.searchState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(text = "City or place") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.search(query) }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isSearching -> CircularProgressIndicator()
            state.error != null ->
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            else ->
                state.results.forEach { result ->
                    CityResultRow(result = result, onClick = { viewModel.saveFromResult(result, onDone) })
                }
        }
    }
}

@Composable
private fun CityResultRow(
    result: GeocodeResult,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = result.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "%.4f, %.4f".format(result.coordinates.latitude, result.coordinates.longitude),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
