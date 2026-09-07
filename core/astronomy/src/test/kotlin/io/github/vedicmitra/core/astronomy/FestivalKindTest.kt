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
import org.junit.Test

/**
 * [FestivalKind.id] is what durable things refer to a festival by, so these are data-format tests
 * rather than style ones. They mirror `MuhurtaKindTest`, which guards the same property for the
 * day's windows.
 *
 * This enum replaced two `when` blocks that mapped observances *by name* — one forward, one back.
 * The tests at the bottom are the ones those blocks needed and never had: nothing checked that the
 * two agreed.
 */
class FestivalKindTest {
    @Test
    fun `no id is a display name`() {
        // The whole failure mode in one assertion: an id equal to its label is a label, and will be
        // "corrected" by the next person who improves the wording. A glossary keyed on it then
        // stops matching, silently, and the app says it knows nothing about Diwali.
        FestivalKind.entries.forEach { kind ->
            assertWithMessage("${kind.name} id").that(kind.id).isNotEqualTo(kind.label)
            assertWithMessage("${kind.name} id should be a lowercase slug")
                .that(kind.id)
                .matches("[a-z][a-z0-9-]*")
        }
    }

    @Test
    fun `ids and labels are each unique`() {
        // Two kinds sharing an id would merge two festivals' blurbs into one.
        assertThat(FestivalKind.entries.map { it.id }).containsNoDuplicates()
        assertThat(FestivalKind.entries.map { it.label }).containsNoDuplicates()
    }

    @Test
    fun `the recurring observances are exactly these, on exactly these tithis`() {
        // Pinned deliberately. Krishna tithis are 15 + their number in the fortnight, so Sankashti
        // Chaturthi (Krishna Chaturthi) is 19 and Masik Shivaratri (Krishna Chaturdashi) is 29.
        val recurring = FestivalKind.entries.filter { it.isRecurring }.associate { it.name to it.monthlyTithis }

        assertThat(recurring).containsExactlyEntriesIn(
            mapOf(
                "VINAYAKA_CHATURTHI" to setOf(4),
                "EKADASHI" to setOf(11, 26),
                "PRADOSH" to setOf(13, 28),
                "PURNIMA" to setOf(15),
                "SANKASHTI_CHATURTHI" to setOf(19),
                "MASIK_SHIVARATRI" to setOf(29),
                "AMAVASYA" to setOf(30),
            ),
        )
    }

    @Test
    fun `a named festival is not a recurring observance`() {
        listOf(FestivalKind.DIWALI, FestivalKind.HOLI, FestivalKind.MAKARA_SANKRANTI).forEach {
            assertWithMessage("${it.name}").that(it.monthlyTithis).isNull()
            assertWithMessage("${it.name}").that(it.isRecurring).isFalse()
        }
    }

    @Test
    fun `every observance tithi resolves back to the observance that claims it`() {
        // The property the old pair of `when` blocks could not guarantee: the forward and reverse
        // mappings were separate code, kept in step by hand.
        FestivalKind.entries.filter { it.isRecurring }.forEach { kind ->
            kind.monthlyTithis.orEmpty().forEach { tithi ->
                assertWithMessage("tithi $tithi should resolve to ${kind.name}")
                    .that(FestivalKind.observanceAt(tithi))
                    .isEqualTo(kind)
            }
        }
    }

    @Test
    fun `no two observances claim the same tithi`() {
        // observanceAt takes the first match, so an overlap would silently hide one of them.
        val claimed = FestivalKind.entries.flatMap { it.monthlyTithis.orEmpty() }

        assertThat(claimed).containsNoDuplicates()
    }

    @Test
    fun `an ordinary tithi has no observance`() {
        listOf(1, 5, 12, 20, 25).forEach {
            assertWithMessage("tithi $it").that(FestivalKind.observanceAt(it)).isNull()
        }
    }
}
