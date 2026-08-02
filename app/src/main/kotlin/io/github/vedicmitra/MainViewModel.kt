/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.datastore.ThemeSettings
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-level state: the theme settings applied to the whole UI. */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        val themeSettings: StateFlow<ThemeSettings> =
            userPreferencesRepository.themeSettings.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    ThemeSettings(
                        darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                        useDynamicColor = true,
                    ),
            )
    }
