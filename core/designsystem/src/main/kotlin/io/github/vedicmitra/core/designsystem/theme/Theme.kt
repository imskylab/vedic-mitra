/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = indigo80,
        secondary = saffron80,
        tertiary = teal80,
        error = error80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = indigo40,
        secondary = saffron40,
        tertiary = teal40,
        error = error40,
    )

/**
 * Root Material 3 theme for Vedic Mitra. Every screen must be wrapped in this so colours,
 * typography, and shapes stay consistent. Supports light/dark and Android 12+ dynamic colour.
 *
 * This is design-system foundation, not feature logic.
 *
 * @param darkTheme whether to use the dark colour scheme; defaults to the system setting.
 * @param dynamicColor whether to derive colours from the device wallpaper on Android 12+.
 * @param content the themed UI.
 */
@Composable
fun VedicMitraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VedicTypography,
        content = content,
    )
}
