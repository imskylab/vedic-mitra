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

class AstangataTest {
    @Test
    fun `a graha inside its orb is combust and outside it is not`() {
        // Budha's direct orb is 14 degrees.
        assertThat(Astangata.isCombust(Graha.BUDHA, 100.0, 90.0, retrograde = false)).isTrue()
        assertThat(Astangata.isCombust(Graha.BUDHA, 105.0, 90.0, retrograde = false)).isFalse()
    }

    @Test
    fun `Budha and Shukra take a tighter orb when retrograde`() {
        // 13 degrees: inside Budha's direct orb of 14, outside its retrograde orb of 12.
        assertThat(Astangata.isCombust(Graha.BUDHA, 103.0, 90.0, retrograde = false)).isTrue()
        assertThat(Astangata.isCombust(Graha.BUDHA, 103.0, 90.0, retrograde = true)).isFalse()
        // 9 degrees: inside Shukra's direct orb of 10, outside its retrograde orb of 8.
        assertThat(Astangata.isCombust(Graha.SHUKRA, 99.0, 90.0, retrograde = false)).isTrue()
        assertThat(Astangata.isCombust(Graha.SHUKRA, 99.0, 90.0, retrograde = true)).isFalse()
    }

    @Test
    fun `the Sun and the nodes are never combust`() {
        listOf(Graha.SUN, Graha.RAHU, Graha.KETU).forEach { graha ->
            assertWithMessage(graha.displayName)
                .that(Astangata.isCombust(graha, 90.0, 90.0, retrograde = true))
                .isFalse()
        }
    }

    @Test
    fun `separation is the shortest way round the circle`() {
        assertThat(Astangata.separation(10.0, 350.0)).isWithin(1e-9).of(20.0)
        assertThat(Astangata.separation(350.0, 10.0)).isWithin(1e-9).of(20.0)
        assertThat(Astangata.separation(0.0, 180.0)).isWithin(1e-9).of(180.0)
        assertThat(Astangata.separation(90.0, 90.0)).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `combustion is symmetric about the Sun`() {
        // Ahead of the Sun and behind it by the same arc must give the same verdict.
        (1..30).forEach { degrees ->
            val ahead = Astangata.isCombust(Graha.SHANI, 90.0 + degrees, 90.0, retrograde = false)
            val behind = Astangata.isCombust(Graha.SHANI, 90.0 - degrees, 90.0, retrograde = false)
            assertWithMessage("$degrees deg from the Sun").that(ahead).isEqualTo(behind)
        }
    }

    @Test
    fun `combustion survives the wrap at zero degrees`() {
        // Shani, orb 15: five degrees apart but either side of 0 Mesha.
        assertThat(Astangata.isCombust(Graha.SHANI, 2.0, 357.0, retrograde = false)).isTrue()
        assertThat(Astangata.isCombust(Graha.SHANI, 20.0, 357.0, retrograde = false)).isFalse()
    }

    @Test
    fun `a natal chart marks the grahas the orbs put inside the Sun's glare`() {
        // Mumbai, 1988-12-31 23:50 IST — the reference set has Shani about 4.7 degrees from
        // the Sun here, comfortably inside its 15 degree orb.
        val chart = natalChart(599_595_600_000L, 19.0760, 72.8777)
        val sun = chart.grahas.first { it.graha == Graha.SUN }
        chart.grahas.filter { it.graha != Graha.SUN }.forEach { graha ->
            val separation = Astangata.separation(graha.siderealLongitude, sun.siderealLongitude)
            if (graha.combust) {
                assertWithMessage("${graha.graha.displayName} marked combust at $separation deg")
                    .that(separation)
                    .isLessThan(18.0)
            }
        }
        assertThat(sun.combust).isFalse()
    }
}
