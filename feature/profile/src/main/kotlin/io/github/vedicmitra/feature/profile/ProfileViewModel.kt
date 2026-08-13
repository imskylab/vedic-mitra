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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.datastore.UserProfile
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
import javax.inject.Inject

/**
 * Presentation logic for the birth-profile screen. Holds the raw text fields (so the inputs are
 * simple to bind), validates the date/time on save, and persists via [ProfileRepository].
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())

        /** Observable form state consumed by the profile screen. */
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

        /** One-shot messages (validation errors, save confirmation) for the screen to surface. */
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /** Emits once the profile has been saved successfully, so the screen can navigate back. */
        val saved: SharedFlow<Unit> = _saved.asSharedFlow()

        init {
            viewModelScope.launch {
                val profile = profileRepository.profile.first()
                _uiState.update {
                    it.copy(
                        name = profile.name,
                        dateOfBirth = profile.dateOfBirth?.toString().orEmpty(),
                        timeOfBirth = profile.timeOfBirth?.toString().orEmpty(),
                        placeOfBirth = profile.placeOfBirth,
                    )
                }
            }
        }

        fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

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
                    state.dateOfBirth.isNotBlank() && date == null ->
                        _messages.emit("Enter the date of birth as YYYY-MM-DD")

                    state.timeOfBirth.isNotBlank() && time == null ->
                        _messages.emit("Enter the time of birth as HH:MM (24-hour)")

                    else -> {
                        profileRepository.setProfile(
                            UserProfile(
                                name = state.name.trim(),
                                dateOfBirth = date,
                                timeOfBirth = time,
                                placeOfBirth = state.placeOfBirth.trim(),
                            ),
                        )
                        _messages.emit("Profile saved")
                        _saved.emit(Unit)
                    }
                }
            }
        }
    }

/**
 * Immutable form state for the profile screen. Fields are raw strings mirroring the text inputs;
 * the date/time are parsed on [ProfileViewModel.save].
 */
data class ProfileUiState(
    val name: String = "",
    val dateOfBirth: String = "",
    val timeOfBirth: String = "",
    val placeOfBirth: String = "",
)

private fun String.toLocalDateOrNull(): LocalDate? =
    if (isBlank()) null else runCatching { LocalDate.parse(trim()) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? =
    if (isBlank()) null else runCatching { LocalTime.parse(trim()) }.getOrNull()
