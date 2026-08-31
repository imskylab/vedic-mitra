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
 * Naming the neighbours is arithmetic, and the arithmetic is only interesting at the seams: the
 * value before the first and after the last. Those cases occur once a cycle, so a wrap bug would
 * appear for a few hours a month and look like a data problem rather than a code one.
 */
class LimbCycleTest {
    @Test
    fun `the cycle wraps at both ends`() {
        // Amavasya is the thirtieth tithi; the next is the first of the waxing fortnight.
        assertThat(PanchangaLimb.TITHI.nextAfter(30)).isEqualTo(PanchangaLimb.TITHI.nameAt(1))
        assertThat(PanchangaLimb.TITHI.previousTo(1)).isEqualTo(PanchangaLimb.TITHI.nameAt(30))
        assertThat(PanchangaLimb.KARANA.nextAfter(60)).isEqualTo(PanchangaLimb.KARANA.nameAt(1))
        assertThat(PanchangaLimb.VARA.previousTo(1)).isEqualTo("Shanivara")
        assertThat(PanchangaLimb.VARA.nextAfter(7)).isEqualTo("Ravivara")
    }

    @Test
    fun `every position in every cycle has a name`() {
        // Guards the tables and the karana rule together: an off-by-one in any of them throws here
        // rather than on someone's screen.
        PanchangaLimb.entries.forEach { limb ->
            (1..limb.cycleLength).forEach { position ->
                assertWithMessage("${limb.displayName} position $position")
                    .that(limb.nameAt(position))
                    .isNotEmpty()
            }
        }
    }

    @Test
    fun `the fifteenth of each fortnight is the full or new moon`() {
        // The one place tithi naming is not a plain table lookup.
        assertThat(PanchangaLimb.TITHI.nameAt(15)).isEqualTo("Purnima")
        assertThat(PanchangaLimb.TITHI.nameAt(30)).isEqualTo("Amavasya")
        assertWithMessage("the fourteenth is an ordinary name in both fortnights")
            .that(PanchangaLimb.TITHI.nameAt(14))
            .isEqualTo(PanchangaLimb.TITHI.nameAt(29))
    }

    @Test
    fun `karana names follow the one-then-seven-then-three rule`() {
        // 60 positions from 11 names: Kimstughna once, seven movable repeating, three fixed at the
        // end. Treating it as a plain modulo would misname the last three days of every month.
        assertThat(PanchangaLimb.KARANA.nameAt(1)).isEqualTo("Kimstughna")
        assertThat(PanchangaLimb.KARANA.nameAt(58)).isEqualTo("Shakuni")
        assertThat(PanchangaLimb.KARANA.nameAt(59)).isEqualTo("Chatushpada")
        assertThat(PanchangaLimb.KARANA.nameAt(60)).isEqualTo("Naga")
        assertWithMessage("the movable seven repeat between")
            .that(PanchangaLimb.KARANA.nameAt(2))
            .isEqualTo(PanchangaLimb.KARANA.nameAt(9))
    }

    @Test
    fun `naming matches what the ephemeris produces`() {
        // The whole premise: a neighbour can be named without consulting the sky. If these two ever
        // disagree, the wheels would show a different tithi from the row above them.
        listOf(0.0, 45.0, 180.0, 300.0, 359.9).forEach { elongation ->
            val fromSky = tithiOf(elongation)
            assertWithMessage("elongation $elongation")
                .that(PanchangaLimb.TITHI.nameAt(fromSky.number))
                .isEqualTo(fromSky.name)

            val karana = karanaOf(elongation)
            assertWithMessage("karana at $elongation")
                .that(PanchangaLimb.KARANA.nameAt(karana.number))
                .isEqualTo(karana.name)
        }
        listOf(10.0, 120.0, 250.0, 359.0).forEach { sum ->
            val yoga = yogaOf(sum)
            assertWithMessage("yoga sum $sum")
                .that(PanchangaLimb.YOGA.nameAt(yoga.number))
                .isEqualTo(yoga.name)
        }
    }

    @Test
    fun `a position outside the cycle wraps rather than throwing`() {
        // Callers do arithmetic on these; going a step past either end must stay in the loop.
        assertThat(PanchangaLimb.NAKSHATRA.nameAt(28)).isEqualTo(PanchangaLimb.NAKSHATRA.nameAt(1))
        assertThat(PanchangaLimb.NAKSHATRA.nameAt(0)).isEqualTo(PanchangaLimb.NAKSHATRA.nameAt(27))
        assertThat(PanchangaLimb.NAKSHATRA.nameAt(-1)).isEqualTo(PanchangaLimb.NAKSHATRA.nameAt(26))
    }

    @Test
    fun `cycle lengths are the real ones`() {
        assertThat(PanchangaLimb.VARA.cycleLength).isEqualTo(7)
        assertThat(PanchangaLimb.TITHI.cycleLength).isEqualTo(30)
        assertThat(PanchangaLimb.NAKSHATRA.cycleLength).isEqualTo(27)
        assertThat(PanchangaLimb.YOGA.cycleLength).isEqualTo(27)
        assertWithMessage("60 positions in a lunar month, not the 11 names")
            .that(PanchangaLimb.KARANA.cycleLength)
            .isEqualTo(60)
        assertThat(PanchangaLimb.MOON_PHASE.cycleLength).isEqualTo(MoonPhase.entries.size)
        assertThat(PanchangaLimb.MOON_RASHI.cycleLength).isEqualTo(RASHI_NAMES.size)
    }
}
