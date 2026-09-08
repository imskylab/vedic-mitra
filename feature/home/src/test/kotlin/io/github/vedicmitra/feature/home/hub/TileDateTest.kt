/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home.hub

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * The date drawn on the Today's Panchanga tile.
 *
 * Every case pins an explicit locale. The function defaults to the reader's, which is the point of
 * it, but a test that inherited the machine's would pass or fail depending on where it ran.
 */
class TileDateTest {
    private fun on(
        year: Int,
        month: Int,
        day: Int,
    ) = tileDate(LocalDate.of(year, month, day), Locale.ENGLISH)

    @Test
    fun `a single-digit day is not padded`() {
        // "08" in a 38dp box wastes half the width on a zero that says nothing.
        val date = on(2026, 9, 8)

        assertThat(date.day).isEqualTo("8")
        assertThat(date.month).isEqualTo("SEP")
    }

    @Test
    fun `a two-digit day is the whole number`() {
        val date = on(2026, 9, 27)

        assertThat(date.day).isEqualTo("27")
        assertThat(date.month).isEqualTo("SEP")
    }

    @Test
    fun `the month is abbreviated and uppercased`() {
        // Uppercase is a drawing decision -- it sits under a large numeral and needs the weight.
        // Asserted here rather than left to the composable so it cannot drift.
        assertThat(on(2026, 9, 8).month).isEqualTo(on(2026, 9, 8).month.uppercase(Locale.ENGLISH))
        assertThat(on(2026, 2, 1).month).isEqualTo("FEB")
    }

    @Test
    fun `the turn of the year is two different months`() {
        val eve = on(2026, 12, 31)
        val day = on(2027, 1, 1)

        assertThat(eve.day).isEqualTo("31")
        assertThat(eve.month).isEqualTo("DEC")
        assertThat(day.day).isEqualTo("1")
        assertThat(day.month).isEqualTo("JAN")
    }

    @Test
    fun `what a screen reader hears is a date, not the two drawn lines`() {
        // "8" and "SEP" read aloud separately are two loose nodes, and a capitalised abbreviation is
        // a coin-flip between "sep" and the letters spelled out.
        assertThat(on(2026, 9, 8).spoken).isEqualTo("8 September")
    }

    @Test
    fun `the month follows the locale it is asked for`() {
        // The tile localizes through the formatter rather than through strings.xml, so this is the
        // thing that has to keep working when #190 lands.
        val french = tileDate(LocalDate.of(2026, 9, 8), Locale.FRENCH)

        assertThat(french.month).isNotEqualTo("SEP")
        assertThat(french.day).isEqualTo("8")
    }
}
