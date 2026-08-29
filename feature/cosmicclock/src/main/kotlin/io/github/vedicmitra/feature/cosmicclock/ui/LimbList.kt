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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.core.astronomy.PanchangaPrimer
import io.github.vedicmitra.feature.cosmicclock.domain.ClockRing
import io.github.vedicmitra.feature.cosmicclock.domain.PanchangaClockModel
import kotlin.time.Instant

/**
 * The same five cycles the clock draws, as rows of text.
 *
 * Not a fallback — it serves three audiences with one rendering, which is why the clock and the list
 * are two views of one model rather than two features:
 *
 * - **TalkBack**, which cannot see a `Canvas` at all. Faking semantics nodes over each arc would mean
 *   maintaining a second hit-test that drifts from the drawing.
 * - **Anyone who would rather read a table**, which for exact times is most people.
 * - **Anyone whose tap missed.** Five rings share the radius between the hub and the rim, leaving
 *   about 19dp each at a 160dp face — well under a comfortable touch target, and unavoidably so:
 *   five 48dp bands would need 240dp of radius before the hub got any. The rings are a convenience;
 *   these rows are the reliable target.
 *
 * Each row carries the one-liner from `PanchangaPrimer` **without a tap**, because clarity that only
 * arrives on tap is clarity most readers never get.
 */
@Composable
fun LimbList(
    model: PanchangaClockModel,
    at: Instant,
    modifier: Modifier = Modifier,
    onSelect: (ClockRing) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        model.rings.forEach { ring ->
            LimbRow(
                ring = ring,
                at = at,
                // The face cannot draw pada — a quarter of a nakshatra arc is about five pixels —
                // so this is the only place it is stated.
                pada =
                    model.pada
                        ?.index
                        ?.plus(1)
                        ?.takeIf { ring.concept == PanchangaConcept.NAKSHATRA },
                onClick = { onSelect(ring) },
            )
        }
    }
}

@Composable
private fun LimbRow(
    ring: ClockRing,
    at: Instant,
    pada: Int?,
    onClick: () -> Unit,
) {
    val primer = PanchangaPrimer.of(ring.concept)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (pada == null) ring.activeName else "${ring.activeName} · pada $pada",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = endingLabel(ring, at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${ring.label} — ${primer.oneLine}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { ring.fraction?.toFloat() ?: 0f },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/**
 * "ends 21:14 · in 4h 12m", or an em dash when the boundary is unknown.
 *
 * Both halves earn their place: the clock time is what a reader checks against an almanac, and the
 * countdown is what they act on.
 */
@Composable
private fun endingLabel(
    ring: ClockRing,
    at: Instant,
): String {
    val window = ring.window ?: return "—"
    return "ends ${formatClockTime(window.end)} · in ${formatRemaining(window.remainingFrom(at))}"
}
