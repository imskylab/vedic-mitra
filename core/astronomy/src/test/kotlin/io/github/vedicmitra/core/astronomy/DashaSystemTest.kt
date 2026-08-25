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
 * Ashtottari and Yogini against the independent implementation their tables were read from.
 *
 * 57 births covering all 27 nakshatras, from two sweeps taken independently of one another, which
 * agreed on every starting lord. The Moon is supplied rather than computed, for the reason
 * [DashaRecursionTest] gives: an arcminute of ephemeris disagreement moves every boundary in a
 * timeline by days, and would drown out the arithmetic under test.
 *
 * Eight goldens carry `lordOnly`. Those are the births whose Moon falls in Rahu's Ashtottari run —
 * nakshatras 26, 27, 1 and 2, the run that wraps the end of the zodiac back to its start — where the
 * reference does something with the period boundaries that could not be recovered from the data.
 * Their **lord is still asserted**, because that part is verified; only the start time is not. See
 * `ashtottariWrapCaveat`.
 */
class DashaSystemTest {
    @Test
    fun `each system starts on the lord the reference reports`() {
        val mismatches = mutableListOf<String>()
        SYSTEM_GOLDENS.forEach { golden ->
            val actual = dashaFromMoon(golden.system, golden.moonLongitude, golden.birthMillis).first()
            if (actual.lord != golden.lord) {
                mismatches +=
                    "${golden.system.displayName} at ${golden.moonLongitude}: " +
                    "expected ${golden.lord}, got ${actual.lord}"
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `each system's first period begins when the reference says`() {
        val mismatches = mutableListOf<String>()
        SYSTEM_GOLDENS.filterNot { it.lordOnly }.forEach { golden ->
            val actual = dashaFromMoon(golden.system, golden.moonLongitude, golden.birthMillis).first()
            val drift = abs(actual.start.toEpochMilliseconds() - golden.startMillis)
            if (drift > TOLERANCE_MILLIS) {
                mismatches +=
                    "${golden.system.displayName} at ${golden.moonLongitude}: " +
                    "start off by ${drift / 3_600_000} h"
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `the wrap-group exclusion stays as small as it is claimed to be`() {
        // If this grows, something has been quietly excused rather than fixed.
        assertThat(SYSTEM_GOLDENS.count { it.lordOnly }).isEqualTo(8)
        assertThat(SYSTEM_GOLDENS.filter { it.lordOnly }.map { it.system }.distinct())
            .containsExactly(DashaSystem.ASHTOTTARI)
    }

    @Test
    fun `Ashtottari runs eight lords over 108 years and has no Ketu`() {
        val system = DashaSystem.ASHTOTTARI
        assertThat(system.lords).hasSize(8)
        assertThat(system.totalYears).isEqualTo(108)
        assertThat(system.lords.map { it.first }).doesNotContain(Graha.KETU)
    }

    @Test
    fun `Yogini runs eight lords over 36 years, one to eight years each`() {
        val system = DashaSystem.YOGINI
        assertThat(system.lords).hasSize(8)
        assertThat(system.totalYears).isEqualTo(36)
        assertThat(system.lords.map { it.second }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8)
    }

    @Test
    fun `Yogini keeps the backward step between Ardra and Punarvasu`() {
        // The one place the lord moves back two instead of forward one. Both sweeps reproduce it, so
        // the table encodes it deliberately -- a "tidy-up" here would silently change every Yogini
        // reading for those nakshatras.
        val lords = DashaSystem.YOGINI.lords.map { it.first }
        val ardra = DashaSystem.YOGINI.starts[5].lord
        val punarvasu = DashaSystem.YOGINI.starts[6].lord
        assertThat(lords.indexOf(punarvasu)).isEqualTo((lords.indexOf(ardra) - 2 + 8) % 8)
    }

    @Test
    fun `every system covers all 27 nakshatras with lords drawn from its own cycle`() {
        DashaSystem.entries.forEach { system ->
            assertWithMessage(system.displayName).that(system.starts).hasSize(27)
            val cycle = system.lords.map { it.first }.toSet()
            system.starts.forEachIndexed { index, start ->
                assertWithMessage("${system.displayName} nakshatra ${index + 1}")
                    .that(start.lord)
                    .isIn(cycle)
                assertWithMessage("${system.displayName} nakshatra ${index + 1} position")
                    .that(start.position)
                    .isLessThan(start.runLength)
            }
        }
    }
}

/** One reference birth: the Moon it was computed from, and the first period it produced. */
private data class SystemGolden(
    val system: DashaSystem,
    val moonLongitude: Double,
    val birthMillis: Long,
    val lord: Graha,
    val startMillis: Long,
    val lordOnly: Boolean,
)

/** Ashtottari's longest period is 21 years, so an hour here is a far tighter bound than it looks. */
private const val TOLERANCE_MILLIS = 60 * 60 * 1000L

private val SYSTEM_GOLDENS =
    listOf(
        SystemGolden(DashaSystem.ASHTOTTARI, 3.8549, 643348200000L, Graha.RAHU, 236480506000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 4.0509, 730698600000L, Graha.RAHU, 324072956000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 16.1397, 730785000000L, Graha.RAHU, 339087583000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 18.9733, 643434600000L, Graha.RAHU, 255236353000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 28.4153, 730871400000L, Graha.SHUKRA, 701900744000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 34.2575, 643521000000L, Graha.SHUKRA, 517756436000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 40.9514, 730957800000L, Graha.SHUKRA, 494287589000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 49.5215, 643607400000L, Graha.SHUKRA, 264947814000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 53.8263, 731044200000L, Graha.SHUKRA, 281062493000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 58.6939, 728711400000L, Graha.SHUKRA, 198082580000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 64.5829, 643693800000L, Graha.SHUKRA, 15497244000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 67.1138, 731130600000L, Graha.SUN, 729543167000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 72.6124, 728797800000L, Graha.SUN, 707688811000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 79.2944, 643780200000L, Graha.SUN, 598948014000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 80.8709, 731217000000L, Graha.SUN, 680787852000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 87.0077, 728884200000L, Graha.SUN, 656667419000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 93.5638, 643866600000L, Graha.SUN, 548373928000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 95.1216, 731303400000L, Graha.SUN, 630280000000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 101.8200, 728970600000L, Graha.SUN, 604165861000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 107.3565, 643953000000L, Graha.SUN, 499492086000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 116.9317, 729057000000L, Graha.SUN, 550601536000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 120.6869, 644039400000L, Graha.MOON, 635910778000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 132.1786, 729143400000L, Graha.MOON, 585017634000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 133.6031, 644125800000L, Graha.MOON, 483142842000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 147.3764, 729229800000L, Graha.MOON, 405249218000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 158.4706, 644298600000L, Graha.MOON, 189025714000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 162.3532, 729316200000L, Graha.MANGALA, 718176652000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 170.5703, 644385000000L, Graha.MANGALA, 594348197000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 176.9789, 729402600000L, Graha.MANGALA, 649029062000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 182.5386, 644471400000L, Graha.MANGALA, 537779889000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 191.1792, 729489000000L, Graha.MANGALA, 581895353000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 194.4329, 644557800000L, Graha.MANGALA, 481562232000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 204.9345, 729575400000L, Graha.MANGALA, 516867746000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 206.2999, 644644200000L, Graha.MANGALA, 425473134000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 218.1773, 644730600000L, Graha.BUDHA, 579762897000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 218.2685, 729661800000L, Graha.BUDHA, 663470247000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 230.0948, 644817000000L, Graha.BUDHA, 420008249000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 231.2319, 729748200000L, Graha.BUDHA, 489688675000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 242.0785, 644903400000L, Graha.BUDHA, 259367208000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 243.8883, 729834600000L, Graha.BUDHA, 320025342000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 254.1535, 644989800000L, Graha.SHANI, 638518978000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 256.3025, 729921000000L, Graha.SHANI, 706495743000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 268.5335, 730007400000L, Graha.SHANI, 610084835000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 278.6964, 645162600000L, Graha.SHANI, 445059447000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 280.6305, 730093800000L, Graha.SHANI, 514731684000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 291.2396, 645249000000L, Graha.SHANI, 346186173000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 292.6317, 730180200000L, Graha.SHANI, 420134101000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 294.3147, 642916200000L, Graha.GURU, 628205939000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 304.5661, 730266600000L, Graha.GURU, 561886803000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 307.3405, 643002600000L, Graha.GURU, 433034144000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 316.4561, 730353000000L, Graha.GURU, 383739400000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 320.7829, 643089000000L, Graha.GURU, 231616559000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 328.3226, 730439400000L, Graha.GURU, 205945950000L, false),
        SystemGolden(DashaSystem.ASHTOTTARI, 334.6863, 643175400000L, Graha.RAHU, 644846128000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 340.1887, 730525800000L, Graha.RAHU, 738991429000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 349.0582, 643261800000L, Graha.RAHU, 662680213000L, true),
        SystemGolden(DashaSystem.ASHTOTTARI, 352.0847, 730612200000L, Graha.RAHU, 753767979000L, true),
        SystemGolden(DashaSystem.YOGINI, 3.8549, 643348200000L, Graha.SHUKRA, 579480208000L, false),
        SystemGolden(DashaSystem.YOGINI, 4.0509, 730698600000L, Graha.SHUKRA, 663583097000L, false),
        SystemGolden(DashaSystem.YOGINI, 16.1397, 730785000000L, Graha.RAHU, 677647283000L, false),
        SystemGolden(DashaSystem.YOGINI, 18.9733, 643434600000L, Graha.RAHU, 536642944000L, false),
        SystemGolden(DashaSystem.YOGINI, 28.4153, 730871400000L, Graha.MOON, 726732735000L, false),
        SystemGolden(DashaSystem.YOGINI, 34.2575, 643521000000L, Graha.MOON, 625554634000L, false),
        SystemGolden(DashaSystem.YOGINI, 40.9514, 730957800000L, Graha.SUN, 726454040000L, false),
        SystemGolden(DashaSystem.YOGINI, 49.5215, 643607400000L, Graha.SUN, 598535247000L, false),
        SystemGolden(DashaSystem.YOGINI, 53.8263, 731044200000L, Graha.GURU, 727543796000L, false),
        SystemGolden(DashaSystem.YOGINI, 58.6939, 728711400000L, Graha.GURU, 690647948000L, false),
        SystemGolden(DashaSystem.YOGINI, 64.5829, 643693800000L, Graha.GURU, 563815604000L, false),
        SystemGolden(DashaSystem.YOGINI, 67.1138, 731130600000L, Graha.MANGALA, 726897444000L, false),
        SystemGolden(DashaSystem.YOGINI, 72.6124, 728797800000L, Graha.MANGALA, 672507163000L, false),
        SystemGolden(DashaSystem.YOGINI, 79.2944, 643780200000L, Graha.MANGALA, 524227705000L, false),
        SystemGolden(DashaSystem.YOGINI, 80.8709, 731217000000L, Graha.SUN, 727094435000L, false),
        SystemGolden(DashaSystem.YOGINI, 87.0077, 728884200000L, Graha.SUN, 695711458000L, false),
        SystemGolden(DashaSystem.YOGINI, 93.5638, 643866600000L, Graha.GURU, 642230156000L, false),
        SystemGolden(DashaSystem.YOGINI, 95.1216, 731303400000L, Graha.GURU, 718605498000L, false),
        SystemGolden(DashaSystem.YOGINI, 101.8200, 728970600000L, Graha.GURU, 668710021000L, false),
        SystemGolden(DashaSystem.YOGINI, 107.3565, 643953000000L, Graha.MANGALA, 637421694000L, false),
        SystemGolden(DashaSystem.YOGINI, 116.9317, 729057000000L, Graha.MANGALA, 631873561000L, false),
        SystemGolden(DashaSystem.YOGINI, 120.6869, 644039400000L, Graha.BUDHA, 635910778000L, false),
        SystemGolden(DashaSystem.YOGINI, 132.1786, 729143400000L, Graha.BUDHA, 585017634000L, false),
        SystemGolden(DashaSystem.YOGINI, 133.6031, 644125800000L, Graha.SHANI, 640295150000L, false),
        SystemGolden(DashaSystem.YOGINI, 147.3764, 729229800000L, Graha.SHUKRA, 717471083000L, false),
        SystemGolden(DashaSystem.YOGINI, 158.4706, 644298600000L, Graha.SHUKRA, 448730657000L, false),
        SystemGolden(DashaSystem.YOGINI, 162.3532, 729316200000L, Graha.RAHU, 684758006000L, false),
        SystemGolden(DashaSystem.YOGINI, 170.5703, 644385000000L, Graha.RAHU, 444237788000L, false),
        SystemGolden(DashaSystem.YOGINI, 176.9789, 729402600000L, Graha.MOON, 720773981000L, false),
        SystemGolden(DashaSystem.YOGINI, 182.5386, 644471400000L, Graha.MOON, 622683794000L, false),
        SystemGolden(DashaSystem.YOGINI, 191.1792, 729489000000L, Graha.SUN, 708127953000L, false),
        SystemGolden(DashaSystem.YOGINI, 194.4329, 644557800000L, Graha.SUN, 607794831000L, false),
        SystemGolden(DashaSystem.YOGINI, 204.9345, 729575400000L, Graha.GURU, 694537268000L, false),
        SystemGolden(DashaSystem.YOGINI, 206.2999, 644644200000L, Graha.GURU, 599910949000L, false),
        SystemGolden(DashaSystem.YOGINI, 218.1773, 644730600000L, Graha.MANGALA, 598871045000L, false),
        SystemGolden(DashaSystem.YOGINI, 218.2685, 729661800000L, Graha.MANGALA, 682938351000L, false),
        SystemGolden(DashaSystem.YOGINI, 230.0948, 644817000000L, Graha.BUDHA, 604247087000L, false),
        SystemGolden(DashaSystem.YOGINI, 231.2319, 729748200000L, Graha.BUDHA, 675721722000L, false),
        SystemGolden(DashaSystem.YOGINI, 242.0785, 644903400000L, Graha.SHANI, 615386407000L, false),
        SystemGolden(DashaSystem.YOGINI, 243.8883, 729834600000L, Graha.SHANI, 674616714000L, false),
        SystemGolden(DashaSystem.YOGINI, 254.1535, 644989800000L, Graha.SHUKRA, 631401074000L, false),
        SystemGolden(DashaSystem.YOGINI, 256.3025, 729921000000L, Graha.SHUKRA, 680727960000L, false),
        SystemGolden(DashaSystem.YOGINI, 268.5335, 730007400000L, Graha.RAHU, 694658442000L, false),
        SystemGolden(DashaSystem.YOGINI, 278.6964, 645162600000L, Graha.RAHU, 417380231000L, false),
        SystemGolden(DashaSystem.YOGINI, 280.6305, 730093800000L, Graha.MOON, 728601465000L, false),
        SystemGolden(DashaSystem.YOGINI, 291.2396, 645249000000L, Graha.MOON, 618646452000L, false),
        SystemGolden(DashaSystem.YOGINI, 292.6317, 730180200000L, Graha.MOON, 700282670000L, false),
        SystemGolden(DashaSystem.YOGINI, 294.3147, 642916200000L, Graha.SUN, 638270855000L, false),
        SystemGolden(DashaSystem.YOGINI, 304.5661, 730266600000L, Graha.SUN, 677094033000L, false),
        SystemGolden(DashaSystem.YOGINI, 307.3405, 643002600000L, Graha.GURU, 638218307000L, false),
        SystemGolden(DashaSystem.YOGINI, 316.4561, 730353000000L, Graha.GURU, 660842060000L, false),
        SystemGolden(DashaSystem.YOGINI, 320.7829, 643089000000L, Graha.MANGALA, 635676868000L, false),
        SystemGolden(DashaSystem.YOGINI, 328.3226, 730439400000L, Graha.MANGALA, 651645578000L, false),
        SystemGolden(DashaSystem.YOGINI, 334.6863, 643175400000L, Graha.BUDHA, 627164252000L, false),
        SystemGolden(DashaSystem.YOGINI, 340.1887, 730525800000L, Graha.BUDHA, 649396852000L, false),
        SystemGolden(DashaSystem.YOGINI, 349.0582, 643261800000L, Graha.SHANI, 609298954000L, false),
        SystemGolden(DashaSystem.YOGINI, 352.0847, 730612200000L, Graha.SHANI, 653669646000L, false),
    )
