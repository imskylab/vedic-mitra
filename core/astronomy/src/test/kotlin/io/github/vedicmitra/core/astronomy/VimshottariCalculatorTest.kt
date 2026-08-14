/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val NAKSHATRA_SPAN = 360.0 / 27.0
private const val YEAR_MILLIS = 31_557_600_000.0
private const val BIRTH = 1_786_000_000_000L

@Suppress("MagicNumber")
class VimshottariCalculatorTest {
    @Test
    fun `the Moon at the start of Ashwini begins a full seven-year Ketu mahadasha at birth`() {
        val periods = vimshottariFromMoon(moonLongitude = 0.0, epochMillis = BIRTH)

        val first = periods.first()
        assertThat(first.lord).isEqualTo(Graha.KETU)
        assertThat(first.start.toEpochMilliseconds()).isEqualTo(BIRTH)
        assertThat(first.end.toEpochMilliseconds() - first.start.toEpochMilliseconds())
            .isEqualTo((7 * YEAR_MILLIS).toLong())
    }

    @Test
    fun `the Moon halfway through Ashwini leaves half the Ketu mahadasha remaining`() {
        val periods = vimshottariFromMoon(moonLongitude = NAKSHATRA_SPAN / 2.0, epochMillis = BIRTH)

        assertThat(periods.first().lord).isEqualTo(Graha.KETU)
        // Birth is ~3.5 years into the 7-year Ketu dasha, so it started ~3.5 years before birth.
        assertThat(BIRTH - periods.first().start.toEpochMilliseconds())
            .isWithin(1_000L)
            .of((3.5 * YEAR_MILLIS).toLong())
    }

    @Test
    fun `the Moon in Bharani begins the Shukra mahadasha`() {
        val periods = vimshottariFromMoon(moonLongitude = NAKSHATRA_SPAN + 1.0, epochMillis = BIRTH)

        assertThat(periods.first().lord).isEqualTo(Graha.SHUKRA)
    }

    @Test
    fun `the nine mahadashas are contiguous and span 120 years`() {
        val periods = vimshottariFromMoon(moonLongitude = 100.0, epochMillis = BIRTH)

        assertThat(periods).hasSize(9)
        for (i in 1 until periods.size) {
            assertThat(periods[i].start).isEqualTo(periods[i - 1].end)
        }
        val total = periods.last().end.toEpochMilliseconds() - periods.first().start.toEpochMilliseconds()
        assertThat(total).isEqualTo((120 * YEAR_MILLIS).toLong())
    }

    @Test
    fun `the lords follow the Vimshottari order cyclically from the first`() {
        val periods = vimshottariFromMoon(moonLongitude = 0.0, epochMillis = BIRTH) // starts with Ketu

        assertThat(periods.map { it.lord })
            .containsExactly(
                Graha.KETU,
                Graha.SHUKRA,
                Graha.SUN,
                Graha.MOON,
                Graha.MANGALA,
                Graha.RAHU,
                Graha.GURU,
                Graha.SHANI,
                Graha.BUDHA,
            ).inOrder()
    }

    @Test
    fun `birth falls within the first mahadasha of the real timeline`() {
        val first = vimshottariDasha(BIRTH).first()

        assertThat(first.start.toEpochMilliseconds()).isAtMost(BIRTH)
        assertThat(first.end.toEpochMilliseconds()).isGreaterThan(BIRTH)
    }
}
