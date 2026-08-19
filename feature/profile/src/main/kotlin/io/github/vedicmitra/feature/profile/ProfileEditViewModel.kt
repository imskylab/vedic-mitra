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
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.Gender
import io.github.vedicmitra.core.datastore.ProfileRelation
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.location.GeocodeResult
import io.github.vedicmitra.core.location.GeocodingClient
import io.github.vedicmitra.core.location.TimeZoneResolver
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
 * The birthplace is searched via the geocoder and, on selection, resolved to coordinates + a time
 * zone (what a chart needs).
 */
@HiltViewModel
class ProfileEditViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val geocodingClient: GeocodingClient,
        private val timeZoneResolver: TimeZoneResolver,
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
                    if (existing != null) _uiState.update { it.fromProfile(existing) }
                }
            }
        }

        fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

        fun onRelationChange(relation: ProfileRelation) = _uiState.update { it.copy(relation = relation) }

        /** Sets the gender, or clears it back to unset when `null` ("Not specified"). */
        fun onGenderChange(gender: Gender?) = _uiState.update { it.copy(gender = gender) }

        fun onDateOfBirthChange(value: String) = _uiState.update { it.copy(dateOfBirth = value) }

        fun onTimeOfBirthChange(value: String) = _uiState.update { it.copy(timeOfBirth = value) }

        /** Editing the place text invalidates any previously resolved coordinates/zone. */
        fun onPlaceOfBirthChange(value: String) {
            _uiState.update {
                it.copy(
                    placeOfBirth = value,
                    birthCoordinates = null,
                    birthZoneId = null,
                    placeResults = emptyList(),
                    placeError = null,
                )
            }
        }

        /** Forward-geocodes the current place text into candidate places. */
        fun searchPlace() {
            val query = _uiState.value.placeOfBirth
            if (query.isBlank()) {
                _uiState.update { it.copy(placeResults = emptyList(), placeError = null) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSearchingPlace = true, placeError = null) }
                when (val result = geocodingClient.search(query)) {
                    is AppResult.Success ->
                        _uiState.update {
                            it.copy(
                                isSearchingPlace = false,
                                placeResults = result.data,
                                placeError = if (result.data.isEmpty()) "No matching places found" else null,
                            )
                        }

                    is AppResult.Failure ->
                        _uiState.update {
                            it.copy(
                                isSearchingPlace = false,
                                placeResults = emptyList(),
                                placeError = result.cause.message ?: "Search failed",
                            )
                        }
                }
            }
        }

        /** Chooses a geocoded [result] as the birthplace, resolving its time zone. */
        fun selectPlace(result: GeocodeResult) {
            viewModelScope.launch {
                val zone = timeZoneResolver.resolve(result.coordinates)
                _uiState.update {
                    it.copy(
                        placeOfBirth = result.label,
                        birthCoordinates = result.coordinates,
                        birthZoneId = zone,
                        placeResults = emptyList(),
                        placeError = null,
                    )
                }
            }
        }

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
                                gender = state.gender,
                                dateOfBirth = date,
                                timeOfBirth = time,
                                placeOfBirth = state.placeOfBirth.trim(),
                                birthCoordinates = state.birthCoordinates,
                                birthZoneId = state.birthZoneId,
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
 * @property placeResults candidate places from the last birthplace search.
 * @property birthCoordinates the resolved birthplace coordinates once a place is chosen.
 * @property birthZoneId the resolved birthplace time zone once a place is chosen.
 */
data class ProfileEditUiState(
    val name: String = "",
    val relation: ProfileRelation = ProfileRelation.SELF,
    val gender: Gender? = null,
    val dateOfBirth: String = "",
    val timeOfBirth: String = "",
    val placeOfBirth: String = "",
    val isEditing: Boolean = false,
    val isSearchingPlace: Boolean = false,
    val placeResults: List<GeocodeResult> = emptyList(),
    val placeError: String? = null,
    val birthCoordinates: GeoCoordinates? = null,
    val birthZoneId: String? = null,
)

private fun ProfileEditUiState.fromProfile(profile: BirthProfile): ProfileEditUiState =
    copy(
        name = profile.name,
        relation = profile.relation,
        gender = profile.gender,
        dateOfBirth = profile.dateOfBirth?.toString().orEmpty(),
        timeOfBirth = profile.timeOfBirth?.toString().orEmpty(),
        placeOfBirth = profile.placeOfBirth,
        isEditing = true,
        birthCoordinates = profile.birthCoordinates,
        birthZoneId = profile.birthZoneId,
    )

private fun String.toLocalDateOrNull(): LocalDate? =
    if (isBlank()) null else runCatching { LocalDate.parse(trim()) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? =
    if (isBlank()) null else runCatching { LocalTime.parse(trim()) }.getOrNull()
