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
 * Mangal dosha, against the rule it claims to implement.
 *
 * These are built from hand-placed charts rather than computed ones on purpose. The rule is a
 * statement about *house counts*, and a computed chart puts Mars wherever the ephemeris puts it —
 * which exercises whichever cases that birth happens to produce and silently skips the rest. Placing
 * Mars deliberately covers every house of the rule, both reference points that disagree, and each
 * parihara in isolation, none of which a real chart can be relied on to contain.
 *
 * The reference charts still get a pass at the end, for the invariants that must hold on real data.
 */
class MangalDoshaTest {
    @Test
    fun `Mars in a dosha house from the lagna raises it`() {
        setOf(1, 2, 4, 7, 8, 12).forEach { house ->
            // Lagna is Mesha (0), so house n means Mars in sign n-1.
            val dosha = mangalDoshaOf(chartWith(Graha.MANGALA to house - 1))
            assertWithMessage("Mars in the ${ordinal(house)} from the lagna")
                .that(dosha.afflicted)
                .isTrue()
        }
    }

    @Test
    fun `Mars anywhere else does not raise it`() {
        setOf(3, 5, 6, 9, 10, 11).forEach { house ->
            // Every reference point is put in Mesha too, so only the lagna count can fire.
            val dosha =
                mangalDoshaOf(
                    chartWith(
                        Graha.MANGALA to house - 1,
                        Graha.MOON to 0,
                        Graha.SHUKRA to 0,
                    ),
                )
            assertWithMessage("Mars in the ${ordinal(house)} from the lagna")
                .that(dosha.afflicted)
                .isFalse()
        }
    }

    @Test
    fun `the Moon and Venus are counted from as well as the lagna`() {
        // Mars in Mithuna (2): the 3rd from a Mesha lagna, so the lagna count is clear. But the Moon
        // is in Vrishabha (1), which puts Mars in the 2nd from it.
        val dosha =
            mangalDoshaOf(
                chartWith(
                    Graha.MANGALA to 2,
                    Graha.MOON to 1,
                    Graha.SHUKRA to 5,
                ),
            )
        assertThat(dosha.afflicted).isTrue()
        assertThat(dosha.triggers.map { it.reference }).containsExactly(MangalReference.CHANDRA)
        assertThat(dosha.triggers.single().house).isEqualTo(2)
    }

    @Test
    fun `every trigger names the placement that produced it`() {
        // Mars in Vrischika (7) is the 8th from a Mesha lagna, and the Moon is in Mesha too, so the
        // same placement is the 8th from the Moon as well. Both must be reported, not just the first.
        val dosha =
            mangalDoshaOf(
                chartWith(
                    Graha.MANGALA to 7,
                    Graha.MOON to 0,
                    Graha.SHUKRA to 5,
                ),
            )
        assertThat(dosha.triggers.map { it.reference })
            .containsExactly(MangalReference.LAGNA, MangalReference.CHANDRA)
        assertThat(dosha.triggers.map { it.description })
            .containsExactly("Mars in the 8th from the lagna", "Mars in the 8th from the Moon")
    }

    @Test
    fun `Mars in its own sign or exaltation answers the dosha wherever it arises`() {
        // Mesha and Vrischika are Mars's own; Makara is its exaltation. Each is placed so that it
        // also lands in a dosha house, so the trigger genuinely fires before being answered.
        mapOf(0 to "Mesha", 7 to "Vrischika", 9 to "Makara").forEach { (sign, name) ->
            val dosha = mangalDoshaOf(chartWith(Graha.MANGALA to sign, Graha.MOON to sign, Graha.SHUKRA to sign))
            assertWithMessage("Mars in $name is afflicted before parihara").that(dosha.afflicted).isTrue()
            assertWithMessage("Mars in $name is answered").that(dosha.present).isFalse()
            assertThat(dosha.cancellations).isNotEmpty()
        }
    }

    @Test
    fun `Jupiter with or aspecting Mars answers the dosha`() {
        // Mars in Karka (3), the 4th from a Mesha lagna. Jupiter in Vrischika (7) looks upon Karka by
        // its ninth-house drishti -- Karka is the 9th counted from Vrischika.
        val aspected = mangalDoshaOf(chartWith(Graha.MANGALA to 3, Graha.GURU to 7))
        assertWithMessage("Jupiter aspecting Mars").that(aspected.afflicted).isTrue()
        assertWithMessage("Jupiter aspecting Mars").that(aspected.present).isFalse()

        val conjunct = mangalDoshaOf(chartWith(Graha.MANGALA to 3, Graha.GURU to 3))
        assertWithMessage("Jupiter with Mars").that(conjunct.present).isFalse()

        // Jupiter in Dhanu puts Karka in its 8th, which Jupiter does not aspect, so the dosha stands.
        val unaspected = mangalDoshaOf(chartWith(Graha.MANGALA to 3, Graha.GURU to 8))
        assertWithMessage("Jupiter not aspecting Mars").that(unaspected.present).isTrue()
    }

