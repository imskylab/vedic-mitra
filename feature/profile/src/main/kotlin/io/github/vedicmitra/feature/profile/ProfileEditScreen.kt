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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.datastore.ProfileRelation
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Add/edit-profile screen. Collects [ProfileEditViewModel] state, surfaces messages as toasts, and
 * pops back once saved. The stateless [ProfileEditContent] is previewable and testable.
 */
@Composable
fun ProfileEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
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

    ProfileEditContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onRelationChange = viewModel::onRelationChange,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onTimeOfBirthChange = viewModel::onTimeOfBirthChange,
        onPlaceOfBirthChange = viewModel::onPlaceOfBirthChange,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
private fun ProfileEditContent(
    uiState: ProfileEditUiState,
    onNameChange: (String) -> Unit,
    onRelationChange: (ProfileRelation) -> Unit,
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
        Text(
            text = if (uiState.isEditing) "Edit profile" else "Add profile",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Relation",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RelationSelector(selected = uiState.relation, onSelect = onRelationChange)
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
            Text(text = if (uiState.isEditing) "Save changes" else "Add profile")
        }
    }
}

/** A horizontal row of selectable chips for choosing the profile's [ProfileRelation]. */
@Composable
private fun RelationSelector(
    selected: ProfileRelation,
    onSelect: (ProfileRelation) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileRelation.entries.forEach { relation ->
            val active = relation == selected
            Text(
                text = relation.displayName,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (active) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(relation) }
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileEditContentPreview() {
    VedicMitraTheme {
        ProfileEditContent(
            uiState = ProfileEditUiState(name = "Mia", relation = ProfileRelation.SPOUSE),
            onNameChange = {},
            onRelationChange = {},
            onDateOfBirthChange = {},
            onTimeOfBirthChange = {},
            onPlaceOfBirthChange = {},
            onSave = {},
        )
    }
}
