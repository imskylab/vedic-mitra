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

/**
 * Boundary behaviour of the angular divisions.
 *
 * Every division is half-open — `[start, end)` — so a longitude sitting exactly on a boundary
 * belongs to the division that is beginning. These cases are the ones that used to be decided by
 * whichever way two separate `Double` roundings happened to fall.
 */
class AngularBucketsTest {
    private val nakshatraSpan = 360.0 / 27.0
    private val padaSpan = nakshatraSpan / 4

    @Test
    fun `26 degrees 40 minutes is Krittika, not Bharani`() {
        // The exact Bharani/Krittika boundary. Half-open means it belongs to Krittika.
        val boundary = 26.0 + 40.0 / 60.0
        assertThat(AngularBuckets.nakshatraIndex(boundary)).isEqualTo(2)
    }

    @Test
    fun `a hair below a boundary stays in the division that is ending`() {
        val boundary = 26.0 + 40.0 / 60.0
        assertThat(AngularBuckets.nakshatraIndex(boundary - 1e-9)).isEqualTo(1)
    }

    @Test
    fun `every nakshatra boundary lands in the division it opens`() {
        repeat(AngularBuckets.NAKSHATRA_COUNT) { k ->
            assertThat(AngularBuckets.nakshatraIndex(k * nakshatraSpan)).isEqualTo(k)
        }
    }

    @Test
    fun `every pada boundary lands in the quarter it opens`() {
        // The regression this guards: taking `longitude % nakshatraSpan` and dividing again rounds
        // twice, and put 40 of these 108 boundaries in the wrong quarter.
        repeat(AngularBuckets.NAKSHATRA_COUNT) { k ->
            repeat(AngularBuckets.PADAS_PER_NAKSHATRA) { p ->
                val longitude = k * nakshatraSpan + p * padaSpan
                assertThat(AngularBuckets.pada(longitude)).isEqualTo(p + 1)
            }
        }
    }

    @Test
    fun `every tithi, karana and rashi boundary lands in the division it opens`() {
        repeat(30) { k -> assertThat(AngularBuckets.tithiIndex(k * 12.0)).isEqualTo(k) }
        repeat(60) { k -> assertThat(AngularBuckets.karanaIndex(k * 6.0)).isEqualTo(k) }
        repeat(12) { k -> assertThat(AngularBuckets.rashiIndex(k * 30.0)).isEqualTo(k) }
    }

    @Test
    fun `longitudes are normalised before bucketing`() {
        assertThat(AngularBuckets.rashiIndex(-30.0)).isEqualTo(11)
        assertThat(AngularBuckets.rashiIndex(360.0)).isEqualTo(0)
        assertThat(AngularBuckets.rashiIndex(390.0)).isEqualTo(1)
        assertThat(AngularBuckets.nakshatraIndex(-1e-16)).isEqualTo(0)
    }

    @Test
    fun `no index ever escapes its range`() {
        // Sweep finely enough to catch an off-by-one at any division edge.
        var degrees = -720.0
        while (degrees < 720.0) {
            assertThat(AngularBuckets.nakshatraIndex(degrees)).isIn(0..26)
            assertThat(AngularBuckets.rashiIndex(degrees)).isIn(0..11)
            assertThat(AngularBuckets.tithiIndex(degrees)).isIn(0..29)
            assertThat(AngularBuckets.karanaIndex(degrees)).isIn(0..59)
            assertThat(AngularBuckets.pada(degrees)).isIn(1..4)
            degrees += 0.37
        }
    }

    @Test
    fun `the fraction through a division runs from zero up to but not including one`() {
        assertThat(AngularBuckets.fractionThrough(0.0, AngularBuckets.NAKSHATRA_ARCSEC)).isEqualTo(0.0)
        assertThat(AngularBuckets.fractionThrough(nakshatraSpan / 2, AngularBuckets.NAKSHATRA_ARCSEC))
            .isWithin(1e-9)
            .of(0.5)
        val justBefore = AngularBuckets.fractionThrough(nakshatraSpan - 1e-6, AngularBuckets.NAKSHATRA_ARCSEC)
        assertThat(justBefore).isLessThan(1.0)
        assertThat(justBefore).isGreaterThan(0.99)
    }

    @Test
    fun `bucketing agrees with the panchanga limbs it backs`() {
        // nakshatraOf/tithiOf/karanaOf are 1-based over the same divisions.
        val moon = 200.125
        assertThat(nakshatraOf(moon).number).isEqualTo(AngularBuckets.nakshatraIndex(moon) + 1)
        val elongation = 143.75
        assertThat(tithiOf(elongation).number).isEqualTo(AngularBuckets.tithiIndex(elongation) + 1)
    }
}
