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
import kotlin.math.min

/**
 * Validates the low-precision ephemeris against known astronomical events: the vernal equinox
 * (Sun's apparent longitude = 0°), a known new/full moon (Sun–Moon elongation = 0°/180°), and the
 * Lahiri ayanamsa for the current era. Epoch-millis constants are UTC.
 */
class EphemerisTest {
    private fun elongationAt(epochMillis: Long): Double {
        val t = Ephemeris.julianCenturies(epochMillis)
        return Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.sunApparentLongitude(t))
    }

    @Test
    fun `sun apparent longitude is zero at the 2024 vernal equinox`() {
        // 2024-03-20 03:06 UTC
        val t = Ephemeris.julianCenturies(1_710_903_960_000L)
        val longitude = Ephemeris.sunApparentLongitude(t)
        val distanceFromZero = min(longitude, 360.0 - longitude)
        assertThat(distanceFromZero).isLessThan(0.05)
    }

    @Test
    fun `elongation is zero at the 2024-01-11 new moon`() {
        val elongation = elongationAt(1_704_974_220_000L)
        val distanceFromNewMoon = min(elongation, 360.0 - elongation)
        assertThat(distanceFromNewMoon).isLessThan(0.3)
    }

    @Test
    fun `elongation is 180 degrees at the 2024-01-25 full moon`() {
        assertThat(elongationAt(1_706_205_240_000L)).isWithin(0.3).of(180.0)
    }

    @Test
    fun `lahiri ayanamsa is about 24 degrees in 2024`() {
        val t = Ephemeris.julianCenturies(1_704_067_200_000L) // 2024-01-01
        assertThat(Ephemeris.lahiriAyanamsa(t)).isWithin(0.05).of(24.19)
    }

    @Test
    fun `moon latitude stays within its physical bound`() {
        // The Moon's orbital inclination to the ecliptic (~5.145 degrees) bounds its ecliptic
        // latitude to a little over 5 degrees, regardless of date.
        for (epochMillis in listOf(0L, 1_705_320_000_000L, 1_785_911_400_000L, 1_600_000_000_000L)) {
            val t = Ephemeris.julianCenturies(epochMillis)
            assertThat(Ephemeris.moonLatitude(t)).isIn(-5.5..5.5)
        }
    }

    @Test
    fun `moon distance stays within its physical bound`() {
        // The Moon's distance from Earth ranges from perigee (~356,500 km) to apogee (~406,700 km).
        for (epochMillis in listOf(0L, 1_705_320_000_000L, 1_785_911_400_000L, 1_600_000_000_000L)) {
            val t = Ephemeris.julianCenturies(epochMillis)
            assertThat(Ephemeris.moonDistanceKm(t)).isIn(356_000.0..407_000.0)
        }
    }

    @Test
    fun `greenwich mean sidereal time is the known constant at J2000_0`() {
        // JD 2451545.0 = 2000-01-01 12:00 UTC (by this Ephemeris's own Julian Day convention);
        // Meeus's GMST formula reduces to exactly its constant term there.
        val j2000EpochMillis = ((2_451_545.0 - 2_440_587.5) * 86_400_000.0).toLong()
        assertThat(Ephemeris.greenwichMeanSiderealTimeDeg(j2000EpochMillis)).isWithin(1e-6).of(280.46061837)
    }

    @Test
    fun `equatorial conversion round-trips a known ecliptic position`() {
        // The vernal equinox point (ecliptic longitude/latitude 0,0) lies on the celestial equator,
        // so it converts to right ascension 0 and declination 0 regardless of obliquity.
        val (rightAscension, declination) =
            Ephemeris.equatorialFromEcliptic(
                eclipticLongitudeDeg = 0.0,
                eclipticLatitudeDeg = 0.0,
                obliquityDeg = 23.44,
            )

        assertThat(rightAscension).isWithin(1e-9).of(0.0)
        assertThat(declination).isWithin(1e-9).of(0.0)
    }
}
