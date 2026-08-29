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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.astronomy.PanchangaPrimer
import io.github.vedicmitra.feature.cosmicclock.domain.ClockRing
import kotlin.time.Instant

/**
 * What a limb is, for a reader who tapped because they wanted to know.
 *
 * The explanation comes from `PanchangaPrimer`, which is keyed by concept rather than by display
 * string precisely so this cannot silently fall back to a placeholder: every concept the clock can
 * show has copy, and a build fails if one is added without it.
 *
 * The sheet leads with *this chart's* value and its timings before the general explanation. Someone
 * who taps "Tithi" is usually asking two questions at once — what is this thing, and what is mine —
 * and answering the second first is what makes the first worth reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSheet(
    ring: ClockRing,
    at: Instant,
    onDismiss: () -> Unit,
) {
    val primer = PanchangaPrimer.of(ring.concept)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = primer.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = ring.activeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ring.window?.let { window ->
                Text(
                    text =
                        "Began ${formatClockTime(window.start)}, ends ${formatClockTime(window.end)}" +
                            " — ${formatRemaining(window.remainingFrom(at))} to go.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = primer.body, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "One of ${ring.segmentCount} in the full cycle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
