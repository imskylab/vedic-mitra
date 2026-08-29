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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.feature.cosmicclock.domain.PanchangaClockModel

/**
 * The clock's states, for eyes rather than assertions.
 *
 * Nothing here can be asserted usefully — "is the dimmed ring legible against a dark background" is
 * not a unit test — and the repo has no screenshot or UI test infrastructure. These previews are
 * therefore the actual verification step for everything visual, and they exist for the cases that
 * would otherwise only appear on someone's device at an awkward moment.
 */
@Preview(name = "Clock", showBackground = true)
@Composable
private fun PanchangaClockPreview() = Framed(ClockPreviewData.typical())

@Preview(name = "Clock (dark)", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PanchangaClockDarkPreview() = Framed(ClockPreviewData.typical())

/**
 * Several limbs on the point of turning over.
 *
 * Worth looking at because it is where the arcs nearly close their division, and any rounding in the
 * gap between neighbouring segments shows up as a fill that overshoots into the next one.
 */
@Preview(name = "Clock — about to roll over", showBackground = true)
@Composable
private fun PanchangaClockRollingOverPreview() = Framed(ClockPreviewData.aboutToRollOver())

/** A latitude with no sunrise: the vara ring fills whole rather than disappearing. */
@Preview(name = "Clock — polar (no sunrise)", showBackground = true)
@Composable
private fun PanchangaClockPolarPreview() = Framed(ClockPreviewData.polar())

@Composable
private fun Framed(model: PanchangaClockModel) {
    VedicMitraTheme {
        PanchangaClock(
            model = model,
            progress = model.staticProgress(),
            colors = clockColors(),
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
        )
    }
}
