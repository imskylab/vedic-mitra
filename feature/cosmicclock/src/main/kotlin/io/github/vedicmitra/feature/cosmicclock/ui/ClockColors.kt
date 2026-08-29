/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Every colour the clock draws with, resolved once from the theme.
 *
 * A `DrawScope` cannot read `MaterialTheme` — composition locals are not available inside the draw
 * lambda — so the alternative to this is reading colours at the call site and threading half a dozen
 * `Color` parameters through every drawing function. Bundling them means the light and dark answer
 * lives in exactly one place, and the draw functions can be exercised without a theme at all.
 *
 * [Immutable] because it is read inside `drawWithCache`: Compose needs to know that an equal instance
 * means an unchanged cache, or the static layer would be re-rendered on every recomposition.
 *
 * @property activeArc the current division of a ring, filled as far as its progress.
 * @property activeTrack the rest of that division — so the reader can see how long it is, not only
 *   how far through it they are.
 * @property inactiveTick every other division: present as context, deliberately quiet.
 * @property padaActive the current quarter of the active nakshatra.
 * @property padaInactive its other three quarters.
 */
@Immutable
data class ClockColors(
    val activeArc: Color,
    val activeTrack: Color,
    val inactiveTick: Color,
    val padaActive: Color,
    val padaInactive: Color,
)

/**
 * The clock's colours for the current theme.
 *
 * Inactive divisions are dimmed with **alpha, not blur**. A real blur means `RenderEffect`, which is
 * API 31+ and costs a render pass per frame; at this scale dimming reads the same for free. The
 * choice is here rather than in the drawing code so it can be revisited in one place — and it would
 * only ever apply to the static layer, never the animated one.
 */
@Composable
fun clockColors(): ClockColors {
    val scheme = MaterialTheme.colorScheme
    return ClockColors(
        activeArc = scheme.primary,
        activeTrack = scheme.primary.copy(alpha = ACTIVE_TRACK_ALPHA),
        inactiveTick = scheme.onSurfaceVariant.copy(alpha = INACTIVE_ALPHA),
        padaActive = scheme.tertiary,
        padaInactive = scheme.tertiary.copy(alpha = PADA_INACTIVE_ALPHA),
    )
}

/**
 * Quiet enough to read as background, strong enough to survive a light theme.
 *
 * The inactive ticks carry the *shape* of each cycle — that a tithi ring has thirty divisions and a
 * vara ring seven is the thing that makes the face legible as a set of cycles rather than a set of
 * arcs. Taking them much below this loses that on a bright screen.
 */
private const val INACTIVE_ALPHA = 0.22f

/** The unfilled remainder of the current division: visible, but clearly behind the fill. */
private const val ACTIVE_TRACK_ALPHA = 0.28f

private const val PADA_INACTIVE_ALPHA = 0.3f
