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

// Brand colour palette for Vedic Mitra. Placeholder brand values for Phase 1 — a full tonal palette
// can be generated from the seed colour in a later design pass. Colours are consumed only through
// [VedicMitraTheme]; do not reference them directly from feature UI. The 80/40 suffixes denote
// tonal values used by the dark and light schemes respectively.

// Primary — deep indigo, evoking the night sky.
internal val indigo80 = Color(0xFFB9C0FF)
internal val indigo40 = Color(0xFF3F4C9A)

// Secondary — warm saffron accent.
internal val saffron80 = Color(0xFFFFDDAE)
internal val saffron40 = Color(0xFF7A5900)

// Tertiary — muted teal.
internal val teal80 = Color(0xFF8FD8CE)
internal val teal40 = Color(0xFF00696A)

internal val error80 = Color(0xFFFFB4AB)
internal val error40 = Color(0xFFBA1A1A)
