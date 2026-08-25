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
import kotlin.math.abs

/**
 * The three-level dasha recursion against an independent implementation.
 *
 * The Moon's longitude is **supplied rather than computed**, for the reason [VargaRuleTest] spells
 * out: comparing a whole timeline against another implementation asks about our rule and about our
 * ephemeris at once, and here the ephemeris would dominate by a wide margin. A single arcminute of
 * disagreement in the Moon is 0.00125 of a nakshatra, which over a seven-year first mahadasha moves
 * every boundary in the timeline by about three days. Feeding in their Moon isolates the arithmetic,
 * which is what this change actually touched.
 *
 * The goldens are a sample of all 729 periods — every level, spanning the full 120 years. The
 * complete set was checked during derivation and matched on lord and order at every one.
 */
class DashaRecursionTest {
    @Test
    fun `three levels match the reference implementation`() {
        val flat = flattenedTimeline()
        assertThat(flat).hasSize(729)
        val mismatches = mutableListOf<String>()
        DASHA_GOLDENS.forEach { golden ->
            val actual = flat[golden.index]
            val lords = listOf(actual.maha, actual.antara, actual.pratyantara).map { it.lord }
            if (lords != golden.lords) {
                mismatches += "row ${golden.index}: expected ${golden.lords}, got $lords"
                return@forEach
            }
            val drift = abs(actual.pratyantara.start.toEpochMilliseconds() - golden.startMillis)
            if (drift > TOLERANCE_MILLIS) {
                mismatches += "row ${golden.index} ${golden.lords}: start off by ${drift / 60_000} min"
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `the year length is the sidereal one`() {
        // Guards the constant directly. With the Julian year the far end of the timeline drifts by
        // about eighteen hours, which the goldens above would catch, but this says why.
        val ketu = vimshottariFromMoon(0.0, 0L).first()
        val sevenSiderealYears = (7 * 365.2564 * 24 * 60 * 60 * 1000).toLong()
        assertThat(ketu.end.toEpochMilliseconds() - ketu.start.toEpochMilliseconds())
            .isEqualTo(sevenSiderealYears)
    }

    @Test
    fun `every level divides into nine, and the third does not divide further`() {
        val maha = vimshottariFromMoon(0.0, 0L).first()
        assertThat(maha.level).isEqualTo(1)
        assertThat(maha.subPeriods).hasSize(9)
        val antara = maha.subPeriods.first()
        assertThat(antara.level).isEqualTo(2)
        assertThat(antara.subPeriods).hasSize(9)
        val pratyantara = antara.subPeriods.first()
        assertThat(pratyantara.level).isEqualTo(3)
        // The rule keeps applying, but a fourth level splits a few weeks into a few hours, which is
        // finer than a birth time known to the minute supports.
        assertThat(pratyantara.subPeriods).isEmpty()
    }

    @Test
    fun `sub-periods tile their parent exactly at every level`() {
        vimshottariFromMoon(123.456, 1_000_000_000_000L).forEach { maha ->
            assertTiles(maha)
            maha.subPeriods.forEach { assertTiles(it) }
        }
    }

    @Test
    fun `each period's first sub-period is ruled by its own lord`() {
        vimshottariFromMoon(123.456, 1_000_000_000_000L).forEach { maha ->
            assertWithMessage(maha.lord.displayName).that(maha.subPeriods.first().lord).isEqualTo(maha.lord)
            maha.subPeriods.forEach { antara ->
                assertWithMessage("${maha.lord}/${antara.lord}")
                    .that(antara.subPeriods.first().lord)
                    .isEqualTo(antara.lord)
            }
        }
    }

    @Test
    fun `antardashas is the same thing as subPeriods`() {
        val maha = vimshottariFromMoon(200.0, 0L).first()
        assertThat(maha.antardashas).isEqualTo(maha.subPeriods)
    }

    private fun assertTiles(parent: DashaPeriod) {
        val children = parent.subPeriods
        assertWithMessage("${parent.lord} level ${parent.level} starts with its parent")
            .that(children.first().start)
            .isEqualTo(parent.start)
        assertWithMessage("${parent.lord} level ${parent.level} ends with its parent")
            .that(children.last().end)
            .isEqualTo(parent.end)
        children.zipWithNext().forEach { (earlier, later) ->
            assertWithMessage("${parent.lord}: ${earlier.lord} into ${later.lord}")
                .that(later.start)
                .isEqualTo(earlier.end)
        }
    }

    private fun flattenedTimeline(): List<Nested> =
        vimshottariFromMoon(REFERENCE_MOON, REFERENCE_BIRTH_MILLIS).flatMap { maha ->
            maha.subPeriods.flatMap { antara ->
                antara.subPeriods.map { Nested(maha, antara, it) }
            }
        }

    /** Named rather than using kotlin.Triple, which it would shadow and read worse than. */
    private data class Nested(
        val maha: DashaPeriod,
        val antara: DashaPeriod,
        val pratyantara: DashaPeriod,
    )
}

/** The Hyderabad 1990 reference birth, with the reference implementation's own Moon longitude. */
private const val REFERENCE_MOON = 294.3147
private const val REFERENCE_BIRTH_MILLIS = 642_916_200_000L

/**
 * Generous next to the 604-second worst case measured during derivation, because the goldens carry
 * the Moon to four decimals and that rounding alone is worth minutes at the end of 120 years. Still
 * far tighter than the eighteen-hour drift a wrong year length produces.
 */
private const val TOLERANCE_MILLIS = 20 * 60 * 1000L

/** One reference period: where it sits in the flattened timeline, its three lords, and its start. */
private data class DashaGolden(
    val index: Int,
    val lords: List<Graha>,
    val startMillis: Long,
)

private val DASHA_GOLDENS =
    listOf(
        DashaGolden(0, listOf(Graha.MANGALA, Graha.MANGALA, Graha.MANGALA), 626657491000L),
        DashaGolden(18, listOf(Graha.MANGALA, Graha.GURU, Graha.GURU), 672679793000L),
        DashaGolden(36, listOf(Graha.MANGALA, Graha.BUDHA, Graha.BUDHA), 737111015000L),
        DashaGolden(54, listOf(Graha.MANGALA, Graha.SHUKRA, Graha.SHUKRA), 781292425000L),
        DashaGolden(72, listOf(Graha.MANGALA, Graha.MOON, Graha.MOON), 829155619000L),
        DashaGolden(90, listOf(Graha.RAHU, Graha.GURU, Graha.GURU), 932771544000L),
        DashaGolden(108, listOf(Graha.RAHU, Graha.BUDHA, Graha.BUDHA), 1098451831000L),
        DashaGolden(126, listOf(Graha.RAHU, Graha.SHUKRA, Graha.SHUKRA), 1212061171000L),
        DashaGolden(144, listOf(Graha.RAHU, Graha.MOON, Graha.MOON), 1335137955000L),
        DashaGolden(162, listOf(Graha.GURU, Graha.GURU, Graha.GURU), 1415611237000L),
        DashaGolden(180, listOf(Graha.GURU, Graha.BUDHA, Graha.BUDHA), 1562882603000L),
        DashaGolden(198, listOf(Graha.GURU, Graha.SHUKRA, Graha.SHUKRA), 1663868683000L),
        DashaGolden(216, listOf(Graha.GURU, Graha.MOON, Graha.MOON), 1773270269000L),
        DashaGolden(234, listOf(Graha.GURU, Graha.RAHU, Graha.RAHU), 1844802075000L),
        DashaGolden(252, listOf(Graha.SHANI, Graha.BUDHA, Graha.BUDHA), 2015479069000L),
        DashaGolden(270, listOf(Graha.SHANI, Graha.SHUKRA, Graha.SHUKRA), 2135400038000L),
        DashaGolden(288, listOf(Graha.SHANI, Graha.MOON, Graha.MOON), 2265314422000L),
        DashaGolden(306, listOf(Graha.SHANI, Graha.RAHU, Graha.RAHU), 2350258442000L),
        DashaGolden(324, listOf(Graha.BUDHA, Graha.BUDHA, Graha.BUDHA), 2520146482000L),
        DashaGolden(342, listOf(Graha.BUDHA, Graha.SHUKRA, Graha.SHUKRA), 2627444191000L),
        DashaGolden(360, listOf(Graha.BUDHA, Graha.MOON, Graha.MOON), 2743683377000L),
        DashaGolden(378, listOf(Graha.BUDHA, Graha.RAHU, Graha.RAHU), 2819685921000L),
        DashaGolden(396, listOf(Graha.BUDHA, Graha.SHANI, Graha.SHANI), 2971691009000L),
        DashaGolden(414, listOf(Graha.KETU, Graha.SHUKRA, Graha.SHUKRA), 3069521274000L),
        DashaGolden(432, listOf(Graha.KETU, Graha.MOON, Graha.MOON), 3117384468000L),
        DashaGolden(450, listOf(Graha.KETU, Graha.RAHU, Graha.RAHU), 3148679633000L),
        DashaGolden(468, listOf(Graha.KETU, Graha.SHANI, Graha.SHANI), 3211269964000L),
        DashaGolden(486, listOf(Graha.SHUKRA, Graha.SHUKRA, Graha.SHUKRA), 3277542078000L),
        DashaGolden(504, listOf(Graha.SHUKRA, Graha.MOON, Graha.MOON), 3414294061000L),
        DashaGolden(522, listOf(Graha.SHUKRA, Graha.RAHU, Graha.RAHU), 3503708819000L),
        DashaGolden(540, listOf(Graha.SHUKRA, Graha.SHANI, Graha.SHANI), 3682538335000L),
        DashaGolden(558, listOf(Graha.SHUKRA, Graha.KETU, Graha.KETU), 3871887234000L),
        DashaGolden(576, listOf(Graha.SUN, Graha.MOON, Graha.MOON), 3918172520000L),
        DashaGolden(594, listOf(Graha.SUN, Graha.RAHU, Graha.RAHU), 3944996948000L),
        DashaGolden(612, listOf(Graha.SUN, Graha.SHANI, Graha.SHANI), 3998645802000L),
        DashaGolden(630, listOf(Graha.SUN, Graha.KETU, Graha.KETU), 4055450472000L),
        DashaGolden(648, listOf(Graha.MOON, Graha.MOON, Graha.MOON), 4098053974000L),
        DashaGolden(666, listOf(Graha.MOON, Graha.RAHU, Graha.RAHU), 4142761353000L),
        DashaGolden(684, listOf(Graha.MOON, Graha.SHANI, Graha.SHANI), 4232176111000L),
        DashaGolden(702, listOf(Graha.MOON, Graha.KETU, Graha.KETU), 4326850561000L),
        DashaGolden(720, listOf(Graha.MOON, Graha.SUN, Graha.SUN), 4397856398000L),
        DashaGolden(728, listOf(Graha.MOON, Graha.SUN, Graha.SHUKRA), 4411005627000L),
    )
