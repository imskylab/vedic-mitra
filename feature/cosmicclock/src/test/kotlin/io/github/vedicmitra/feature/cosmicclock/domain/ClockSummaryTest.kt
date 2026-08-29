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
import io.github.vedicmitra.core.astronomy.LimbWindow
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The hub's sentence is the one thing a reader is guaranteed to take away, so the values behind it
 * are worth pinning — particularly the paksha, which is derived rather than carried and would be
 * silently wrong for half of every month if the split were off by one.
 */
class ClockSummaryTest {
    @Test
    fun `the fortnight is named, because a tithi name alone is ambiguous`() {
        // "Chaturdashi" happens twice a lunar month. Without the paksha a reader checking against an
        // almanac cannot tell which one the clock means.
        assertThat(model(tithiIndex = 13).summaryAt(AT)?.tithi).isEqualTo("Shukla Chaturdashi")
        assertThat(model(tithiIndex = 28).summaryAt(AT)?.tithi).isEqualTo("Krishna Chaturdashi")
    }

    @Test
    fun `the fortnights split exactly halfway`() {
        // The engine numbers tithis 1..30 with 1..15 waxing, so the 0-based index splits at 15. An
        // off-by-one here mislabels either Purnima or Pratipada, and only for part of the month.
        assertWithMessage("index 14 is Purnima, the last of the waxing fortnight")
            .that(pakshaOf(14))
            .isEqualTo("Shukla")
        assertWithMessage("index 15 is the first of the waning fortnight")
            .that(pakshaOf(15))
            .isEqualTo("Krishna")
        assertThat(pakshaOf(0)).isEqualTo("Shukla")
        assertThat(pakshaOf(29)).isEqualTo("Krishna")
    }

    @Test
    fun `the summary carries the tithi's ending and how far off it is`() {
        val summary = checkNotNull(model().summaryAt(AT))
        assertThat(summary.tithiEndsAt).isEqualTo(END)
        assertThat(summary.tithiRemaining).isEqualTo(24.hours)
    }

    @Test
    fun `remaining time is measured from the moment asked about, not the model's own instant`() {
        // The hub re-reads this every minute while the model stays put; if it used the model's
        // instant the countdown would freeze at whatever it was when the clock last loaded.
        val sixHoursLater = Instant.fromEpochMilliseconds(AT.toEpochMilliseconds() + 6 * 3_600_000L)
        assertThat(model().summaryAt(sixHoursLater)?.tithiRemaining).isEqualTo(18.hours)
    }

    @Test
    fun `pada is reported one-based, as a reader would say it`() {
        // Stored 0-based for drawing; "pada 0" would be meaningless on screen.
        assertThat(model(padaIndex = 0).summaryAt(AT)?.pada).isEqualTo(1)
        assertThat(model(padaIndex = 3).summaryAt(AT)?.pada).isEqualTo(4)
        assertThat(model(padaIndex = null).summaryAt(AT)?.pada).isNull()
    }

    @Test
    fun `a tithi with no known boundary still reads`() {
        val summary = checkNotNull(model(tithiWindow = null).summaryAt(AT))
        assertThat(summary.tithi).isEqualTo("Shukla Chaturdashi")
        assertThat(summary.tithiEndsAt).isNull()
        assertThat(summary.tithiRemaining).isNull()
    }

    @Test
    fun `a clock without the rings it needs has no summary`() {
        val withoutTithi =
            PanchangaClockModel(at = AT, rings = listOf(ring(PanchangaConcept.VARA, 7, 5, "Shukravara")), pada = null)
        assertThat(withoutTithi.summaryAt(AT)).isNull()
    }

    private fun model(
        tithiIndex: Int = 13,
        padaIndex: Int? = 2,
        tithiWindow: LimbWindow? = window(),
    ) = PanchangaClockModel(
        at = AT,
        rings =
            listOf(
                ring(PanchangaConcept.TITHI, 30, tithiIndex, "Chaturdashi", tithiWindow),
                ring(PanchangaConcept.NAKSHATRA, 27, 3, "Rohini", window()),
            ),
        pada = padaIndex?.let { PadaMarker(index = it, window = window()) },
    )

    private fun ring(
        concept: PanchangaConcept,
        segments: Int,
        active: Int,
        name: String,
        window: LimbWindow? = window(),
    ) = ClockRing(
        concept = concept,
        label = name,
        segmentCount = segments,
        activeIndex = active,
        activeName = name,
        window = window,
    )

    private companion object {
        val AT = Instant.fromEpochMilliseconds(1_787_000_000_000L)
        val END = Instant.fromEpochMilliseconds(1_787_000_000_000L + 86_400_000L)

        fun window() = LimbWindow(start = AT, end = END, angularFraction = 0.25)
    }
}
