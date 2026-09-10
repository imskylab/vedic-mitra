/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The day's reading for one person, reported rather than scored.
 *
 * The counting is the part worth pinning: it wraps at 27 nakshatras and at 12 rashis, and an
 * off-by-one there would mislabel every tara without failing anything.
 */
class PersonalStandingTest {
    @Test
    fun `the birth star itself is the Janma tara`() {
        val standing = standingFor(birthNakshatra = 5, dayNakshatra = 5)

        assertThat(standing.tara.number).isEqualTo(1)
        assertThat(standing.tara.name).isEqualTo("Janma")
    }

    @Test
    fun `the count wraps past the twenty-seventh nakshatra`() {
        // Revati (27) to Ashwini (1) is one step on, so the second tara -- Sampat. Counting without
        // the wrap would run backwards through the whole cycle and land on the ninth.
        val standing = standingFor(birthNakshatra = 27, dayNakshatra = 1)

        assertThat(standing.tara.number).isEqualTo(2)
        assertThat(standing.tara.name).isEqualTo("Sampat")
        assertThat(standing.tara.strength).isEqualTo(Bala.STRONG)
    }

    @Test
    fun `the ninth tara is the last before the cycle repeats`() {
        // 27 nakshatras over 9 taras: the 10th step is a Janma again, not a tenth tara.
        assertThat(standingFor(birthNakshatra = 1, dayNakshatra = 9).tara.number).isEqualTo(9)
        assertThat(standingFor(birthNakshatra = 1, dayNakshatra = 10).tara.number).isEqualTo(1)
    }

    @Test
    fun `an unfavourable tara is graded weak`() {
        // Krittika (3) from Ashwini (1) is the third tara, Vipat.
        val standing = standingFor(birthNakshatra = 1, dayNakshatra = 3)

        assertThat(standing.tara.name).isEqualTo("Vipat")
        assertThat(standing.tara.strength).isEqualTo(Bala.WEAK)
    }

    @Test
    fun `chandrabala counts the day's moon sign from the birth sign`() {
        // Mesha (0) to Kanya (5) is the sixth position, which the tradition grades strong.
        val standing = standingFor(birthNakshatra = 1, dayNakshatra = 1, birthMoonRasi = 0, dayMoonRasi = 5)

        val chandrabala = checkNotNull(standing.chandrabala)
        assertThat(chandrabala.position).isEqualTo(6)
        assertThat(chandrabala.strength).isEqualTo(Bala.STRONG)
    }

    @Test
    fun `chandrabala is omitted rather than guessed when the day's moon sign is unknown`() {
        val standing = standingFor(birthNakshatra = 1, dayNakshatra = 1, dayMoonRasi = null)

        assertThat(standing.chandrabala).isNull()
    }

    @Test
    fun `the standing is attributed to the person it was asked for`() {
        assertThat(standingFor(birthNakshatra = 1, dayNakshatra = 1).personId).isEqualTo("meera")
    }

    private fun standingFor(
        birthNakshatra: Int,
        dayNakshatra: Int,
        birthMoonRasi: Int = 0,
        dayMoonRasi: Int? = null,
    ): PersonalStanding =
        personalStandingOn(
            person =
                MuhurtaPerson(
                    id = "meera",
                    context =
                        PersonalMuhurtaContext(
                            birthNakshatraNumber = birthNakshatra,
                            birthMoonRasiIndex = birthMoonRasi,
                        ),
                ),
            dayNakshatraNumber = dayNakshatra,
            dayMoonRasiIndex = dayMoonRasi,
        )
}
