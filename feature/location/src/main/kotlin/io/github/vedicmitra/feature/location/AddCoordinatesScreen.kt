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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.location.TimeZoneEstimator
import java.time.ZoneId

private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

/**
 * Add-by-coordinates screen. Takes a name, latitude, longitude, and an IANA time-zone id
 * (pre-filled with a best guess once the coordinates are valid, and editable), then saves and
 * selects the location and returns via [onDone].
 */
@Composable
fun AddCoordinatesScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddLocationViewModel = hiltViewModel(),
) {
    var label by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var zoneId by rememberSaveable { mutableStateOf("") }
    var zoneEdited by rememberSaveable { mutableStateOf(false) }

    val latitudeValue = latitude.toDoubleOrNull()
    val longitudeValue = longitude.toDoubleOrNull()
    val latitudeValid = latitudeValue != null && latitudeValue in MIN_LATITUDE..MAX_LATITUDE
    val longitudeValid = longitudeValue != null && longitudeValue in MIN_LONGITUDE..MAX_LONGITUDE
    val zoneValid = zoneId.isNotBlank() && runCatching { ZoneId.of(zoneId) }.isSuccess

    // Pre-fill the time zone with a best guess once the coordinates parse, unless the user edited it.
    LaunchedEffect(latitude, longitude) {
        if (!zoneEdited && latitudeValid && longitudeValid) {
            zoneId =
                TimeZoneEstimator.estimate(
                    GeoCoordinates(latitude = latitudeValue!!, longitude = longitudeValue!!),
                )
        }
    }

    CoordinatesForm(
        label = label,
        onLabelChange = { label = it },
        latitude = latitude,
        onLatitudeChange = { latitude = it },
        latitudeError = latitude.isNotBlank() && !latitudeValid,
        longitude = longitude,
        onLongitudeChange = { longitude = it },
        longitudeError = longitude.isNotBlank() && !longitudeValid,
        zoneId = zoneId,
        onZoneChange = {
            zoneId = it
            zoneEdited = true
        },
        zoneError = zoneId.isNotBlank() && !zoneValid,
        canSave = latitudeValid && longitudeValid && zoneValid,
        onSave = {
            val lat = latitudeValue
            val lng = longitudeValue
            if (lat != null && lng != null) {
                viewModel.saveManual(label = label, latitude = lat, longitude = lng, zoneId = zoneId, onSaved = onDone)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun CoordinatesForm(
    label: String,
    onLabelChange: (String) -> Unit,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    latitudeError: Boolean,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    longitudeError: Boolean,
    zoneId: String,
    onZoneChange: (String) -> Unit,
    zoneError: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            label = { Text(text = "Name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = latitude,
            onValueChange = onLatitudeChange,
            label = { Text(text = "Latitude") },
            isError = latitudeError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = longitude,
            onValueChange = onLongitudeChange,
            label = { Text(text = "Longitude") },
            isError = longitudeError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = zoneId,
            onValueChange = onZoneChange,
            label = { Text(text = "Time zone") },
            supportingText = { Text(text = "IANA id, e.g. Asia/Kolkata") },
            isError = zoneError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Save location")
        }
    }
}
