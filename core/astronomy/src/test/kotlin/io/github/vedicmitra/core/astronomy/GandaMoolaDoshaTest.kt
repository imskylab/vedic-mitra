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
 * The rule is membership of a six-element set, so the test that matters is the exhaustive one: all
 * 27 nakshatras, each asserted either way. The goldens are the verdicts an independent
 * implementation returned for a Moon swept through a full lunar month.
 */
class GandaMoolaDoshaTest {
    @Test
    fun `exactly the six junctional nakshatras raise the dosha`() {
        // Swept from the reference implementation: 29 charts covering 26 of the 27 nakshatras. The
        // 27th (Shravana) was not sampled and is asserted absent on the same grounds as its
        // neighbours -- it is not adjacent to a rashi seam.
        val expected = setOf(1, 9, 10, 18, 19, 27)
        (1..27).forEach { number ->
            val dosha = gandaMoolaDoshaOf(chartWithMoonIn(number))
            assertWithMessage("nakshatra $number (${NAKSHATRA_NAMES[number - 1]})")
                .that(dosha.present)
                .isEqualTo(number in expected)
        }
    }

    @Test
    fun `the pada does not affect the verdict`() {
        // The one thing worth checking, and the reference data settles it: Jyeshtha fired at pada 2
        // and again at pada 4, and no nakshatra appeared on both sides of the sweep.
        (1..4).forEach { pada ->
            assertWithMessage("Jyeshtha pada $pada")
                .that(gandaMoolaDoshaOf(chartWithMoonIn(18, pada)).present)
                .isTrue()
            assertWithMessage("Rohini pada $pada")
                .that(gandaMoolaDoshaOf(chartWithMoonIn(4, pada)).present)
                .isFalse()
        }
    }

    @Test
    fun `the six are three pairs straddling a rashi boundary`() {
        // Not an arbitrary list: 27 nakshatras do not divide evenly into 12 rashis, so three seams
        // fall inside a nakshatra. Each pair is the one ending a water sign and the one starting the
        // fire sign after it. If this ever fails, the set has been edited without the reason for it.
        listOf(27 to 1, 9 to 10, 18 to 19).forEach { (end, start) ->
            assertWithMessage("pair $end->$start").that(GANDA_MOOLA_NAKSHATRAS).containsAtLeast(end, start)
        }
        assertThat(GANDA_MOOLA_NAKSHATRAS).hasSize(6)
    }

    @Test
    fun `the working names the Moon's nakshatra either way`() {
        val afflicted = gandaMoolaDoshaOf(chartWithMoonIn(19))
        assertThat(afflicted.rule).contains("Mula")
        assertThat(afflicted.summary).isNotNull()

        // "Why not" is worth stating too, so the rule is checkable from a chart that does not have it.
        val clear = gandaMoolaDoshaOf(chartWithMoonIn(4))
        assertThat(clear.rule).contains("Rohini")
        assertThat(clear.summary).isNull()
    }

    @Test
    fun `the reference births are judged by their own Moon`() {
        // Real charts rather than hand-placed ones, checking the wiring to moonNakshatra rather than
        // the rule: whatever the ephemeris put the Moon in, the verdict must agree with the set.
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            assertWithMessage("${birth.label}, Moon in ${chart.moonNakshatra.name}")
                .that(gandaMoolaDoshaOf(chart).present)
                .isEqualTo(chart.moonNakshatra.number in GANDA_MOOLA_NAKSHATRAS)
        }
    }

    private fun chartWithMoonIn(
        nakshatraNumber: Int,
        pada: Int = 1,
    ): NatalChart =
        NatalChart(
            lagna = Lagna(siderealLongitude = 0.0, rasi = Rasi(index = 0, name = "Mesha")),
            houses = (0 until 12).map { Rasi(index = it, name = "R$it") },
            moonHouses = (0 until 12).map { Rasi(index = it, name = "R$it") },
            grahas = emptyList(),
            moonNakshatra = Nakshatra(number = nakshatraNumber, name = NAKSHATRA_NAMES[nakshatraNumber - 1]),
            moonPada = pada,
            vimshottari = emptyList(),
        )
}