    @Test
    fun `a house parihara answers its own trigger and no other`() {
        // Mars in Karka (3) is the 4th from a Mesha lagna and the 7th from a Makara Moon. Karka lifts
        // the 7th but not the 4th, so exactly one trigger is answered and the dosha still stands.
        val dosha =
            mangalDoshaOf(
                chartWith(
                    Graha.MANGALA to 3,
                    Graha.MOON to 9,
                    Graha.SHUKRA to 5,
                    farAway = 5,
                ),
            )
        val byReference = dosha.triggers.associateBy { it.reference }
        assertThat(byReference.keys)
            .containsExactly(MangalReference.LAGNA, MangalReference.CHANDRA)
        assertWithMessage("4th from the lagna is not lifted by Karka")
            .that(byReference[MangalReference.LAGNA]?.cancelled)
            .isFalse()
        assertWithMessage("7th from the Moon is lifted by Karka")
            .that(byReference[MangalReference.CHANDRA]?.cancelled)
            .isTrue()
        assertWithMessage("one trigger stands, so the dosha stands").that(dosha.present).isTrue()
    }

    @Test
    fun `a dosha answered on every trigger does not stand`() {
        // Mars in Mithuna (2), the 2nd from a Vrishabha Moon — and Mithuna lifts the 2nd. Nothing
        // else fires, so every trigger is answered.
        val dosha =
            mangalDoshaOf(
                chartWith(
                    Graha.MANGALA to 2,
                    Graha.MOON to 1,
                    Graha.SHUKRA to 5,
                    farAway = 5,
                ),
            )
        assertThat(dosha.afflicted).isTrue()
        assertThat(dosha.triggers.all { it.cancelled }).isTrue()
        assertThat(dosha.present).isFalse()
    }

    @Test
    fun `two afflicted charts cancel each other`() {
        // Mars in Karka, the 4th from the lagna, with Jupiter in Dhanu aspecting nothing of it --
        // Karka is neither strong nor harmless for Mars, so nothing answers the trigger.
        val afflicted = mangalDoshaOf(chartWith(Graha.MANGALA to 3, Graha.GURU to 8))
        // Mars in Kanya with every reference point in Mesha puts it in the 6th from all three.
        val clear = mangalDoshaOf(chartWith(Graha.MANGALA to 5, Graha.MOON to 0, Graha.SHUKRA to 0, farAway = 0))
        assertWithMessage("the afflicted chart really is").that(afflicted.present).isTrue()
        assertWithMessage("the clear chart really is").that(clear.present).isFalse()
        assertThat(mangalDoshaCancelsBetween(afflicted, afflicted)).isTrue()
        assertThat(mangalDoshaCancelsBetween(afflicted, clear)).isFalse()
        assertThat(mangalDoshaCancelsBetween(clear, clear)).isFalse()
    }

    @Test
    fun `the invariants hold on every reference chart`() {
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            val dosha = mangalDoshaOf(chart)
            val mars = chart.grahas.first { it.graha == Graha.MANGALA }
            dosha.triggers.forEach { trigger ->
                assertWithMessage("${birth.label}: ${trigger.description}")
                    .that(trigger.house)
                    .isIn(setOf(1, 2, 4, 7, 8, 12))
                // Every trigger must be a real house count from what it claims to count from.
                val from =
                    when (trigger.reference) {
                        MangalReference.LAGNA -> chart.lagna.rasi.index
                        MangalReference.CHANDRA ->
                            chart.grahas
                                .first { it.graha == Graha.MOON }
                                .rasi.index
                        MangalReference.SHUKRA ->
                            chart.grahas
                                .first { it.graha == Graha.SHUKRA }
                                .rasi.index
                    }
                assertWithMessage("${birth.label}: ${trigger.description} is a real count")
                    .that(trigger.house)
                    .isEqualTo(houseFrom(from, mars.rasi.index))
            }
            assertWithMessage("${birth.label}: present implies afflicted")
                .that(!dosha.present || dosha.afflicted)
                .isTrue()
        }
    }

    private fun chartWith(
        vararg placements: Pair<Graha, Int>,
        farAway: Int = 5,
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
