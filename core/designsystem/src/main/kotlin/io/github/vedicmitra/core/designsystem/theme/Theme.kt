/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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

private val DarkColorScheme = darkColorScheme(
    primary = VedicColors.Indigo80,
    secondary = VedicColors.Saffron80,
    tertiary = VedicColors.Teal80,
    error = VedicColors.Error80,
)

private val LightColorScheme = lightColorScheme(
    primary = VedicColors.Indigo40,
    secondary = VedicColors.Saffron40,
    tertiary = VedicColors.Teal40,
    error = VedicColors.Error40,
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
    val colorScheme = when {
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
