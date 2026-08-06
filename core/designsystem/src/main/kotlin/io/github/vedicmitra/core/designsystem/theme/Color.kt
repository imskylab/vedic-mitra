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

import androidx.compose.ui.graphics.Color

// Brand colour palette for Vedic Mitra, drawn from the app's emblem: burnished gold, deep maroon,
// and bronze/brown over a warm parchment (light) or near-black (dark) ground. Values are placed at
// Material 3 tonal positions and can be regenerated/tuned with the Material Theme Builder. Colours
// are consumed only through [VedicMitraTheme]; do not reference them directly from feature UI.

// --- Light scheme ------------------------------------------------------------
internal val goldPrimaryLight = Color(0xFF8A5100)
internal val onGoldPrimaryLight = Color(0xFFFFFFFF)
internal val goldContainerLight = Color(0xFFFFDCBE)
internal val onGoldContainerLight = Color(0xFF2C1600)

internal val maroonSecondaryLight = Color(0xFF9C4234)
internal val onMaroonSecondaryLight = Color(0xFFFFFFFF)
internal val maroonContainerLight = Color(0xFFFFDAD3)
internal val onMaroonContainerLight = Color(0xFF3B0A02)

internal val bronzeTertiaryLight = Color(0xFF7A5B2E)
internal val onBronzeTertiaryLight = Color(0xFFFFFFFF)
internal val bronzeContainerLight = Color(0xFFFFDEA6)
internal val onBronzeContainerLight = Color(0xFF271900)

internal val backgroundLight = Color(0xFFFFF8F4)
internal val onBackgroundLight = Color(0xFF211A13)
internal val surfaceVariantLight = Color(0xFFF2DFD0)
internal val onSurfaceVariantLight = Color(0xFF524435)
internal val surfaceContainerLight = Color(0xFFFBEEDF)
internal val outlineLight = Color(0xFF857465)

internal val errorLight = Color(0xFFBA1A1A)
internal val onErrorLight = Color(0xFFFFFFFF)
internal val errorContainerLight = Color(0xFFFFDAD6)
internal val onErrorContainerLight = Color(0xFF410002)

// --- Dark scheme -------------------------------------------------------------
internal val goldPrimaryDark = Color(0xFFFFB951)
internal val onGoldPrimaryDark = Color(0xFF4A2800)
internal val goldContainerDark = Color(0xFF693C00)
internal val onGoldContainerDark = Color(0xFFFFDCBE)

internal val maroonSecondaryDark = Color(0xFFFFB4A4)
internal val onMaroonSecondaryDark = Color(0xFF5C1A0F)
internal val maroonContainerDark = Color(0xFF7B2D22)
internal val onMaroonContainerDark = Color(0xFFFFDAD3)

internal val bronzeTertiaryDark = Color(0xFFE8C08D)
internal val onBronzeTertiaryDark = Color(0xFF422C00)
internal val bronzeContainerDark = Color(0xFF5E4100)
internal val onBronzeContainerDark = Color(0xFFFFDEA6)

internal val backgroundDark = Color(0xFF1A120B)
internal val onBackgroundDark = Color(0xFFEFE0D2)
internal val surfaceVariantDark = Color(0xFF524435)
internal val onSurfaceVariantDark = Color(0xFFD6C3B2)
internal val surfaceContainerDark = Color(0xFF271E16)
internal val outlineDark = Color(0xFF9F8D7D)

internal val errorDark = Color(0xFFFFB4AB)
internal val onErrorDark = Color(0xFF690005)
internal val errorContainerDark = Color(0xFF93000A)
internal val onErrorContainerDark = Color(0xFFFFDAD6)
