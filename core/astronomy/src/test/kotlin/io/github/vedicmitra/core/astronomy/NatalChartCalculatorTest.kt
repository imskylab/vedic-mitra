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

private const val BIRTH = 1_786_000_000_000L
private const val DELHI_LAT = 28.6139
private const val DELHI_LNG = 77.2090

@Suppress("MagicNumber")
class NatalChartCalculatorTest {
    private val chart = natalChart(BIRTH, DELHI_LAT, DELHI_LNG)

    @Test
    fun `the chart carries all nine grahas`() {
        assertThat(chart.grahas.map { it.graha }).containsExactlyElementsIn(Graha.entries)
    }

    @Test
    fun `the twelve houses are the rashis starting from the lagna`() {
        assertThat(chart.houses).hasSize(12)
        assertThat(chart.houses.first().index).isEqualTo(chart.lagna.rasi.index)
        assertThat(chart.houses.map { it.index }).containsNoDuplicates()
    }

    @Test
    fun `each graha's house is its rashi counted from the lagna`() {
        val ascendant = chart.lagna.rasi.index
        chart.grahas.forEach { graha ->
            val expected = ((graha.rasi.index - ascendant + 12) % 12) + 1
            assertThat(graha.house).isEqualTo(expected)
            assertThat(graha.house).isAtLeast(1)
            assertThat(graha.house).isAtMost(12)
        }
    }

    @Test
    fun `the Sun and Moon are never retrograde and the nodes always are`() {
        val byGraha = chart.grahas.associateBy { it.graha }
        assertThat(byGraha.getValue(Graha.SUN).retrograde).isFalse()
        assertThat(byGraha.getValue(Graha.MOON).retrograde).isFalse()
        assertThat(byGraha.getValue(Graha.RAHU).retrograde).isTrue()
        assertThat(byGraha.getValue(Graha.KETU).retrograde).isTrue()
    }

    @Test
    fun `the Moon's nakshatra matches the Moon's longitude and the pada is valid`() {
        val moonLongitude = chart.grahas.first { it.graha == Graha.MOON }.siderealLongitude
        assertThat(chart.moonNakshatra).isEqualTo(nakshatraOf(moonLongitude))
        assertThat(chart.moonPada).isAtLeast(1)
        assertThat(chart.moonPada).isAtMost(4)
    }

    @Test
    fun `the Vimshottari timeline has nine periods with the birth inside the first`() {
        assertThat(chart.vimshottari).hasSize(9)
        assertThat(
            chart.vimshottari
                .first()
                .start
                .toEpochMilliseconds(),
        ).isAtMost(BIRTH)
        assertThat(
            chart.vimshottari
                .first()
                .end
                .toEpochMilliseconds(),
        ).isGreaterThan(BIRTH)
    }
}
