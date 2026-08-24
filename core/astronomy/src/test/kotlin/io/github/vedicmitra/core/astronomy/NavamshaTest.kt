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

package io.github.vedicmitra.core.astronomy

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The position-within-rashi columns of the Spashta Graha table.
 *
 * The navamsha rule itself moved to [VargaTest] when it was generalised to every divisional chart;
 * what remains here is the degree-and-minute arithmetic, which is where the boundary snap bites.
 */
class NavamshaTest {
    /**
     * Comfortably above the boundary snap (1e-8 arcsec, or 2.8e-12 degrees) and far below the
     * arcminute the position is truncated to, so it catches a real regression without failing on
     * the snap itself. The measured overshoot is about 1.8e-13 degrees.
     */
    private val snapTolerance = 1e-9

    @Test
    fun `position in rashi is measured from the start of the sign`() {
        // 26 deg 40 min of Mesha, and the same offset in Vrishchika.
        assertThat(positionInRashi(26.0 + 40.0 / 60.0)).isEqualTo(PositionInRashi(26, 40))
        assertThat(positionInRashi(7 * 30.0 + 26.0 + 40.0 / 60.0)).isEqualTo(PositionInRashi(26, 40))
    }

    @Test
    fun `position in rashi stays inside the sign at both ends`() {
        assertThat(positionInRashi(0.0)).isEqualTo(PositionInRashi(0, 0))
        var degrees = 0.0
        while (degrees < 360.0) {
            val position = positionInRashi(degrees)
            assertThat(position.degrees).isIn(0..29)
            assertThat(position.minutes).isIn(0..59)
            degrees += 0.37
        }
    }

    @Test
    fun `position in rashi reassembles to the longitude it came from`() {
        var degrees = 0.13
        while (degrees < 360.0) {
            val position = positionInRashi(degrees)
            val signStart = AngularBuckets.rashiIndex(degrees) * 30.0
            val rebuilt = signStart + position.degrees + position.minutes / 60.0
            // Truncated to the arcminute, so the rebuilt value trails by less than one minute — but
            // it can also sit a sliver *above* the original. AngularBuckets nudges by 1e-8 arcsec
            // before flooring so that a boundary expressed in degrees reads as the boundary, and a
            // longitude a hair below an arcminute mark is carried over it by that same nudge.
            assertWithMessage("longitude $degrees").that(degrees - rebuilt).isGreaterThan(-snapTolerance)
            assertWithMessage("longitude $degrees").that(degrees - rebuilt).isLessThan(1.0 / 60.0)
            degrees += 0.37
        }
    }
}
