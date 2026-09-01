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
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.common.model.MaasaReckoning
import org.junit.Test

/**
 * The interesting case is the dark fortnight, where the two schemes disagree — a suite that only
 * checked Shukla days would pass with purnimanta doing nothing at all.
 */
class MaasaNamingTest {
    @Test
    fun `the two schemes agree through the bright fortnight`() {
        MONTHS.forEach { month ->
            assertWithMessage("${month.name} Shukla")
                .that(month.nameIn(MaasaReckoning.PURNIMANTA, Paksha.SHUKLA))
                .isEqualTo(month.nameIn(MaasaReckoning.AMANTA, Paksha.SHUKLA))
        }
    }

    @Test
    fun `purnimanta names the dark fortnight after the following month`() {
        // What amanta calls Phalguna Krishna, purnimanta calls Chaitra Krishna. This is the case
        // that moves festival dates a fortnight for readers following the northern scheme.
        val phalguna = Maasa(number = 12, name = "Phalguna", adhika = false)

        assertThat(phalguna.nameIn(MaasaReckoning.AMANTA, Paksha.KRISHNA)).isEqualTo("Phalguna")
        assertThat(phalguna.nameIn(MaasaReckoning.PURNIMANTA, Paksha.KRISHNA)).isEqualTo("Chaitra")
    }

    @Test
    fun `the dark fortnight of every month rolls forward exactly one, wrapping at the year end`() {
        MONTHS.forEach { month ->
            val expected = MONTHS[month.number % MONTHS.size].name
            assertWithMessage("${month.name} Krishna under purnimanta")
                .that(month.nameIn(MaasaReckoning.PURNIMANTA, Paksha.KRISHNA))
                .isEqualTo(expected)
        }
    }

    @Test
    fun `amanta is always the month's own display name`() {
        // Purnimanta must be additive: selecting amanta has to leave every existing reading alone.
        MONTHS.forEach { month ->
            Paksha.entries.forEach { paksha ->
                assertWithMessage("${month.name} $paksha")
                    .that(month.nameIn(MaasaReckoning.AMANTA, paksha))
                    .isEqualTo(month.displayName)
            }
        }
    }

    @Test
    fun `a leap month keeps its prefix in the bright fortnight`() {
        val adhika = Maasa(number = 3, name = "Jyeshtha", adhika = true)

        assertThat(adhika.nameIn(MaasaReckoning.AMANTA, Paksha.SHUKLA)).isEqualTo("Adhika Jyeshtha")
        assertThat(adhika.nameIn(MaasaReckoning.PURNIMANTA, Paksha.SHUKLA)).isEqualTo("Adhika Jyeshtha")
    }

    @Test
    fun `a leap month's dark fortnight takes the nija name, which is the unverified case`() {
        // The month following an Adhika Jyeshtha is the nija (true) Jyeshtha, not Ashadha -- so the
        // rule applied mechanically drops the prefix rather than advancing the number. This is
        // pinned so the behaviour cannot drift silently, NOT because it is confirmed: no
        // independent implementation was available to check intercalary labelling against, and
        // MaasaNaming.kt says so. If a reference later disagrees, change the rule and this case.
        val adhika = Maasa(number = 3, name = "Jyeshtha", adhika = true)

        assertThat(adhika.nameIn(MaasaReckoning.PURNIMANTA, Paksha.KRISHNA)).isEqualTo("Jyeshtha")
    }

    private companion object {
        val MONTHS =
            listOf(
                "Chaitra",
                "Vaishakha",
                "Jyeshtha",
                "Ashadha",
                "Shravana",
                "Bhadrapada",
                "Ashwina",
                "Kartika",
                "Margashirsha",
                "Pausha",
                "Magha",
                "Phalguna",
            ).mapIndexed { index, name -> Maasa(number = index + 1, name = name, adhika = false) }
    }
}
