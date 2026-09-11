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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The two verdict colours: favourable and to-be-avoided.
 *
 * **Not a `ColorScheme` entry, because Material 3 has no "success" role.** Extending the scheme is
 * the documented way to add a semantic colour it does not model, and keeping these out of
 * `colorScheme` means nothing can mistake them for a Material role with Material's contrast
 * guarantees.
 *
 * They are also **deliberately fixed rather than derived from dynamic colour**. `VedicMitraTheme`
 * can be handed a palette from the wallpaper; whether a muhurta is auspicious is not a matter the
 * wallpaper gets a say in.
 *
 * Green is a departure from the saffron/gold/maroon brand, taken on purpose. The first drawing of
 * the muhurta bar used the brand's saffron against the theme's red, and at band width — some are
 * only a few millimetres — the two are hard to tell apart, worst of all in the dimmed bands that
 * mark time already gone. Distinguishing favourable from unfavourable is the entire job of that bar.
 *
 * Colour is never the only cue. The bar puts the two qualities in separate labelled lanes, following
 * the rule `MatchmakingScreen`'s condition rows already state: shape as well as colour, for the
 * roughly one reader in twelve who cannot separate red from green.
 */
@Immutable
data class VedicAccents(
    val auspicious: Color,
    val caution: Color,
)

/** Deep leaf-green: dark enough to hold its own on the cream ground without turning black. */
internal val auspiciousLight = Color(0xFF2F6B3A)

/** Lifted well clear of the temple-stone ground, the way the dark scheme lifts every other accent. */
internal val auspiciousDark = Color(0xFF8FD69B)

private val LightAccents = VedicAccents(auspicious = auspiciousLight, caution = errorLight)
private val DarkAccents = VedicAccents(auspicious = auspiciousDark, caution = errorDark)

/** The accents for a scheme. */
internal fun accentsFor(darkTheme: Boolean): VedicAccents = if (darkTheme) DarkAccents else LightAccents

/**
 * The accents in scope. Provided by [VedicMitraTheme]; the default is the light pair so a preview
 * that forgets the theme still draws something rather than throwing.
 */
val LocalVedicAccents = staticCompositionLocalOf { LightAccents }
