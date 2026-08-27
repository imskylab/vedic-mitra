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
 * Dignity is three small tables and a precedence order, so most of the risk is a mistyped entry
 * rather than a wrong rule. The invariants at the bottom are the ones that would catch that: they
 * are properties the tables must have as a whole, and a single wrong sign index breaks them without
 * anyone having to think of the case.
 */
class DignityTest {
    @Test
    fun `exaltation and debilitation sit opposite one another`() {
        // The Sun is exalted in Mesha (0) and debilitated in Tula (6).
        assertThat(dignityOf(Graha.SUN, 0)).isEqualTo(Dignity.EXALTED)
        assertThat(dignityOf(Graha.SUN, 6)).isEqualTo(Dignity.DEBILITATED)
        // Shani reverses it — exalted in Tula, debilitated in Mesha.
        assertThat(dignityOf(Graha.SHANI, 6)).isEqualTo(Dignity.EXALTED)
        assertThat(dignityOf(Graha.SHANI, 0)).isEqualTo(Dignity.DEBILITATED)
    }

    @Test
    fun `a graha in a sign it rules is in its own sign`() {
        // Mangala rules Mesha (0) and Vrischika (7); Guru rules Dhanu (8) and Meena (11).
        assertThat(dignityOf(Graha.MANGALA, 0)).isEqualTo(Dignity.OWN)
        assertThat(dignityOf(Graha.MANGALA, 7)).isEqualTo(Dignity.OWN)
        assertThat(dignityOf(Graha.GURU, 8)).isEqualTo(Dignity.OWN)
        assertThat(dignityOf(Graha.GURU, 11)).isEqualTo(Dignity.OWN)
    }

    @Test
    fun `debilitation outranks the sign lord's friendship`() {
        // The real precedence case. Guru is debilitated in Makara (9), which Shani rules -- and Guru
        // counts Shani neither friend nor enemy, so without the ordering in dignityOf this would
        // read as a neutral sign and lose the debilitation entirely.
        assertThat(RASI_LORD[9]).isEqualTo(Graha.SHANI)
        assertThat(naturalRelation(Graha.GURU, Graha.SHANI)).isEqualTo(Dignity.NEUTRAL)
        assertThat(dignityOf(Graha.GURU, 9)).isEqualTo(Dignity.DEBILITATED)

        // Likewise exaltation: Shukra is exalted in Meena (11), which Guru rules. Shukra counts Guru
        // neutral, so exaltation has to win or a graha at its strongest would read as unremarkable.
        assertThat(naturalRelation(Graha.SHUKRA, Graha.GURU)).isEqualTo(Dignity.NEUTRAL)
        assertThat(dignityOf(Graha.SHUKRA, 11)).isEqualTo(Dignity.EXALTED)
        assertThat(dignityOf(Graha.SHUKRA, 6)).isEqualTo(Dignity.OWN)
    }

    @Test
    fun `a sign ruled by a friend, a neutral and an enemy read as such`() {
        // Karka (3) is the Moon's. The Sun counts the Moon a friend.
        assertThat(dignityOf(Graha.SUN, 3)).isEqualTo(Dignity.FRIEND)
        // Makara (9) is Shani's, and the Sun counts Shani an enemy.
        assertThat(dignityOf(Graha.SUN, 9)).isEqualTo(Dignity.ENEMY)
        // Kanya (5) is Budha's; Guru counts Budha an enemy, while Mithuna (2) is also Budha's.
        assertThat(dignityOf(Graha.GURU, 2)).isEqualTo(Dignity.ENEMY)
        // Vrischika (7) is Mangala's, and Shani counts Mangala an enemy; Kumbha (10) is Shani's own.
        assertThat(dignityOf(Graha.SHANI, 7)).isEqualTo(Dignity.ENEMY)
        assertThat(dignityOf(Graha.SHANI, 10)).isEqualTo(Dignity.OWN)
    }

    @Test
    fun `Rahu and Ketu have no dignity rather than a made-up one`() {
        // They own no sign, and the exaltations attributed to them are not agreed between sources.
        // Every rashi, so this cannot pass by landing on a sign that happens to be handled.
        (0 until 12).forEach { sign ->
            assertWithMessage("Rahu in sign $sign").that(dignityOf(Graha.RAHU, sign)).isNull()
            assertWithMessage("Ketu in sign $sign").that(dignityOf(Graha.KETU, sign)).isNull()
        }
    }

