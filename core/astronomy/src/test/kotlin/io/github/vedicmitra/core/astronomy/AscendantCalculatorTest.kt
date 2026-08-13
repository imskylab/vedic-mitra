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

// Standard mean obliquity near J2000, and Delhi (a mid-latitude reference observer).
private const val OBLIQUITY = 23.4392
private const val DELHI_LAT = 28.6139
private const val DELHI_LNG = 77.2090
private const val REFERENCE = 1_786_000_000_000L
private const val HALF_HOUR_MILLIS = 1_800_000L

@Suppress("MagicNumber")
class AscendantCalculatorTest {
    @Test
    fun `at the equator with the equinox on the meridian the ascendant is 90 degrees`() {
        assertThat(ascendantTropical(lstDeg = 0.0, latDeg = 0.0, obliquityDeg = OBLIQUITY)).isWithin(0.05).of(90.0)
    }

    @Test
    fun `advancing the sidereal time by 90 degrees at the equator moves the ascendant to 180`() {
        assertThat(ascendantTropical(lstDeg = 90.0, latDeg = 0.0, obliquityDeg = OBLIQUITY)).isWithin(0.05).of(180.0)
    }

    @Test
    fun `matches the known ascendant at 45 degrees north`() {
        // From the standard formula: LST 0°, lat 45°N, ε 23.4392° → 111.70°.
        assertThat(ascendantTropical(lstDeg = 0.0, latDeg = 45.0, obliquityDeg = OBLIQUITY)).isWithin(0.1).of(111.70)
    }

    @Test
    fun `the southern hemisphere mirrors the northern`() {
        assertThat(ascendantTropical(lstDeg = 0.0, latDeg = -45.0, obliquityDeg = OBLIQUITY)).isWithin(0.1).of(68.30)
    }

    @Test
    fun `whole-sign houses start at the ascendant and run in zodiacal order`() {
        val houses = wholeSignHouses(ascendantRasiIndex = 9) // Makara ascendant

        assertThat(houses).hasSize(12)
        assertThat(houses.first().index).isEqualTo(9)
        assertThat(houses[3].index).isEqualTo(0) // 9 → 10 → 11 → 0
        assertThat(houses[6].index).isEqualTo(3) // the 7th house is opposite the ascendant
        assertThat(houses.map { it.index }).containsNoDuplicates()
        houses.forEach { assertThat(it.name).isEqualTo(RASHI_NAMES[it.index]) }
    }

    @Test
    fun `over a day the ascendant rises through all twelve rashis`() {
        val signs =
            (0 until 48)
                .map { lagnaAt(REFERENCE + it * HALF_HOUR_MILLIS, DELHI_LAT, DELHI_LNG).rasi.index }
                .toSet()

        assertThat(signs).hasSize(12)
    }

    @Test
    fun `the lagna resolves to a valid rashi`() {
        val lagna = lagnaAt(REFERENCE, DELHI_LAT, DELHI_LNG)

        assertThat(lagna.rasi.index).isAtLeast(0)
        assertThat(lagna.rasi.index).isAtMost(11)
        assertThat(lagna.rasi.name).isEqualTo(RASHI_NAMES[lagna.rasi.index])
        assertThat(lagna.siderealLongitude).isAtLeast(0.0)
        assertThat(lagna.siderealLongitude).isLessThan(360.0)
    }
}
