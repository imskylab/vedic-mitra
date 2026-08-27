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
 * Charts are built by **longitude**, not by sign index, which the other dosha tests do not need to
 * do. That is the whole point here: the rule is whole-sign, and the case that separates it from the
 * textbook longitude rule is a graha sharing a node's sign while sitting past the node in degrees.
 * A sign-precise fixture could not express that case at all.
 */
class KalaSarpaDoshaTest {
    @Test
    fun `all seven on one side of the axis raises the dosha`() {
        // Rahu at 10° Karka (3), so Ketu at 10° Makara (9). The arc runs Karka..Makara; everything
        // is parked in Simha and Kanya, well inside it.
        val chart =
            chartWith(
                rahuLongitude = 100.0,
                Graha.SUN to 130.0,
                Graha.MOON to 135.0,
                Graha.MANGALA to 140.0,
                Graha.BUDHA to 155.0,
                Graha.GURU to 160.0,
                Graha.SHUKRA to 165.0,
                Graha.SHANI to 170.0,
            )

        assertThat(kalaSarpaDoshaOf(chart).present).isTrue()
    }

    @Test
    fun `one graha outside the arc breaks it`() {
        // Saturn moved to Meena (11), on the far side of the Rahu-Ketu axis.
        val chart =
            chartWith(
                rahuLongitude = 100.0,
                Graha.SUN to 130.0,
                Graha.MOON to 135.0,
                Graha.MANGALA to 140.0,
                Graha.BUDHA to 155.0,
                Graha.GURU to 160.0,
                Graha.SHUKRA to 165.0,
                Graha.SHANI to 340.0,
            )

        val dosha = kalaSarpaDoshaOf(chart)
        assertThat(dosha.present).isFalse()
        assertWithMessage("the working names the graha that broke it").that(dosha.rule).contains("Shani")
        assertThat(dosha.summary).isNull()
    }

    @Test
    fun `a graha outside the arc by longitude but sharing a node's sign is still inside`() {
        // THE case the rule turns on, and the one a longitude comparison gets wrong. Both variants
        // are checked, because both occurred among the sampled positives.

        // Behind Rahu: Rahu at 10° Karka, Saturn at 5° Karka. Going forward from Rahu, Saturn is
        // 355° away -- outside the 180° arc -- yet it shares Rahu's sign.
        val behindRahu =
            chartWith(
                rahuLongitude = 100.0,
                Graha.SUN to 130.0,
                Graha.MOON to 135.0,
                Graha.MANGALA to 140.0,
                Graha.BUDHA to 155.0,
                Graha.GURU to 160.0,
                Graha.SHUKRA to 165.0,
                Graha.SHANI to 95.0,
            )
        assertWithMessage("5° Karka is behind Rahu at 10° Karka, but in its sign")
            .that(kalaSarpaDoshaOf(behindRahu).present)
            .isTrue()

        // Past Ketu: Ketu at 10° Makara, the Moon at 15° Makara — 185° from Rahu, so past the far
        // end of the arc, but again in the node's own sign.
        val pastKetu =
            chartWith(
                rahuLongitude = 100.0,
                Graha.SUN to 130.0,
                Graha.MOON to 285.0,
                Graha.MANGALA to 140.0,
                Graha.BUDHA to 155.0,
                Graha.GURU to 160.0,
                Graha.SHUKRA to 165.0,
                Graha.SHANI to 170.0,
            )
        assertWithMessage("15° Makara is past Ketu at 10° Makara, but in its sign")
            .that(kalaSarpaDoshaOf(pastKetu).present)
            .isTrue()
    }

    @Test
    fun `the arc counts from either node`() {
        // Grahas gathered on Ketu's side rather than Rahu's. Sources describe this reversed axis as
        // a separate "Kala Amrita" variant; the reference reports the plain dosha either way, so
        // this engine does too rather than inventing a distinction it cannot check.
        val chart =
            chartWith(
                rahuLongitude = 100.0,
                Graha.SUN to 300.0,
                Graha.MOON to 310.0,
                Graha.MANGALA to 320.0,
                Graha.BUDHA to 330.0,
                Graha.GURU to 340.0,
                Graha.SHUKRA to 350.0,
                Graha.SHANI to 20.0,
            )

        assertThat(kalaSarpaDoshaOf(chart).present).isTrue()
    }

