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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.ProfileRelation
import io.github.vedicmitra.core.datastore.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/** The `profileId` nav argument; `null` (absent) means the form is adding a new profile. */
internal const val PROFILE_ID_ARG = "profileId"

/**
 * Presentation logic for adding or editing one [BirthProfile]. When the `profileId` argument is
 * present the form loads that profile; otherwise it starts blank and a new id is minted on save.
 */
@HiltViewModel
class ProfileEditViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val profileId: String? = savedStateHandle[PROFILE_ID_ARG]

        private val _uiState = MutableStateFlow(ProfileEditUiState())
        val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val saved: SharedFlow<Unit> = _saved.asSharedFlow()

        init {
            if (profileId != null) {
                viewModelScope.launch {
                    val existing = profileRepository.profiles.first().firstOrNull { it.id == profileId }
                    if (existing != null) {
                        _uiState.update {
                            it.copy(
                                name = existing.name,
                                relation = existing.relation,
                                dateOfBirth = existing.dateOfBirth?.toString().orEmpty(),
                                timeOfBirth = existing.timeOfBirth?.toString().orEmpty(),
                                placeOfBirth = existing.placeOfBirth,
                                isEditing = true,
                            )
                        }
                    }
                }
            }
        }

        fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

        fun onRelationChange(relation: ProfileRelation) = _uiState.update { it.copy(relation = relation) }

        fun onDateOfBirthChange(value: String) = _uiState.update { it.copy(dateOfBirth = value) }

        fun onTimeOfBirthChange(value: String) = _uiState.update { it.copy(timeOfBirth = value) }

        fun onPlaceOfBirthChange(value: String) = _uiState.update { it.copy(placeOfBirth = value) }

        /** Validates and persists the form; emits a message and, on success, [saved]. */
        fun save() {
            viewModelScope.launch {
                val state = _uiState.value
                val date = state.dateOfBirth.toLocalDateOrNull()
                val time = state.timeOfBirth.toLocalTimeOrNull()
                when {
                    state.name.isBlank() -> _messages.emit("Enter a name")
                    state.dateOfBirth.isNotBlank() && date == null ->
                        _messages.emit("Enter the date of birth as YYYY-MM-DD")

                    state.timeOfBirth.isNotBlank() && time == null ->
                        _messages.emit("Enter the time of birth as HH:MM (24-hour)")

                    else -> {
                        profileRepository.upsert(
                            BirthProfile(
                                id = profileId ?: UUID.randomUUID().toString(),
                                name = state.name.trim(),
                                relation = state.relation,
                                dateOfBirth = date,
                                timeOfBirth = time,
                                placeOfBirth = state.placeOfBirth.trim(),
                            ),
                        )
                        _saved.emit(Unit)
                    }
                }
            }
        }
    }

/**
 * Immutable form state for the profile-edit screen.
 *
 * @property isEditing whether an existing profile is being edited (vs. a new one added).
 */
data class ProfileEditUiState(
    val name: String = "",
    val relation: ProfileRelation = ProfileRelation.SELF,
    val dateOfBirth: String = "",
    val timeOfBirth: String = "",
    val placeOfBirth: String = "",
    val isEditing: Boolean = false,
)

private fun String.toLocalDateOrNull(): LocalDate? =
    if (isBlank()) null else runCatching { LocalDate.parse(trim()) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? =
    if (isBlank()) null else runCatching { LocalTime.parse(trim()) }.getOrNull()
