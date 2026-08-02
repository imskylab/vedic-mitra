/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
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
