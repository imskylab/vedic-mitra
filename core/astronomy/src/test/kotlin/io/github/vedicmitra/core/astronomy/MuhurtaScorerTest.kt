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

class MuhurtaScorerTest {
    @Test
    fun `an ideal Griha Pravesh day scores Excellent`() {
        val score =
            scoreMuhurta(
                activity = MuhurtaActivity.GRIHA_PRAVESH,
                tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                nakshatra = Nakshatra(number = 4, name = "Rohini"),
                vara = Vara.GURUVARA,
                yoga = Yoga(number = 1, name = "Vishkambha"),
                karana = Karana(number = 2, name = "Bava"),
            )

        assertThat(score.score).isEqualTo(100)
        assertThat(score.rating).isEqualTo(MuhurtaRating.EXCELLENT)
        assertThat(score.reasons.map { it.text }).contains("Favourable nakshatra (Rohini)")
        assertThat(score.reasons.all { it.favourable }).isTrue()
    }

    @Test
    fun `a Rikta Vishti Vyatipata day scores Avoid`() {
        val score =
            scoreMuhurta(
                activity = MuhurtaActivity.GRIHA_PRAVESH,
                // Krishna Chaturthi is global tithi 19 — a Rikta tithi.
                tithi = Tithi(number = 19, paksha = Paksha.KRISHNA, name = "Chaturthi"),
                nakshatra = Nakshatra(number = 2, name = "Bharani"),
                vara = Vara.MANGALAVARA,
                yoga = Yoga(number = 17, name = "Vyatipata"),
                karana = Karana(number = 7, name = "Vishti"),
            )

        assertThat(score.rating).isEqualTo(MuhurtaRating.AVOID)
        assertThat(score.reasons.any { !it.favourable && it.text.contains("Rikta") }).isTrue()
        assertThat(score.reasons.any { !it.favourable && it.text.contains("Vishti") }).isTrue()
        assertThat(score.reasons.any { !it.favourable && it.text.contains("Vyatipata") }).isTrue()
    }

    @Test
    fun `universal doshas penalise even a default-rules activity`() {
        val clean =
            scoreMuhurta(
                MuhurtaActivity.SOWING,
                Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                Nakshatra(number = 4, name = "Rohini"),
                Vara.GURUVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 2, name = "Bava"),
            )
        val withVishti =
            scoreMuhurta(
                MuhurtaActivity.SOWING,
                Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                Nakshatra(number = 4, name = "Rohini"),
                Vara.GURUVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 7, name = "Vishti"),
            )

        assertThat(withVishti.score).isLessThan(clean.score)
    }

    @Test
    fun `a weekday outside the benefic set is flagged`() {
        val score =
            scoreMuhurta(
                MuhurtaActivity.GRIHA_PRAVESH,
                Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                Nakshatra(number = 4, name = "Rohini"),
                Vara.SHANIVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 2, name = "Bava"),
            )

        assertThat(score.reasons.any { !it.favourable && it.text.contains("Weekday not ideal") }).isTrue()
    }

    @Test
    fun `score stays within zero and one hundred`() {
        val worst =
            scoreMuhurta(
                MuhurtaActivity.GRIHA_PRAVESH,
                Tithi(number = 30, paksha = Paksha.KRISHNA, name = "Amavasya"),
                Nakshatra(number = 2, name = "Bharani"),
                Vara.MANGALAVARA,
                Yoga(number = 17, name = "Vyatipata"),
                Karana(number = 7, name = "Vishti"),
            )

        assertThat(worst.score).isAtLeast(0)
        assertThat(worst.score).isAtMost(100)
    }
}
