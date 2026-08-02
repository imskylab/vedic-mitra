/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
