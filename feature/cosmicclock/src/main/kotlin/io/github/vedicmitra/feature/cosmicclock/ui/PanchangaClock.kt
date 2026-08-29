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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import io.github.vedicmitra.feature.cosmicclock.domain.ClockRing
import io.github.vedicmitra.feature.cosmicclock.domain.PanchangaClockModel
import io.github.vedicmitra.feature.cosmicclock.domain.RingGeometry
import kotlin.math.min

/**
 * The clock face: five cycles as concentric rings, the current division of each picked out.
 *
 * Drawn in two layers, and the split is not cosmetic. The dim ticks — some 150 of them across the
 * five rings — never change between limb boundaries, so they go in a `drawWithCache` layer keyed on
 * the model and the colours. Only the active arcs read the per-minute progress. Drawing both in one
 * `Canvas` would re-render every tick on the whole face once a minute for no visible difference.
 *
 * @param progress each ring's fill, parallel to `model.rings`. Passed in rather than read from the
 *   model so the caller owns the animation, including the unwrap across a rollover.
 * @param hub what to show in the middle. A slot rather than a parameter so this file stays about
 *   drawing: text in a `Canvas` would mean measuring glyphs by hand, and the hub is ordinary
 *   Compose text that should scale, wrap and be readable by TalkBack like any other.
 */
@Composable
fun PanchangaClock(
    model: PanchangaClockModel,
    progress: List<Float>,
    colors: ClockColors,
    modifier: Modifier = Modifier,
    onSelectRing: (ClockRing) -> Unit = {},
    hub: @Composable (maxWidth: Dp) -> Unit = {},
) {
    BoxWithConstraints(
        modifier =
            modifier
                .aspectRatio(1f)
                .drawWithCache {
                    // Static: rebuilt only when the model or the theme changes.
                    onDrawBehind { drawInactiveRings(model, colors) }
                }.pointerInput(model) {
                    detectTapGestures { offset ->
                        val radius = min(size.width, size.height) / 2f
                        RingGeometry
                            .ringAt(
                                dx = offset.x - size.width / 2f,
                                dy = offset.y - size.height / 2f,
                                outerRadius = radius,
                                ringCount = model.rings.size,
                            )?.let { onSelectRing(model.rings[it]) }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawActiveArcs(model, progress, colors)
        }
        hub(maxWidth * HUB_TEXT_WIDTH_FRACTION)
    }
}

/**
 * Every division of every ring, quietly.
 *
 * This is the layer that makes the face read as *cycles* rather than arcs: seeing that one ring has
 * thirty divisions and another seven is what tells a reader these are different clocks running at
 * different speeds.
 */
private fun DrawScope.drawInactiveRings(
    model: PanchangaClockModel,
    colors: ClockColors,
) {
    val bands = RingGeometry.ringBands(model.rings.size)
    model.rings.forEachIndexed { index, ring ->
        val band = bands[index]
        val stroke = strokeFor(band)
        val sweep = RingGeometry.segmentSweep(ring.segmentCount)
        repeat(ring.segmentCount) { segment ->
            // A hairline gap so neighbouring divisions read as separate rather than as one ring.
            drawRingArc(
                color = colors.inactiveTick,
                startDegrees = RingGeometry.segmentStart(segment, ring.segmentCount) + SEGMENT_GAP_DEGREES,
                sweepDegrees = sweep - SEGMENT_GAP_DEGREES * 2f,
                radiusFraction = band.midpoint(),
                stroke = Stroke(width = stroke.width * INACTIVE_WEIGHT),
            )
        }
    }
}

/** The current division of each ring: filled as far as its progress, tracked for the rest. */
private fun DrawScope.drawActiveArcs(
    model: PanchangaClockModel,
    progress: List<Float>,
    colors: ClockColors,
) {
    val bands = RingGeometry.ringBands(model.rings.size)
    model.rings.forEachIndexed { index, ring ->
        val band = bands[index]
        val stroke = strokeFor(band)
        val start = RingGeometry.segmentStart(ring.activeIndex, ring.segmentCount)
        val full = RingGeometry.segmentSweep(ring.segmentCount)
        val fraction = progress.getOrNull(index)?.toDouble() ?: ring.fraction
        drawRingArc(
            color = colors.activeTrack,
            startDegrees = start,
            sweepDegrees = full,
            radiusFraction = band.midpoint(),
            stroke = stroke,
        )
        drawRingArc(
            color = colors.activeArc,
            startDegrees = start,
            sweepDegrees = RingGeometry.activeSweep(fraction, ring.segmentCount),
            radiusFraction = band.midpoint(),
            stroke = stroke,
        )
    }
}

/** One arc at [radiusFraction] of the half-width, in the geometry's degrees-clockwise-from-3pm frame. */
private fun DrawScope.drawRingArc(
    color: Color,
    startDegrees: Float,
    sweepDegrees: Float,
    radiusFraction: Float,
    stroke: Stroke,
) {
    if (sweepDegrees <= 0f) return
    val outer = min(size.width, size.height) / 2f
    val radius = outer * radiusFraction
    val topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius)
    drawArc(
        color = color,
        startAngle = startDegrees,
        sweepAngle = sweepDegrees,
        useCenter = false,
        topLeft = topLeft,
        size = Size(radius * 2f, radius * 2f),
        style = stroke,
    )
}

/** An arc is stroked along the middle of its band, so the stroke fills the band's width. */
private fun ClosedFloatingPointRange<Float>.midpoint(): Float = (start + endInclusive) / 2f

private fun DrawScope.strokeFor(band: ClosedFloatingPointRange<Float>): Stroke {
    val outer = min(size.width, size.height) / 2f
    return Stroke(width = (band.endInclusive - band.start) * outer * BAND_FILL)
}

/**
 * How much of its band a ring's stroke occupies; the remainder is the gap to its neighbour.
 *
 * Rings that touch read as one thick smear at a glance, which defeats the point of showing five
 * cycles at once.
 */
private const val BAND_FILL = 0.72f

/** Half the gap between neighbouring divisions, in degrees. */
private const val SEGMENT_GAP_DEGREES = 0.35f

/**
 * How thick an inactive division is drawn, against the active arc's full band width.
 *
 * The first version drew every division at full width. Rendering the geometry showed the result: a
 * hundred and fifty chunky blocks that read as a brick wall, with the active arcs lost inside it.
 * Dimming by colour alone did not fix it, because the *weight* was unchanged. Hairline ticks keep
 * what the inactive divisions are for — showing that this cycle has thirty parts and that one has
 * seven — while letting the current division be the only heavy mark on the face.
 */
private const val INACTIVE_WEIGHT = 0.3f
