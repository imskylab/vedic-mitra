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
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation logic for the profiles list. Exposes the saved profiles and which is primary, and
 * lets the user make one primary or delete one.
 */
@HiltViewModel
class ProfileListViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        /** Observable UI state consumed by the profiles-list screen. */
        val uiState: StateFlow<ProfileListUiState> =
            combine(
                profileRepository.profiles,
                profileRepository.primaryProfileId,
            ) { profiles, primaryId ->
                ProfileListUiState(profiles = profiles, primaryId = primaryId)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ProfileListUiState(),
            )

        /** Makes the profile with [id] the primary "Self" profile. */
        fun setPrimary(id: String) {
            viewModelScope.launch { profileRepository.setPrimary(id) }
        }

        /** Deletes the profile with [id], promoting another to primary if it was primary. */
        fun delete(id: String) {
            viewModelScope.launch { profileRepository.remove(id) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/**
 * UI state for the profiles-list screen.
 *
 * @property profiles the saved profiles.
 * @property primaryId the id of the primary "Self" profile, or `null` when there are none.
 */
data class ProfileListUiState(
    val profiles: List<BirthProfile> = emptyList(),
    val primaryId: String? = null,
)
