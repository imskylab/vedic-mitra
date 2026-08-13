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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.ProfileRelation
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Profiles-list screen. Lists the saved birth profiles, marks the primary "Self", and lets the user
 * make one primary (tap), edit or delete one, or add a new one. The stateless [ProfileListContent]
 * is previewable and testable.
 */
@Composable
fun ProfileListScreen(
    onAddProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileListContent(
        uiState = uiState,
        onSetPrimary = viewModel::setPrimary,
        onDelete = viewModel::delete,
        onEditProfile = onEditProfile,
        onAddProfile = onAddProfile,
        modifier = modifier,
    )
}

@Composable
private fun ProfileListContent(
    uiState: ProfileListUiState,
    onSetPrimary: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(text = "Profiles", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "The primary profile is you; others are family or friends whose charts you keep.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.profiles.isEmpty()) {
            Text(
                text = "No profiles yet. Add yourself first — that becomes your primary profile.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        uiState.profiles.forEach { profile ->
            ProfileRow(
                profile = profile,
                isPrimary = profile.id == uiState.primaryId,
                onSetPrimary = { onSetPrimary(profile.id) },
                onEdit = { onEditProfile(profile.id) },
                onDelete = { onDelete(profile.id) },
            )
            HorizontalDivider()
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddProfile, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add a profile")
        }
    }
}

@Composable
private fun ProfileRow(
    profile: BirthProfile,
    isPrimary: Boolean,
    onSetPrimary: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = isPrimary, onClick = onSetPrimary)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isPrimary, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = profile.name.ifBlank { "Unnamed" }, style = MaterialTheme.typography.bodyLarge)
            Text(text = profile.subtitle(isPrimary), style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) {
            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit ${profile.name}")
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete ${profile.name}")
        }
    }
}

private fun BirthProfile.subtitle(isPrimary: Boolean): String {
    val relationLabel = if (isPrimary) "Primary · ${relation.displayName}" else relation.displayName
    return if (isComplete) relationLabel else "$relationLabel · incomplete"
}

@Preview
@Composable
private fun ProfileListContentPreview() {
    VedicMitraTheme {
        ProfileListContent(
            uiState =
                ProfileListUiState(
                    profiles =
                        listOf(
                            BirthProfile(id = "a", name = "Leo", relation = ProfileRelation.SELF),
                            BirthProfile(id = "b", name = "Mia", relation = ProfileRelation.SPOUSE),
                        ),
                    primaryId = "a",
                ),
            onSetPrimary = {},
            onDelete = {},
            onEditProfile = {},
            onAddProfile = {},
        )
    }
}
