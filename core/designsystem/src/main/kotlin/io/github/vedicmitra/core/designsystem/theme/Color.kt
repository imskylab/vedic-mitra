/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand colour palette for Vedic Mitra. These are placeholder brand values for Phase 1 — a full
 * tonal palette can be generated from the seed colour in a later design pass. Colours are consumed
 * only through [VedicMitraTheme]; do not reference them directly from feature UI.
 */
internal object VedicColors {
    // Primary — deep indigo, evoking the night sky.
    val Indigo80 = Color(0xFFB9C0FF)
    val Indigo40 = Color(0xFF3F4C9A)

    // Secondary — warm saffron accent.
    val Saffron80 = Color(0xFFFFDDAE)
    val Saffron40 = Color(0xFF7A5900)

    // Tertiary — muted teal.
    val Teal80 = Color(0xFF8FD8CE)
    val Teal40 = Color(0xFF00696A)

    val Error80 = Color(0xFFFFB4AB)
    val Error40 = Color(0xFFBA1A1A)
}
