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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Location screen. Lists the saved locations, marks the selected one (or "Current location" when
 * none is selected), and offers entry points to add a location by city search or by coordinates.
 * The stateless [LocationContent] is previewable and testable.
 */
@Composable
fun LocationScreen(
    onAddCity: () -> Unit,
    onAddCoordinates: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocationContent(
        uiState = uiState,
        onUseCurrentLocation = viewModel::useCurrentLocation,
        onSelect = viewModel::select,
        onDelete = viewModel::delete,
        onAddCity = onAddCity,
        onAddCoordinates = onAddCoordinates,
        modifier = modifier,
    )
}

@Composable
private fun LocationContent(
    uiState: LocationUiState,
    onUseCurrentLocation: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddCity: () -> Unit,
    onAddCoordinates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text(text = "Panchanga location", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LocationRow(
            title = "Current location",
            subtitle = "Use the device's GPS",
            selected = uiState.selectedId == null,
            onClick = onUseCurrentLocation,
            onDelete = null,
        )
        HorizontalDivider()

        uiState.locations.forEach { location ->
            LocationRow(
                title = location.label,
                subtitle = location.subtitle(),
                selected = uiState.selectedId == location.id,
                onClick = { onSelect(location.id) },
                onDelete = { onDelete(location.id) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddCity, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add a city")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onAddCoordinates, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Add coordinates")
        }
    }
}

@Composable
private fun LocationRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete $title")
            }
        } else {
            Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null)
        }
    }
}

private fun SavedLocation.subtitle(): String =
    "%.3f, %.3f · %s".format(coordinates.latitude, coordinates.longitude, zoneId)

@Preview
@Composable
private fun LocationContentPreview() {
    VedicMitraTheme {
        LocationContent(
            uiState =
                LocationUiState(
                    locations =
                        listOf(
                            SavedLocation(
                                id = "1",
                                label = "Varanasi",
                                coordinates = GeoCoordinates(latitude = 25.3176, longitude = 82.9739),
                                zoneId = "Asia/Kolkata",
                                source = LocationSource.CITY,
                            ),
                            SavedLocation(
                                id = "2",
                                label = "London",
                                coordinates = GeoCoordinates(latitude = 51.5072, longitude = -0.1276),
                                zoneId = "Europe/London",
                                source = LocationSource.MANUAL,
                            ),
                        ),
                    selectedId = "1",
                ),
            onUseCurrentLocation = {},
            onSelect = {},
            onDelete = {},
            onAddCity = {},
            onAddCoordinates = {},
        )
    }
}
