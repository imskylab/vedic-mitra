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

private const val DAY = 86_400_000L

class NextTithiOccurrenceTest {
    // A synthetic panchanga: sunrise == the day's own millis, the tithi advances 1→30 then repeats,
    // and the month flips from Chaitra to Vaishakha after 30 days.
    private val source =
        object : FestivalPanchangaSource {
            override fun sunrise(dayEpochMillis: Long): Long = dayEpochMillis

            override fun tithiNumber(epochMillis: Long): Int = ((epochMillis / DAY).toInt() % 30) + 1

            override fun sunRashi(epochMillis: Long): Int = 0

            override fun maasa(epochMillis: Long): Maasa {
                val name = if (epochMillis / DAY < 30) "Chaitra" else "Vaishakha"
                return Maasa(number = 1, name = name, adhika = false)
            }
        }

    @Test
    fun `finds the next day matching a single tithi`() {
        // Amavasya = global tithi 30 → day 29.
        assertThat(nextTithiOccurrence(0L, windowDays = 60, maasa = null, tithis = setOf(30), source = source))
            .isEqualTo(29 * DAY)
    }

    @Test
    fun `matches either fortnight when the target set spans both`() {
        // Ekadashi = {11, 26}; the soonest is tithi 11 → day 10.
        assertThat(nextTithiOccurrence(0L, windowDays = 60, maasa = null, tithis = setOf(11, 26), source = source))
            .isEqualTo(10 * DAY)
    }

    @Test
    fun `honours the month filter, skipping the same tithi in other months`() {
        // Tithi 1 falls on day 0 (Chaitra) and day 30 (Vaishakha); pinned to Vaishakha → day 30.
        assertThat(nextTithiOccurrence(0L, windowDays = 60, maasa = "Vaishakha", tithis = setOf(1), source = source))
            .isEqualTo(30 * DAY)
    }

    @Test
    fun `matches on day zero when today already qualifies`() {
        assertThat(nextTithiOccurrence(0L, windowDays = 60, maasa = null, tithis = setOf(1), source = source))
            .isEqualTo(0L)
    }

    @Test
    fun `returns null when no matching day falls in the window`() {
        // Within five days the tithi only reaches 5, so Amavasya (30) is not found.
        assertThat(nextTithiOccurrence(0L, windowDays = 5, maasa = null, tithis = setOf(30), source = source))
            .isNull()
    }
}
