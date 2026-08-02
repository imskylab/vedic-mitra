/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.datastore

import kotlinx.coroutines.flow.Flow

/** Reads and updates the user's persisted preferences. */
interface UserPreferencesRepository {
    /** The user's current theme settings, emitting on every change. */
    val themeSettings: Flow<ThemeSettings>

    /** Sets how the app chooses between light and dark. */
    suspend fun setDarkThemeConfig(config: DarkThemeConfig)

    /** Enables or disables dynamic (wallpaper-based) colour. */
    suspend fun setDynamicColor(enabled: Boolean)
}
