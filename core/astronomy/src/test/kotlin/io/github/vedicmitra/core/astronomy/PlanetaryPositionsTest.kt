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
import kotlin.math.abs

// A fixed reference instant (~August 2026) — the invariants below hold for any date.
private const val REFERENCE = 1_786_000_000_000L
private const val MINUTE_MILLIS = 60_000L

class PlanetaryPositionsTest {
    private val t = Ephemeris.julianCenturies(REFERENCE)

    @Test
    fun `the Sun's sidereal longitude matches the app's of-date ephemeris`() {
        val expected = Ephemeris.norm360(Ephemeris.sunApparentLongitude(t) - Ephemeris.lahiriAyanamsa(t))
        assertThat(siderealLongitude(Graha.SUN, t)).isWithin(1e-6).of(expected)
    }

    @Test
    fun `Shukra (Venus) never strays beyond its maximum elongation from the Sun`() {
        // Venus's greatest elongation is ~47°; a larger separation would mean the geocentric
        // computation (Kepler solve, Earth subtraction, rotation) is wrong.
        val separation = angularSeparation(siderealLongitude(Graha.SUN, t), siderealLongitude(Graha.SHUKRA, t))
        assertThat(separation).isLessThan(48.0)
    }

    @Test
    fun `every graha resolves to a valid rashi and a future pravesh`() {
        val positions = planetaryPositions(REFERENCE).positions

        assertThat(positions.map { it.graha })
            .containsExactly(Graha.SUN, Graha.MOON, Graha.GURU, Graha.SHUKRA)
            .inOrder()
        positions.forEach { position ->
            assertThat(position.rasi.index).isAtLeast(0)
            assertThat(position.rasi.index).isAtMost(11)
            assertThat(position.rasi.name).isEqualTo(RASHI_NAMES[position.rasi.index])
            position.pravesh?.let { assertThat(it.toEpochMilliseconds()).isGreaterThan(REFERENCE) }
        }
    }

    @Test
    fun `the Moon's rashi actually changes at its pravesh`() {
        val moon = planetaryPositions(REFERENCE).positions.first { it.graha == Graha.MOON }
        val pravesh = requireNotNull(moon.pravesh).toEpochMilliseconds()

        val before = rashiIndex(Graha.MOON, pravesh - MINUTE_MILLIS)
        val after = rashiIndex(Graha.MOON, pravesh + MINUTE_MILLIS)
        assertThat(before).isEqualTo(moon.rasi.index)
        assertThat(after).isNotEqualTo(before)
    }

    private fun rashiIndex(
        graha: Graha,
        epochMillis: Long,
    ): Int = (siderealLongitude(graha, Ephemeris.julianCenturies(epochMillis)) / 30.0).toInt()

    private fun angularSeparation(
        a: Double,
        b: Double,
    ): Double {
        val diff = abs(a - b) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }
}
