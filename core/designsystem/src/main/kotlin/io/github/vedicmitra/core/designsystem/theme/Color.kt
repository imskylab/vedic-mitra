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

// Brand colour palette for Vedic Mitra, sampled from the intro splash video: saffron/marigold robes
// (primary), the temple-gold Om mandala (secondary), and deep kumkum maroon (tertiary), over a warm
// cream-parchment ground (light) or temple-stone brown (dark). Light is the default so the app hands
// off seamlessly from the splash's closing parchment frame. Values sit at Material 3 tonal positions
// and are consumed only through [VedicMitraTheme]; do not reference them directly from feature UI.

// --- Light scheme ------------------------------------------------------------
internal val saffronPrimaryLight = Color(0xFFBC5D12)
internal val onSaffronPrimaryLight = Color(0xFFFFFFFF)
internal val saffronContainerLight = Color(0xFFFFDCC1)
internal val onSaffronContainerLight = Color(0xFF341100)

internal val goldSecondaryLight = Color(0xFF8A6A0E)
internal val onGoldSecondaryLight = Color(0xFFFFFFFF)
internal val goldContainerLight = Color(0xFFFBE1A0)
internal val onGoldContainerLight = Color(0xFF2A1D00)

internal val maroonTertiaryLight = Color(0xFF8E2A21)
internal val onMaroonTertiaryLight = Color(0xFFFFFFFF)
internal val maroonContainerLight = Color(0xFFFFDAD3)
internal val onMaroonContainerLight = Color(0xFF3A0906)

internal val backgroundLight = Color(0xFFFBF4E7)
internal val onBackgroundLight = Color(0xFF2B1E12)
internal val surfaceVariantLight = Color(0xFFF0E3CD)
internal val onSurfaceVariantLight = Color(0xFF54473A)
internal val surfaceContainerLight = Color(0xFFF5E9D2)
internal val outlineLight = Color(0xFF897B67)

internal val errorLight = Color(0xFFBA1A1A)
internal val onErrorLight = Color(0xFFFFFFFF)
internal val errorContainerLight = Color(0xFFFFDAD6)
internal val onErrorContainerLight = Color(0xFF410002)

// --- Dark scheme (temple-stone) ----------------------------------------------
internal val saffronPrimaryDark = Color(0xFFF5A23E)
internal val onSaffronPrimaryDark = Color(0xFF4A2400)
internal val saffronContainerDark = Color(0xFF8E4708)
internal val onSaffronContainerDark = Color(0xFFFFDCC1)

internal val goldSecondaryDark = Color(0xFFE3B84E)
internal val onGoldSecondaryDark = Color(0xFF3A2A00)
internal val goldContainerDark = Color(0xFF5E4600)
internal val onGoldContainerDark = Color(0xFFFBE1A0)

internal val maroonTertiaryDark = Color(0xFFFFB4A4)
internal val onMaroonTertiaryDark = Color(0xFF5C1A0F)
internal val maroonContainerDark = Color(0xFF7B2D22)
internal val onMaroonContainerDark = Color(0xFFFFDAD3)

internal val backgroundDark = Color(0xFF1B140F)
internal val onBackgroundDark = Color(0xFFEFE0C6)
internal val surfaceVariantDark = Color(0xFF4E4536)
internal val onSurfaceVariantDark = Color(0xFFD6C7AE)
internal val surfaceContainerDark = Color(0xFF241C15)
internal val outlineDark = Color(0xFF9C8C77)

internal val errorDark = Color(0xFFFFB4AB)
internal val onErrorDark = Color(0xFF690005)
internal val errorContainerDark = Color(0xFF93000A)
internal val onErrorContainerDark = Color(0xFFFFDAD6)
