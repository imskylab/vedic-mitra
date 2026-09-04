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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.datastore.Gender
import io.github.vedicmitra.core.datastore.ProfileRelation
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.core.location.GeocodeResult
import io.github.vedicmitra.core.ui.text.UiText
import io.github.vedicmitra.core.ui.text.resolve

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
    // A UiText is resolved in a composition, and `collect` is not one. Holding the latest message in
    // state means the toast text is looked up at the locale in force when it is shown -- which is the
    // point of deferring it -- and the LaunchedEffect below only has to fire it.
    var message by remember { mutableStateOf<UiText?>(null) }
    val messageText = message?.resolve()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message = it }
    }
    LaunchedEffect(messageText) {
        messageText?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.saved.collect { onDone() }
    }

    ProfileEditContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onRelationChange = viewModel::onRelationChange,
        onGenderChange = viewModel::onGenderChange,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onTimeOfBirthChange = viewModel::onTimeOfBirthChange,
        onPlaceOfBirthChange = viewModel::onPlaceOfBirthChange,
        onSearchPlace = viewModel::searchPlace,
        onSelectPlace = viewModel::selectPlace,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
private fun ProfileEditContent(
    uiState: ProfileEditUiState,
    onNameChange: (String) -> Unit,
    onRelationChange: (ProfileRelation) -> Unit,
    onGenderChange: (Gender?) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onTimeOfBirthChange: (String) -> Unit,
    onPlaceOfBirthChange: (String) -> Unit,
    onSearchPlace: () -> Unit,
    onSelectPlace: (GeocodeResult) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text =
                stringResource(
                    if (uiState.isEditing) R.string.profile_edit_title_edit else R.string.profile_edit_title_add,
                ),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.profile_edit_relation),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RelationSelector(selected = uiState.relation, onSelect = onRelationChange)
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.profile_edit_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.profile_edit_gender),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GenderSelector(selected = uiState.gender, onSelect = onGenderChange)
        OutlinedTextField(
            value = uiState.dateOfBirth,
            onValueChange = onDateOfBirthChange,
            label = { Text(stringResource(R.string.profile_edit_date_of_birth)) },
            placeholder = { Text(stringResource(R.string.profile_edit_date_of_birth_placeholder)) },
            supportingText = { Text(stringResource(R.string.profile_edit_date_of_birth_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.timeOfBirth,
            onValueChange = onTimeOfBirthChange,
            label = { Text(stringResource(R.string.profile_edit_time_of_birth)) },
            placeholder = { Text(stringResource(R.string.profile_edit_time_of_birth_placeholder)) },
            supportingText = { Text(stringResource(R.string.profile_edit_time_of_birth_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        BirthplaceField(
            uiState = uiState,
            onPlaceChange = onPlaceOfBirthChange,
            onSearch = onSearchPlace,
            onSelect = onSelectPlace,
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(
                text =
                    stringResource(
                        if (uiState.isEditing) R.string.profile_edit_save_edit else R.string.profile_edit_save_add,
                    ),
            )
        }
    }
}

/** The place-of-birth field: type a place, search, pick a result — which resolves coordinates + zone. */
@Composable
private fun BirthplaceField(
    uiState: ProfileEditUiState,
    onPlaceChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelect: (GeocodeResult) -> Unit,
) {
    OutlinedTextField(
        value = uiState.placeOfBirth,
        onValueChange = onPlaceChange,
        label = { Text(stringResource(R.string.profile_edit_place_of_birth)) },
        placeholder = { Text(stringResource(R.string.profile_edit_place_of_birth_placeholder)) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.profile_edit_place_search_description),
                )
            }
        },
        supportingText = { Text(birthplaceHint(uiState)) },
        modifier = Modifier.fillMaxWidth(),
    )
    when {
        uiState.isSearchingPlace -> CircularProgressIndicator()
        uiState.placeError != null ->
            Text(
                text = uiState.placeError.resolve(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

        else ->
            uiState.placeResults.forEach { result ->
                PlaceResultRow(result = result, onClick = { onSelect(result) })
            }
    }
}

@Composable
private fun PlaceResultRow(
    result: GeocodeResult,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = result.label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun birthplaceHint(uiState: ProfileEditUiState): String =
    if (uiState.birthZoneId != null) {
        stringResource(R.string.profile_edit_place_located, uiState.birthZoneId)
    } else {
        stringResource(R.string.profile_edit_place_hint)
    }

/** A dropdown for choosing the profile's [ProfileRelation]. */
@Composable
private fun RelationSelector(
    selected: ProfileRelation,
    onSelect: (ProfileRelation) -> Unit,
) {
    VedicSelectField(
        label = stringResource(R.string.profile_edit_relation),
        options = ProfileRelation.entries,
        selected = selected,
        optionLabel = { stringResource(it.labelRes) },
        onSelect = onSelect,
    )
}

/** A dropdown for the profile's [Gender]; "Not specified" clears it (used for kundali matching). */
@Composable
private fun GenderSelector(
    selected: Gender?,
    onSelect: (Gender?) -> Unit,
) {
    VedicSelectField(
        label = stringResource(R.string.profile_edit_gender_optional),
        options = listOf<Gender?>(null) + Gender.entries,
        selected = selected,
        optionLabel = { stringResource(it?.labelRes ?: R.string.profile_edit_gender_unspecified) },
        onSelect = onSelect,
    )
}

@Preview
@Composable
private fun ProfileEditContentPreview() {
    VedicMitraTheme {
        ProfileEditContent(
            uiState = ProfileEditUiState(name = "Mia", relation = ProfileRelation.SPOUSE, gender = Gender.FEMALE),
            onNameChange = {},
            onRelationChange = {},
            onGenderChange = {},
            onDateOfBirthChange = {},
            onTimeOfBirthChange = {},
            onPlaceOfBirthChange = {},
            onSearchPlace = {},
            onSelectPlace = {},
            onSave = {},
        )
    }
}
