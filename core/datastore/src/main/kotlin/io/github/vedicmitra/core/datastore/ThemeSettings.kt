/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