    @Test
    fun `every graha has a dignity in every sign it can take one in`() {
        CLASSICAL.forEach { graha ->
            (0 until 12).forEach { sign ->
                assertWithMessage("${graha.displayName} in sign $sign")
                    .that(dignityOf(graha, sign))
                    .isNotNull()
            }
        }
    }

    // ---- Invariants over the tables as a whole.

    @Test
    fun `each graha is debilitated exactly six signs from its exaltation`() {
        // Derived rather than tabulated, so this asserts the derivation rather than a second table.
        CLASSICAL.forEach { graha ->
            val exalted = (0 until 12).single { dignityOf(graha, it) == Dignity.EXALTED }
            val debilitated = (0 until 12).single { dignityOf(graha, it) == Dignity.DEBILITATED }
            assertWithMessage("${graha.displayName} exalted $exalted, debilitated $debilitated")
                .that((exalted + 6) % 12)
                .isEqualTo(debilitated)
        }
    }

    @Test
    fun `the five non-luminaries rule two signs each and the luminaries one`() {
        // Twelve signs across seven grahas only comes out even this way, so a mistyped entry in
        // RASI_LORD -- which would silently move an own-sign verdict -- fails here.
        val ruled = CLASSICAL.associateWith { graha -> RASI_LORD.count { it == graha } }
        assertThat(ruled[Graha.SUN]).isEqualTo(1)
        assertThat(ruled[Graha.MOON]).isEqualTo(1)
        listOf(Graha.MANGALA, Graha.BUDHA, Graha.GURU, Graha.SHUKRA, Graha.SHANI).forEach {
            assertWithMessage("${it.displayName} rules").that(ruled[it]).isEqualTo(2)
        }
        assertThat(ruled.values.sum()).isEqualTo(12)
    }

    @Test
    fun `natural friendship is deliberately asymmetric`() {
        // Budha counts the Moon an enemy; the Moon counts Budha a friend. This is the tradition, not
        // a transcription slip, and the assertion exists so nobody "corrects" the tables into a
        // symmetric matrix -- which would change the rule.
        assertThat(naturalRelation(Graha.BUDHA, Graha.MOON)).isEqualTo(Dignity.ENEMY)
        assertThat(naturalRelation(Graha.MOON, Graha.BUDHA)).isEqualTo(Dignity.FRIEND)

        val asymmetric =
            CLASSICAL.flatMap { a ->
                CLASSICAL.filter { it != a }.mapNotNull { b ->
                    "$a/$b".takeIf { naturalRelation(a, b) != naturalRelation(b, a) }
                }
            }
        assertWithMessage("the matrix is not symmetric, by design").that(asymmetric).isNotEmpty()
    }

    @Test
    fun `the Moon counts no graha an enemy`() {
        CLASSICAL.filter { it != Graha.MOON }.forEach {
            assertWithMessage("Moon toward ${it.displayName}")
                .that(naturalRelation(Graha.MOON, it))
                .isNotEqualTo(Dignity.ENEMY)
        }
    }

    @Test
    fun `strong by place means exalted or own sign and nothing else`() {
        // MangalDosha and the Panchamahapurusha yogas both hang off this, so it is pinned here
        // rather than separately in each.
        CLASSICAL.forEach { graha ->
            (0 until 12).forEach { sign ->
                val expected = dignityOf(graha, sign) in setOf(Dignity.EXALTED, Dignity.OWN)
                assertWithMessage("${graha.displayName} in sign $sign")
                    .that(isStrongByPlace(graha, sign))
                    .isEqualTo(expected)
            }
        }
    }

    private companion object {
        /** The seven that take a dignity — the nodes are excluded by [dignityOf], not by this list. */
        val CLASSICAL =
            listOf(
                Graha.SUN,
                Graha.MOON,
                Graha.MANGALA,
                Graha.BUDHA,
                Graha.GURU,
                Graha.SHUKRA,
                Graha.SHANI,
            )
    }
}
