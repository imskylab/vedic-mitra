/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock.domain

import kotlin.math.hypot

/**
 * The maths behind the clock face, deliberately free of Compose and Android.
 *
 * Radial drawing is where the bugs live, and none of them are visible in a screenshot until someone
 * looks closely — an arc a degree out, a ring that swallows its neighbour's taps, a hand that sweeps
 * the wrong way at midnight. Keeping this pure means it can be tested properly, which is the only
 * safety net available: the repo has no UI test infrastructure at all.
 *
 * Angles are **degrees, clockwise, with zero at three o'clock** — the convention Compose's `drawArc`
 * uses, so callers can pass these values straight through without a second conversion to get wrong.
 */
object RingGeometry {
    /** Twelve o'clock, in `drawArc`'s frame. Every cycle on this face starts at the top. */
    const val TOP_DEGREES = -90f

    private const val FULL_CIRCLE = 360f

    /** How wide one division of a [segmentCount]-division ring is, in degrees. */
    fun segmentSweep(segmentCount: Int): Float {
        require(segmentCount > 0) { "segmentCount must be positive, got $segmentCount" }
        return FULL_CIRCLE / segmentCount
    }

    /** Where division [index] begins, in degrees, measured from twelve o'clock clockwise. */
    fun segmentStart(
        index: Int,
        segmentCount: Int,
    ): Float = TOP_DEGREES + index * segmentSweep(segmentCount)

    /**
     * How much of the active division to fill for [fraction] progress through it.
     *
     * A null [fraction] — vara without a known sunrise — fills the whole division instead of none,
     * so the ring still reads as "this is the current one" rather than looking empty.
     */
    fun activeSweep(
        fraction: Double?,
        segmentCount: Int,
    ): Float {
        val sweep = segmentSweep(segmentCount)
        val f = fraction ?: return sweep
        return (f.coerceIn(0.0, 1.0) * sweep).toFloat()
    }

    /**
     * The radius band each ring occupies, outermost first, as fractions of the outer radius.
     *
     * Rings are laid out from the rim inwards so that adding one never moves the others' *outer*
     * edges, and the hub keeps a fixed share of the middle for its text.
     */
    fun ringBands(
        ringCount: Int,
        hubFraction: Float = DEFAULT_HUB_FRACTION,
    ): List<ClosedFloatingPointRange<Float>> {
        require(ringCount > 0) { "ringCount must be positive, got $ringCount" }
        require(hubFraction in 0f..1f) { "hubFraction must be a fraction, got $hubFraction" }
        val band = (1f - hubFraction) / ringCount
        return (0 until ringCount).map { index ->
            val outer = 1f - index * band
            (outer - band)..outer
        }
    }

    /**
     * Which ring a touch at ([dx], [dy]) from the centre falls on, or `null` for the hub or beyond
     * the rim.
     *
     * **Deliberately resolves to a ring, not a segment.** The karana ring divides the circle into
     * sixty, six degrees apiece — far below any usable touch target, and every division except the
     * active one is identical context anyway. Asking "which cycle did they tap" is the question with
     * a meaningful answer.
     */
    fun ringAt(
        dx: Float,
        dy: Float,
        outerRadius: Float,
        ringCount: Int,
        hubFraction: Float = DEFAULT_HUB_FRACTION,
    ): Int? {
        if (outerRadius <= 0f) return null
        val distance = hypot(dx, dy) / outerRadius
        if (distance > 1f || distance < hubFraction) return null
        return ringBands(ringCount, hubFraction)
            .indexOfFirst { distance >= it.start && distance <= it.endInclusive }
            .takeIf { it >= 0 }
    }

    /**
     * The next value for an animated fraction, unwrapped so it never runs backwards.
     *
     * When a limb rolls over, its fraction drops from near 1 to near 0. Animating straight to the
     * new value sweeps the arc *backwards* around the whole ring — a full reverse revolution, once
     * per limb boundary. Returning `next + 1` across that seam keeps the motion forwards; the caller
     * resets the accumulator when the active index changes.
     */
    fun unwrap(
        previous: Float,
        next: Float,
    ): Float = if (next - previous < -WRAP_THRESHOLD) next + 1f else next

    /** How far a fraction must fall in one step to be read as a wrap rather than a correction. */
    private const val WRAP_THRESHOLD = 0.5f

    /** The share of the radius kept for the hub's text. */
    const val DEFAULT_HUB_FRACTION = 0.42f
}
