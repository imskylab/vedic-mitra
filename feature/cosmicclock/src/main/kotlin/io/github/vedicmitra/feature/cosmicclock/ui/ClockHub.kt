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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.vedicmitra.feature.cosmicclock.domain.ClockSummary

/**
 * The middle of the clock, in words.
 *
 * The rings say where in each cycle we are; this says what that means. Someone who reads only this
 * and taps nothing should still leave with one correct, specific sentence — which is the actual test
 * of whether the screen clarifies anything, and something no arrangement of arcs achieves on its own.
 *
 * Deliberately short. The hub is a circle of radius 0.42, so the largest square that fits inside it
 * is about 0.3 of the clock's width — roughly 95dp on a 320dp face. That is room for three short
 * lines at the default font scale and fewer at 200%, so everything beyond the tithi and the
 * nakshatra belongs in the list below the clock, which can scroll.
 *
 * @param maxWidth the widest the text may be — the inscribed square's side. Passed in because it
 *   depends on the face's measured size, which this composable cannot see.
 */
@Composable
fun ClockHub(
    summary: ClockSummary,
    maxWidth: Dp,
    endsAtLabel: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(maxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = summary.tithi,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (endsAtLabel != null) {
            Text(
                text = endsAtLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = summary.pada?.let { "${summary.nakshatra} · pada $it" } ?: summary.nakshatra,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The share of the face's width the hub's text may occupy.
 *
 * A square inscribed in the hub circle has side `2 * hubRadius / sqrt(2)`, and the hub radius is
 * [io.github.vedicmitra.feature.cosmicclock.domain.RingGeometry.DEFAULT_HUB_FRACTION] of the outer
 * radius, which is half the width. So the side is `1.414 * 0.42 * width / 2`, a little under a third.
 * Text wider than this would spill over the innermost ring.
 */
const val HUB_TEXT_WIDTH_FRACTION = 0.297f
