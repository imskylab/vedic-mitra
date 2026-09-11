/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.designsystem.component

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.LocalVedicAccents
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * One stretch of a timeline lane, in fractions of the lane's width.
 *
 * @property depth how many source intervals cover this stretch. One draws at full strength; more is
 *   pushed further from the track, so a doubly-covered stretch reads as a different shade of the same
 *   colour rather than as a different colour.
 */
@Immutable
data class TimelineBand(
    val start: Float,
    val end: Float,
    val depth: Int,
)

/** A lane of the bar: its bands, the colour they take, and the word that names the lane. */
@Immutable
data class TimelineLane(
    val label: String,
    val bands: List<TimelineBand>,
    val colour: Color,
)

/**
 * A small multi-lane timeline: rows of coloured stretches over a shared span, with a marker at the
 * present moment.
 *
 * Deliberately domain-free — it takes fractions, not instants, and knows nothing of muhurtas. That
 * keeps it in `:core:designsystem`, which has no dependencies at all, and lets the muhurat day screen
 * use it later for the same windows it currently lists as text.
 *
 * Drawing follows the house pattern set by the kundali chart: the [Canvas] paints geometry only,
 * every colour is read from the theme *outside* the draw lambda, sizes come from the draw scope, and
 * all text is composed rather than drawn. Nothing in this app draws text onto a canvas.
 *
 * Bands narrower than a finger are normal here — a 49-minute window in a 14-hour span is a couple of
 * millimetres — so **the lane is the touch target and the tap's position chooses the band**, the way
 * a scrubber behaves. Per-band targets would sit far below the 48dp minimum.
 *
 * @param onSelect given the lane's index and the fraction tapped; `null` makes the bar inert.
 * @param spokenDescription what a screen reader hears in place of the bar. A tap position means
 *   nothing to TalkBack, so this carries the same information in words — without it the bar would be
 *   a feature only sighted readers get.
 */
@Composable
fun VedicTimelineBar(
    lanes: List<TimelineLane>,
    nowFraction: Float,
    spokenDescription: String,
    modifier: Modifier = Modifier,
    laneHeight: Dp = 12.dp,
    labelWidth: Dp = 40.dp,
    onSelect: ((laneIndex: Int, fraction: Float) -> Unit)? = null,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    // Which way "deeper" goes. Away from the track, never simply darker: on the dark scheme the
    // accents are light colours on a dark ground, so darkening a doubly-covered band would push it
    // toward the track rather than clear of it. Measured, that mistake cost 5.50:1 down to 2.19:1.
    val deepenToward = if (track.luminance() < MID_LUMINANCE) Color.White else Color.Black
    val marker = MaterialTheme.colorScheme.onSurface
    val labelColour = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(
        // One description for the whole bar: every band is an unlabelled rectangle, and the tap that
        // would name one is a position on a line, which a screen reader cannot aim.
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spokenDescription },
        verticalArrangement = Arrangement.spacedBy(LANE_GAP),
    ) {
        lanes.forEachIndexed { index, lane ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = lane.label, style = labelStyle, color = labelColour, modifier = Modifier.width(labelWidth))
                Canvas(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(laneHeight)
                            .then(tapModifier(lane, index, onSelect)),
                ) {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(color = track, cornerRadius = radius)
                    lane.bands.forEach { band ->
                        drawRoundRect(
                            color = shadeFor(lane.colour, band.depth, deepenToward),
                            topLeft = Offset(size.width * band.start, 0f),
                            // A band can be a fraction of a pixel wide; give it one so it never vanishes.
                            size =
                                Size(
                                    (size.width * (band.end - band.start)).coerceAtLeast(MIN_BAND_PX),
                                    size.height,
                                ),
                            cornerRadius = radius,
                        )
                    }
                    drawRect(
                        color = marker,
                        topLeft = Offset(size.width * nowFraction.coerceIn(0f, 1f) - MARKER_HALF_PX, 0f),
                        size = Size(MARKER_HALF_PX * 2, size.height),
                    )
                }
            }
        }
    }
}

private fun tapModifier(
    lane: TimelineLane,
    index: Int,
    onSelect: ((Int, Float) -> Unit)?,
): Modifier =
    if (onSelect == null) {
        Modifier
    } else {
        Modifier.pointerInput(lane) {
            detectTapGestures { offset -> onSelect(index, (offset.x / size.width).coerceIn(0f, 1f)) }
        }
    }

/** Deeper coverage, stronger band. Two overlapping windows are not the same fact as one. */
private fun shadeFor(
    base: Color,
    depth: Int,
    toward: Color,
): Color = if (depth <= 1) base else lerp(base, toward, OVERLAP_SHIFT)

private val LANE_GAP = 5.dp

/** Below this a band would round away to nothing on a low-density screen. */
private const val MIN_BAND_PX = 2f

/** Half the present-moment marker's width, in pixels. */
private const val MARKER_HALF_PX = 1.5f

/**
 * How far from the track a doubly-covered stretch is pushed. Enough to read as a different shade,
 * not so far that it stops looking like the same colour. Measured at this value, a depth-2 band sits
 * at 8.9:1 on the light scheme and 6.8:1 on the dark, against 5.1:1 and 5.5:1 for depth 1.
 */
private const val OVERLAP_SHIFT = 0.38f

/** Above this a surface counts as light, and "deeper" means darker. */
private const val MID_LUMINANCE = 0.5f

/**
 * A real Friday, at the minute the two lanes were built for: Rahu Kalam and Abhijit Muhurta running
 * together, and a pair of cautions overlapping later in the afternoon.
 *
 * Three previews rather than one, per the house rule: the accents have to survive the dark scheme,
 * and the lane labels have to survive a doubled font scale.
 */
@Preview(name = "Light", uiMode = UI_MODE_NIGHT_NO, showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Preview(name = "Large text", fontScale = 2f, showBackground = true, widthDp = 360)
@Composable
private fun VedicTimelineBarPreview() {
    VedicMitraTheme {
        val accents = LocalVedicAccents.current
        Column(modifier = Modifier.padding(12.dp)) {
            VedicTimelineBar(
                lanes =
                    listOf(
                        TimelineLane(
                            label = "Good",
                            bands = listOf(TimelineBand(0.03f, 0.09f, 1), TimelineBand(0.54f, 0.60f, 1)),
                            colour = accents.auspicious,
                        ),
                        TimelineLane(
                            label = "Avoid",
                            bands =
                                listOf(
                                    TimelineBand(0.23f, 0.31f, 1),
                                    TimelineBand(0.31f, 0.35f, 2),
                                    TimelineBand(0.35f, 0.40f, 1),
                                    TimelineBand(0.45f, 0.57f, 1),
                                    TimelineBand(0.74f, 0.79f, 1),
                                    TimelineBand(0.79f, 0.87f, 2),
                                    TimelineBand(0.87f, 0.92f, 1),
                                ),
                            colour = accents.caution,
                        ),
                    ),
                nowFraction = 0.56f,
                spokenDescription = "Rahu Kalam until 12:18, Abhijit Muhurta until 12:42 running now.",
            )
        }
    }
}
