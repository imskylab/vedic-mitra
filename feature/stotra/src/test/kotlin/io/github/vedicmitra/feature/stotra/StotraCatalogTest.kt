/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.stotra

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek

class StotraCatalogTest {
    @Test
    fun `the library is broad and has unique ids`() {
        assertThat(StotraCatalog.all.size).isAtLeast(20)
        val ids = StotraCatalog.all.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `every stotra has all its text fields`() {
        StotraCatalog.all.forEach { stotra ->
            assertThat(stotra.title).isNotEmpty()
            assertThat(stotra.deity).isNotEmpty()
            assertThat(stotra.devanagari).isNotEmpty()
            assertThat(stotra.transliteration).isNotEmpty()
            assertThat(stotra.significance).isNotEmpty()
        }
    }

    @Test
    fun `each verse and its transliteration have the same number of lines`() {
        StotraCatalog.all.forEach { stotra ->
            val verseLines = stotra.devanagari.lines().size
            val translitLines = stotra.transliteration.lines().size
            assertThat(translitLines).isEqualTo(verseLines)
        }
    }

    @Test
    fun `every weekday resolves to a catalogued stotra`() {
        DayOfWeek.entries.forEach { day ->
            val stotra = StotraCatalog.forWeekday(day)
            assertThat(StotraCatalog.byId(stotra.id)).isEqualTo(stotra)
        }
    }

    @Test
    fun `grouping by deity covers every stotra`() {
        assertThat(StotraCatalog.byDeity.values.sumOf { it.size }).isEqualTo(StotraCatalog.all.size)
    }
}