    @Test
    fun `the type is named from Rahu's house`() {
        // All twelve confirmed against the reference by holding a chart's date fixed and sweeping
        // the birth time, which turns the lagna through every house in a day.
        val expected =
            listOf(
                "Ananta",
                "Kulika",
                "Vasuki",
                "Shankhapala",
                "Padma",
                "Mahapadma",
                "Takshaka",
                "Karkotaka",
                "Shankhachooda",
                "Ghataka",
                "Vishdhara",
                "Sheshanaga",
            )
        expected.forEachIndexed { index, name ->
            // Rahu in Karka (sign 3). Choosing the lagna sign places it in house index+1.
            val lagnaSign = (3 - index + 12) % 12
            val chart =
                chartWith(
                    rahuLongitude = 100.0,
                    lagnaSign = lagnaSign,
                    placements =
                        arrayOf(
                            Graha.SUN to 130.0,
                            Graha.MOON to 135.0,
                            Graha.MANGALA to 140.0,
                            Graha.BUDHA to 155.0,
                            Graha.GURU to 160.0,
                            Graha.SHUKRA to 165.0,
                            Graha.SHANI to 170.0,
                        ),
                )
            val dosha = kalaSarpaDoshaOf(chart)
            assertWithMessage("Rahu in house ${index + 1}").that(dosha.name).isEqualTo("Kala Sarpa ($name)")
            assertWithMessage("Rahu in house ${index + 1}").that(dosha.rule).contains(name)
        }
    }

    @Test
    fun `a chart missing grahas does not claim the dosha`() {
        // The sample charts the Kundali previews build carry only a few grahas; absent data must not
        // read as "all seven are hemmed", which an empty-list check would.
        val sparse =
            chartWith(rahuLongitude = 100.0, placements = arrayOf(Graha.SUN to 130.0, Graha.MOON to 135.0))

        assertThat(kalaSarpaDoshaOf(sparse).present).isFalse()
    }

    @Test
    fun `the reference births are judged consistently with their own placements`() {
        // Real charts rather than hand-placed ones: whatever the ephemeris produced, the verdict must
        // agree with the whole-sign rule re-derived independently here.
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            val byGraha = chart.grahas.associateBy { it.graha }
            val rahuSign = byGraha.getValue(Graha.RAHU).rasi.index
            val ketuSign = byGraha.getValue(Graha.KETU).rasi.index
            val signs =
                listOf(
                    Graha.SUN,
                    Graha.MOON,
                    Graha.MANGALA,
                    Graha.BUDHA,
                    Graha.GURU,
                    Graha.SHUKRA,
                    Graha.SHANI,
                ).map { byGraha.getValue(it).rasi.index }
            val arc = { start: Int -> (0 until 7).map { (start + it) % 12 }.toSet() }
            val expected = signs.all { it in arc(rahuSign) } || signs.all { it in arc(ketuSign) }

            assertWithMessage("${birth.label}: Rahu in $rahuSign, grahas in $signs")
                .that(kalaSarpaDoshaOf(chart).present)
                .isEqualTo(expected)
        }
    }

    private fun chartWith(
        rahuLongitude: Double,
        vararg placements: Pair<Graha, Double>,
        lagnaSign: Int = 0,
    ): NatalChart {
        val longitudes =
            placements.toMap() +
                mapOf(Graha.RAHU to rahuLongitude, Graha.KETU to (rahuLongitude + 180.0) % 360.0)
        val grahas =
            longitudes.map { (graha, longitude) ->
                val signIndex = (longitude / 30.0).toInt() % 12
                NatalGraha(
                    graha = graha,
                    siderealLongitude = longitude,
                    rasi = Rasi(index = signIndex, name = RASHI_NAMES[signIndex]),
                    house = ((signIndex - lagnaSign + 12) % 12) + 1,
                    houseFromMoon = 1,
                    retrograde = false,
                )
            }
        return NatalChart(
            lagna = Lagna(siderealLongitude = lagnaSign * 30.0, rasi = Rasi(lagnaSign, RASHI_NAMES[lagnaSign])),
            houses = (0 until 12).map { Rasi(it, RASHI_NAMES[it]) },
            moonHouses = (0 until 12).map { Rasi(it, RASHI_NAMES[it]) },
            grahas = grahas,
            moonNakshatra = Nakshatra(number = 1, name = "Ashwini"),
            moonPada = 1,
            vimshottari = emptyList(),
        )
    }
}
