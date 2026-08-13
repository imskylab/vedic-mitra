/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Birth-profile screen. Collects [ProfileViewModel] state, surfaces messages as toasts, and pops
 * back once the profile is saved. The stateless [ProfileContent] is previewable and testable.
 */
@Composable
fun ProfileScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.saved.collect { onDone() }
    }

    ProfileContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onTimeOfBirthChange = viewModel::onTimeOfBirthChange,
        onPlaceOfBirthChange = viewModel::onPlaceOfBirthChange,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onNameChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onTimeOfBirthChange: (String) -> Unit,
    onPlaceOfBirthChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Your birth profile", style = MaterialTheme.typography.titleLarge)
        Text(
            text =
                "Used to build your Kundali, Rashifal and personalised Muhurat (coming soon). " +
                    "Stored only on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.dateOfBirth,
            onValueChange = onDateOfBirthChange,
            label = { Text("Date of birth") },
            placeholder = { Text("YYYY-MM-DD") },
            supportingText = { Text("e.g. 1995-03-14") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.timeOfBirth,
            onValueChange = onTimeOfBirthChange,
            label = { Text("Time of birth") },
            placeholder = { Text("HH:MM (24-hour)") },
            supportingText = { Text("As exact as possible — the chart depends on it") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.placeOfBirth,
            onValueChange = onPlaceOfBirthChange,
            label = { Text("Place of birth") },
            placeholder = { Text("City, Country") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Save profile")
        }
    }
}

@Preview
@Composable
private fun ProfileContentPreview() {
    VedicMitraTheme {
        ProfileContent(
            uiState =
                ProfileUiState(
                    name = "Leo",
                    dateOfBirth = "1995-03-14",
                    timeOfBirth = "09:30",
                    placeOfBirth = "Hyderabad, India",
                ),
            onNameChange = {},
            onDateOfBirthChange = {},
            onTimeOfBirthChange = {},
            onPlaceOfBirthChange = {},
            onSave = {},
        )
    }
}
