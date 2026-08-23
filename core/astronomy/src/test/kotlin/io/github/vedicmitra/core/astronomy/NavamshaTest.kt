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
 * The navamsha division and the position-within-rashi columns of the Spashta Graha table.
 *
 * The navamsha implementation counts navamshas continuously from 0° Aries and takes the result
 * modulo twelve. The tests below check that against the classical three-case rule it replaces —
 * movable signs start from themselves, fixed signs from the ninth, dual signs from the fifth —
 * because the collapse into one expression is the part worth pinning down.
 */
class NavamshaTest {
    private val navamshaSpan = 360.0 / 108.0

    @Test
    fun `a movable sign's first navamsha is the sign itself`() {
        // Mesha, Karka, Tula, Makara — indices 0, 3, 6, 9.
        listOf(0, 3, 6, 9).forEach { sign ->
            assertWithMessage("movable sign $sign")
                .that(navamshaOf(sign * 30.0).index)
                .isEqualTo(sign)
        }
    }

    @Test
    fun `a fixed sign's first navamsha is the ninth from it`() {
        // Vrishabha, Simha, Vrishchika, Kumbha — indices 1, 4, 7, 10.
        listOf(1, 4, 7, 10).forEach { sign ->
            assertWithMessage("fixed sign $sign")
                .that(navamshaOf(sign * 30.0).index)
                .isEqualTo((sign + 8) % 12)
        }
    }

    @Test
    fun `a dual sign's first navamsha is the fifth from it`() {
        // Mithuna, Kanya, Dhanu, Meena — indices 2, 5, 8, 11.
        listOf(2, 5, 8, 11).forEach { sign ->
            assertWithMessage("dual sign $sign")
                .that(navamshaOf(sign * 30.0).index)
                .isEqualTo((sign + 4) % 12)
        }
    }

    @Test
    fun `the navamshas of a sign run through nine consecutive signs`() {
        repeat(12) { sign ->
            val first = navamshaOf(sign * 30.0).index
            repeat(9) { part ->
                val longitude = sign * 30.0 + part * navamshaSpan
                assertWithMessage("sign $sign navamsha ${part + 1}")
                    .that(navamshaOf(longitude).index)
                    .isEqualTo((first + part) % 12)
            }
        }
    }

    @Test
    fun `every one of the 108 navamsha boundaries opens its own division`() {
        repeat(108) { ordinal ->
            assertWithMessage("navamsha boundary $ordinal")
                .that(navamshaOf(ordinal * navamshaSpan).index)
                .isEqualTo(ordinal % 12)
        }
    }

    @Test
    fun `the whole zodiac maps to a valid sign`() {
        var degrees = 0.0
        while (degrees < 360.0) {
            assertThat(navamshaOf(degrees).index).isIn(0..11)
            degrees += 0.37
        }
    }

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
            // Truncated to the arcminute, so the rebuilt value trails by less than one minute.
            assertWithMessage("longitude $degrees").that(degrees - rebuilt).isAtLeast(0.0)
            assertWithMessage("longitude $degrees").that(degrees - rebuilt).isLessThan(1.0 / 60.0)
            degrees += 0.37
        }
    }
}
