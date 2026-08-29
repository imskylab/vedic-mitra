/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.feature.cosmicclock.domain

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The clock has no UI test infrastructure behind it — the repo has none at all — so this file is the
 * whole safety net for the drawing maths. Every expected value here was computed independently
 * before the Kotlin was written, rather than read back off the implementation.
 */
class RingGeometryTest {
    @Test
    fun `every ring starts its first division at twelve o'clock`() {
        // If the rings disagreed on where a cycle begins, the face would look plausible and be
        // meaningless — the reader would compare positions across rings that share no origin.
        RING_SIZES.forEach { segments ->
            assertWithMessage("$segments segments")
                .that(RingGeometry.segmentStart(index = 0, segmentCount = segments))
                .isEqualTo(RingGeometry.TOP_DEGREES)
        }
    }

    @Test
    fun `a full ring of divisions closes the circle exactly`() {
        // Catches an off-by-one in either sweep or start: the last division must end where the first
        // began, one revolution on.
        RING_SIZES.forEach { segments ->
            val lastEnd =
                RingGeometry.segmentStart(segments - 1, segments) + RingGeometry.segmentSweep(segments)
            assertWithMessage("$segments segments")
                .that(lastEnd.toDouble())
                .isWithin(TOLERANCE)
                .of(RingGeometry.TOP_DEGREES + 360.0)
        }
    }

    @Test
    fun `division widths match the cycle`() {
        assertThat(RingGeometry.segmentSweep(60).toDouble()).isWithin(TOLERANCE).of(6.0)
        assertThat(RingGeometry.segmentSweep(30).toDouble()).isWithin(TOLERANCE).of(12.0)
        assertThat(RingGeometry.segmentSweep(27).toDouble()).isWithin(TOLERANCE).of(13.3333333)
        assertThat(RingGeometry.segmentSweep(7).toDouble()).isWithin(TOLERANCE).of(51.4285714)
    }

    @Test
    fun `the active division fills in proportion to its progress`() {
        assertThat(RingGeometry.activeSweep(0.5, 30).toDouble()).isWithin(TOLERANCE).of(6.0)
        assertThat(RingGeometry.activeSweep(0.0, 30).toDouble()).isWithin(TOLERANCE).of(0.0)
        assertThat(RingGeometry.activeSweep(1.0, 60).toDouble()).isWithin(TOLERANCE).of(6.0)
    }

    @Test
    fun `an unknown fraction fills the whole division rather than none`() {
        // Vara at a latitude where the Sun does not rise: the weekday is known, its sunrise boundary
        // is not. Drawing nothing would read as "no current vara", which is wrong.
        assertThat(RingGeometry.activeSweep(null, 7).toDouble())
            .isWithin(TOLERANCE)
            .of(RingGeometry.segmentSweep(7).toDouble())
    }

    @Test
    fun `progress outside zero to one is clamped`() {
        assertThat(RingGeometry.activeSweep(1.5, 30).toDouble()).isWithin(TOLERANCE).of(12.0)
        assertThat(RingGeometry.activeSweep(-0.2, 30).toDouble()).isWithin(TOLERANCE).of(0.0)
    }

    @Test
    fun `rings tile the radius from the hub to the rim without gaps`() {
        val bands = RingGeometry.ringBands(ringCount = 5)
        assertThat(bands).hasSize(5)
        assertWithMessage("outermost reaches the rim")
            .that(bands.first().endInclusive.toDouble())
            .isWithin(TOLERANCE)
            .of(1.0)
        assertWithMessage("innermost meets the hub")
            .that(bands.last().start.toDouble())
            .isWithin(TOLERANCE)
            .of(RingGeometry.DEFAULT_HUB_FRACTION.toDouble())
        bands.zipWithNext().forEach { (outer, inner) ->
            assertWithMessage("no gap between rings")
                .that(inner.endInclusive.toDouble())
                .isWithin(TOLERANCE)
                .of(outer.start.toDouble())
        }
    }

    @Test
    fun `a touch resolves to the ring it lands on`() {
        // Radius 100, five rings: bands are 0.884..1.0, 0.768..0.884, 0.652..0.768, 0.536..0.652,
        // 0.420..0.536 — all verified independently.
        assertThat(RingGeometry.ringAt(dx = 98f, dy = 0f, outerRadius = 100f, ringCount = 5)).isEqualTo(0)
        assertThat(RingGeometry.ringAt(dx = 71f, dy = 0f, outerRadius = 100f, ringCount = 5)).isEqualTo(2)
        assertThat(RingGeometry.ringAt(dx = 0f, dy = 47.8f, outerRadius = 100f, ringCount = 5)).isEqualTo(4)
    }

    @Test
    fun `the hub and the space beyond the rim are not rings`() {
        assertWithMessage("inside the hub").that(RingGeometry.ringAt(40f, 0f, 100f, 5)).isNull()
        assertWithMessage("dead centre").that(RingGeometry.ringAt(0f, 0f, 100f, 5)).isNull()
        assertWithMessage("past the rim").that(RingGeometry.ringAt(101f, 0f, 100f, 5)).isNull()
        assertWithMessage("degenerate radius").that(RingGeometry.ringAt(1f, 1f, 0f, 5)).isNull()
    }

    @Test
    fun `a ring is thinner than a comfortable touch target, by geometry`() {
        // Not a defect to fix — it is arithmetic. Five rings between the hub and the rim leave about
        // 0.116 of the radius each, which is roughly 19dp at a 160dp radius; five 48dp bands would
        // need 240dp of radius before the hub gets any. So tapping a ring is a convenience that
        // always lands on *some* ring, and the limb list below the clock is the reliable target and
        // the accessible one. This test exists so that trade-off is recorded rather than discovered.
        val band = RingGeometry.ringBands(5).first()
        val width = band.endInclusive - band.start
        assertThat(width.toDouble()).isWithin(TOLERANCE).of(0.116)
    }

    @Test
    fun `an animated fraction never runs backwards across a rollover`() {
        // A limb boundary drops the fraction from near 1 to near 0. Animating straight there sweeps
        // the arc backwards around the entire ring, once per boundary.
        assertThat(RingGeometry.unwrap(previous = 0.99f, next = 0.01f).toDouble())
            .isWithin(TOLERANCE)
            .of(1.01)
        assertThat(RingGeometry.unwrap(previous = 0.95f, next = 0.05f).toDouble())
            .isWithin(TOLERANCE)
            .of(1.05)
    }

    @Test
    fun `ordinary progress and small corrections are left alone`() {
        assertThat(RingGeometry.unwrap(0.10f, 0.20f).toDouble()).isWithin(TOLERANCE).of(0.20)
        assertWithMessage("a small backwards nudge is a correction, not a wrap")
            .that(RingGeometry.unwrap(0.50f, 0.49f).toDouble())
            .isWithin(TOLERANCE)
            .of(0.49)
    }

    private companion object {
        /** Karana, tithi, nakshatra and yoga, vara — every cycle length the clock draws. */
        val RING_SIZES = listOf(60, 30, 27, 7)
        const val TOLERANCE = 1e-4
    }
}
