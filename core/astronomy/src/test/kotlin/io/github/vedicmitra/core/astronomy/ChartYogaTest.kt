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
import org.junit.Test

/**
 * Each yoga rule against a chart built to satisfy it, and against one built not to.
 *
 * The charts are synthetic: grahas are placed at chosen longitudes rather than taken from a real
 * birth, so a rule is tested against exactly the placement it claims to detect. A yoga is a claim
 * people repeat about themselves, so a false positive matters more than a miss.
 */
class ChartYogaTest {
    @Test
    fun `Gajakesari needs Guru in a kendra from the Moon`() {
        // Moon in Mesha, Guru in Karka — the 4th from it.
        assertThat(yogaNames(chartWith(Graha.MOON to 0, Graha.GURU to 3))).contains("Gajakesari")
        // Guru in the 3rd from the Moon is not a kendra.
        assertThat(yogaNames(chartWith(Graha.MOON to 0, Graha.GURU to 2))).doesNotContain("Gajakesari")
    }

    @Test
    fun `Budhaditya needs the Sun and Budha in one rashi`() {
        assertThat(yogaNames(chartWith(Graha.SUN to 5, Graha.BUDHA to 5))).contains("Budhaditya")
        assertThat(yogaNames(chartWith(Graha.SUN to 5, Graha.BUDHA to 6))).doesNotContain("Budhaditya")
    }

    @Test
    fun `Chandra-Mangala needs the Moon and Mangala in one rashi`() {
        assertThat(yogaNames(chartWith(Graha.MOON to 8, Graha.MANGALA to 8))).contains("Chandra-Mangala")
        assertThat(yogaNames(chartWith(Graha.MOON to 8, Graha.MANGALA to 9))).doesNotContain("Chandra-Mangala")
    }

    @Test
    fun `Panchamahapurusha needs own sign or exaltation AND a kendra`() {
        // Shani in Kumbha (its own sign) with Kumbha rising — the 1st house, a kendra.
        assertThat(yogaNames(chartWith(Graha.SHANI to 10), lagnaRasi = 10)).contains("Sasa")
        // Same own sign, but in the 2nd house from a Makara lagna — not a kendra.
        assertThat(yogaNames(chartWith(Graha.SHANI to 10), lagnaRasi = 9)).doesNotContain("Sasa")
        // Exaltation also qualifies: Guru exalted in Karka, in the 1st house.
        assertThat(yogaNames(chartWith(Graha.GURU to 3), lagnaRasi = 3)).contains("Hamsa")
        // A kendra alone is not enough.
        assertThat(yogaNames(chartWith(Graha.SHANI to 2), lagnaRasi = 2)).doesNotContain("Sasa")
    }

    @Test
    fun `Sunapha, Anapha and Durudhara depend on what flanks the Moon`() {
        // Everything parked far away, then one graha in the 2nd from the Moon.
        val sunapha = yogaNames(chartWith(Graha.MOON to 0, Graha.SHUKRA to 1))
        assertThat(sunapha).contains("Sunapha")
        assertThat(sunapha).doesNotContain("Kemadruma")

        val anapha = yogaNames(chartWith(Graha.MOON to 0, Graha.SHUKRA to 11))
        assertThat(anapha).contains("Anapha")

        val durudhara = yogaNames(chartWith(Graha.MOON to 0, Graha.SHUKRA to 1, Graha.SHANI to 11))
        assertThat(durudhara).contains("Durudhara")
        assertThat(durudhara).doesNotContain("Sunapha")
    }

    @Test
    fun `Kemadruma is the Moon with nothing beside it`() {
        // The Sun and the nodes do not count as attendants, so a Moon flanked only by them is bare.
        val names = yogaNames(chartWith(Graha.MOON to 0, Graha.SUN to 1, Graha.RAHU to 11, farAway = 6))
        assertThat(names).contains("Kemadruma")
    }

    @Test
    fun `no yoga is reported twice`() {
        val chart = chartWith(Graha.MOON to 0, Graha.GURU to 3, Graha.SUN to 5, Graha.BUDHA to 5)
        val names = yogaNames(chart)
        assertThat(names).containsNoDuplicates()
    }

    @Test
    fun `every reported yoga states the rule that produced it`() {
        val chart = chartWith(Graha.MOON to 0, Graha.GURU to 3)
        chart.yogas.forEach { yoga ->
            assertThat(yoga.rule).isNotEmpty()
            assertThat(yoga.summary).isNotEmpty()
        }
    }

    private fun yogaNames(
        chart: NatalChart,
        lagnaRasi: Int = 0,
    ): List<String> =
        chart
            .copy(lagna = Lagna(siderealLongitude = lagnaRasi * 30.0, rasi = rasi(lagnaRasi)))
            .yogas
            .map { it.name }

    /**
     * A chart with the named grahas in the given rashi indices and everything else parked in
     * [farAway], so only the placement under test can trigger a rule.
     */
    private fun chartWith(
        vararg placements: Pair<Graha, Int>,
        farAway: Int = 6,
    ): NatalChart {
        val placed = placements.toMap()
        val grahas =
            Graha.entries.map { graha ->
                val rasiIndex = placed[graha] ?: farAway
                NatalGraha(
                    graha = graha,
                    siderealLongitude = rasiIndex * 30.0 + 15.0,
                    rasi = rasi(rasiIndex),
                    house = rasiIndex + 1,
                    houseFromMoon = 1,
                    retrograde = false,
                )
            }
        return NatalChart(
            lagna = Lagna(siderealLongitude = 0.0, rasi = rasi(0)),
            houses = (0 until 12).map { rasi(it) },
            moonHouses = (0 until 12).map { rasi(it) },
            grahas = grahas,
            moonNakshatra = Nakshatra(number = 1, name = "Ashwini"),
            moonPada = 1,
            vimshottari = emptyList(),
        )
    }

    private fun rasi(index: Int) = Rasi(index = index, name = RASHI_NAMES[index])
}
