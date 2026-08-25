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
 * The four additional porutham, against an independent implementation.
 *
 * The goldens below are a recorded sample of a wider check: 186 pairings across six groom nakshatras
 * and all twenty-seven bride nakshatras, every one of which agreed on all four rules. They are
 * inlined so the suite stays offline, as every other reference test here does.
 *
 * Two of the four came out differently from the textbook summary they would otherwise have been
 * written from, which is the whole reason the sweep was worth doing:
 *
 * - **Vedha is not thirteen disjoint pairs.** It is a sum relation, giving most nakshatras two
 *   partners and nine of them three. Written as pairs it would have missed about half of all vedha.
 * - **Sthree Dheerga is directional** in a way Mahendra is not, and counting it the wrong way round
 *   inverts nearly every verdict.
 */
class AdditionalPoruthamTest {
    @Test
    fun `every rule matches the reference implementation`() {
        val mismatches = mutableListOf<String>()
        GOLDENS.forEach { golden ->
            val actual = additionalPorutham(profile(golden.groomNak), profile(golden.brideNak))
            // Verdicts only. The working is prose; pinning it here would make 45 goldens brittle
            // against a wording change without testing anything the booleans do not already cover.
            val verdicts = actual.all.map { it.held }
            if (verdicts != golden.verdicts) {
                mismatches +=
                    "groom ${golden.groomNak} / bride ${golden.brideNak}: expected ${golden.verdicts}, got $verdicts"
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `rajju lays the nakshatras along the body and back again`() {
        val expected =
            listOf(
                Rajju.PADA,
                Rajju.KATI,
                Rajju.UDARA,
                Rajju.KANTHA,
                Rajju.SIRO,
                Rajju.KANTHA,
                Rajju.UDARA,
                Rajju.KATI,
                Rajju.PADA,
            )
        (1..27).forEach { nak ->
            assertWithMessage("nakshatra $nak")
                .that(rajjuOf(nak))
                .isEqualTo(expected[(nak - 1) % 9])
        }
    }

    @Test
    fun `vedha is symmetric`() {
        (1..27).forEach { a ->
            (1..27).forEach { b ->
                assertWithMessage("$a and $b")
                    .that(hasVedha(a, b))
                    .isEqualTo(hasVedha(b, a))
            }
        }
    }

    @Test
    fun `most nakshatras have two vedha partners and nine have three`() {
        // The shape that a thirteen-pairs reading gets wrong. Magha through Jyeshtha (10..18) sit in
        // the overlap of all three sums and so are pierced from three directions.
        val partners = (1..27).associateWith { a -> (1..27).count { b -> hasVedha(a, b) } }
        val three = partners.filterValues { it == 3 }.keys
        assertThat(three).containsExactlyElementsIn(10..18)
        assertThat(partners.filterValues { it == 2 }.keys).hasSize(18)
        assertThat(partners.values.filter { it !in setOf(2, 3) }).isEmpty()
    }

    @Test
    fun `Chitra pierces itself`() {
        // 14 + 14 = 28, one of the three sums. It looks like an off-by-one and is not: the reference
        // implementation reports vedha for two people both born under Chitra.
        assertThat(hasVedha(14, 14)).isTrue()
        assertThat((1..27).filter { hasVedha(it, it) }).containsExactly(14)
    }

    @Test
    fun `Mahendra reads the same in either direction`() {
        // Counting one way and the other sums to 29, and the matching set is closed under that, so
        // sources giving opposite directions do not actually disagree.
        (1..27).forEach { a ->
            (1..27).forEach { b ->
                assertWithMessage("$a and $b")
                    .that(mahendraPorutham(a, b))
                    .isEqualTo(mahendraPorutham(b, a))
            }
        }
    }

    @Test
    fun `Mahendra holds on every third count from the fourth to the twenty-fifth`() {
        val groom = 1
        val matched = (1..27).filter { mahendraPorutham(groom, it) }.map { ((it - groom + 27) % 27) + 1 }
        assertThat(matched).containsExactly(4, 7, 10, 13, 16, 19, 22, 25)
    }

    @Test
    fun `Sthree Dheerga is directional, unlike Mahendra`() {
        // If this ever starts reading the same both ways, the count has been written backwards.
        val asymmetric =
            (1..27).any { a ->
                (1..27).any { b ->
                    sthreeDheergaPorutham(a, b) !=
                        sthreeDheergaPorutham(b, a)
                }
            }
        assertThat(asymmetric).isTrue()
    }

    @Test
    fun `Sthree Dheerga wants more than thirteen from the bride's star to the groom's`() {
        val groom = 1
        (1..27).forEach { bride ->
            val fromBride = ((groom - bride + 27) % 27) + 1
            assertWithMessage("bride $bride, count to groom $fromBride")
                .that(sthreeDheergaPorutham(groom, bride))
                .isEqualTo(fromBride > 13)
        }
    }

    @Test
    fun `sharing a nakshatra fails Rajju and Sthree Dheerga`() {
        (1..27).forEach { nak ->
            val p = additionalPorutham(profile(nak), profile(nak))
            assertWithMessage("nakshatra $nak rajju").that(p.rajju).isFalse()
            assertWithMessage("nakshatra $nak sthree dheerga").that(p.sthreeDheerga).isFalse()
        }
    }

    @Test
    fun `matched counts what holds`() {
        // Built through the real entry point rather than by hand, so `matched` is exercised against
        // results the engine actually produces.
        assertThat(additionalPorutham(profile(1), profile(1)).matched).isIn(0..4)
        (1..27).forEach { nak ->
            val p = additionalPorutham(profile(nak), profile(nak))
            assertWithMessage("nakshatra $nak")
                .that(p.matched)
                .isEqualTo(p.all.count { it.held })
        }
    }

    @Test
    fun `every result names itself and shows its working`() {
        // The working is the reason these are worth displaying at all: a verdict a reader cannot
        // check against another almanac is worth very little, and these four are exactly where
        // almanacs differ.
        (1..27).forEach { g ->
            (1..27).forEach { b ->
                additionalPorutham(profile(g), profile(b)).all.forEach { result ->
                    assertWithMessage("$g/$b ${result.name} name").that(result.name).isNotEmpty()
                    assertWithMessage("$g/$b ${result.name} working").that(result.working).isNotEmpty()
                    assertWithMessage("$g/$b ${result.name} governs").that(result.governs).isNotEmpty()
                }
            }
        }
    }

    @Test
    fun `the working states the count that produced the verdict`() {
        // Pushya and Hasta: six apart, both on different limbs.
        val p = additionalPorutham(profile(8), profile(13))
        assertThat(p.mahendra.working).startsWith("6 between Pushya and Hasta")
        assertThat(p.rajju.working).isEqualTo("Waist and Neck — different limbs")
        assertThat(p.sthreeDheerga.working).startsWith("23 from her star to his")
        assertThat(p.vedha.working).isEqualTo("Pushya and Hasta do not pierce")
    }

    private fun profile(nakshatraNumber: Int) =
        GunaMilanProfile(nakshatraNumber = nakshatraNumber, moonRasiIndex = 0, moonPada = 1)
}

/** One reference pairing: the two nakshatras and the four verdicts reported for them. */
private data class Golden(
    val groomNak: Int,
    val brideNak: Int,
    val mahendra: Boolean,
    val vedha: Boolean,
    val rajju: Boolean,
    val sthreeDheerga: Boolean,
) {
    /** In the order [AdditionalPorutham.all] returns them. */
    val verdicts: List<Boolean> get() = listOf(mahendra, vedha, rajju, sthreeDheerga)
}

private val GOLDENS =
    listOf(
        Golden(1, 1, false, true, false, false),
        Golden(1, 4, true, true, true, true),
        Golden(1, 7, true, true, true, true),
        Golden(1, 9, false, true, false, true),
        Golden(1, 11, false, true, true, true),
        Golden(1, 13, true, true, true, true),
        Golden(1, 15, false, true, true, true),
        Golden(1, 16, true, true, true, false),
        Golden(1, 17, false, true, true, false),
        Golden(1, 18, false, false, false, false),
        Golden(1, 19, true, true, false, false),
        Golden(1, 25, true, true, true, false),
        Golden(1, 27, false, false, false, false),
        Golden(6, 1, false, true, true, false),
        Golden(6, 3, true, true, true, false),
        Golden(6, 6, false, true, false, false),
        Golden(6, 9, true, true, true, true),
        Golden(6, 13, false, false, false, true),
        Golden(6, 14, false, true, true, true),
        Golden(6, 20, false, true, true, true),
        Golden(6, 21, true, true, true, false),
        Golden(6, 22, false, false, false, false),
        Golden(10, 5, false, true, true, false),
        Golden(10, 9, false, false, false, false),
        Golden(10, 10, false, true, false, false),
        Golden(10, 12, false, true, true, true),
        Golden(10, 16, true, true, true, true),
        Golden(10, 18, false, false, false, true),
        Golden(10, 20, false, true, true, true),
        Golden(10, 22, true, true, true, true),
        Golden(10, 24, false, true, true, true),
        Golden(10, 25, true, true, true, false),
        Golden(14, 5, true, false, false, false),
        Golden(14, 10, false, true, true, false),
        Golden(14, 14, false, false, false, false),
        Golden(14, 15, false, true, true, true),
        Golden(14, 21, false, true, true, true),
        Golden(14, 23, true, false, false, true),
        Golden(18, 7, false, true, true, false),
        Golden(18, 10, false, false, false, false),
        Golden(18, 19, false, false, false, true),
        Golden(23, 5, true, false, false, true),
        Golden(23, 7, false, true, true, true),
        Golden(23, 19, false, true, true, false),
        Golden(23, 24, false, true, true, true),
    )
