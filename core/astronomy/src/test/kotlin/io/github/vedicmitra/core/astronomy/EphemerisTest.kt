/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
}
