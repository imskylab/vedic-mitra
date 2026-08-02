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

/** How the app chooses between the light and dark colour schemes. */
enum class DarkThemeConfig { FOLLOW_SYSTEM, LIGHT, DARK }

/**
 * The user's theme preferences.
 *
 * @property darkThemeConfig whether to follow the system setting or force light/dark.
 * @property useDynamicColor whether to derive colours from the wallpaper (Android 12+).
 */
data class ThemeSettings(
    val darkThemeConfig: DarkThemeConfig,
    val useDynamicColor: Boolean,
)
