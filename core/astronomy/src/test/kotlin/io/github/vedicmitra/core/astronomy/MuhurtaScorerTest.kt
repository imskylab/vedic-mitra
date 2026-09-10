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
    fun `a favourable tara and Chandrabala lift the personalised score above the general one`() {
        // Shanivara keeps the general score below 100 so the personal boost has headroom.
        val tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami")
        val nakshatra = Nakshatra(number = 4, name = "Rohini")
        val general =
            scoreMuhurta(
                MuhurtaActivity.SOWING,
                tithi,
                nakshatra,
                Vara.SHANIVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 2, name = "Bava"),
            )
        // Birth star Ashwini (1): day star Rohini (4) is the 4th tara (Kshema — favourable).
        // Birth Moon Mesha (0): day Moon Kanya (5) is the 6th position — a strong Chandrabala.
        val personalised =
            scoreMuhurta(
                MuhurtaActivity.SOWING,
                tithi,
                nakshatra,
                Vara.SHANIVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 2, name = "Bava"),
                personal =
                    DayPersonalisation(
                        people = listOf(personOf("a", birthNakshatra = 1, birthMoonRasi = 0)),
                        dayMoonRasiIndex = 5,
                    ),
            )

        assertThat(personalised.score).isGreaterThan(general.score)
        assertThat(personalised.reasons.any { it.favourable && it.text.contains("tara") }).isTrue()
        assertThat(personalised.reasons.any { it.favourable && it.text.contains("Chandrabala") }).isTrue()
    }

    @Test
    fun `an unfavourable tara lowers the personalised score`() {
        // Birth star Ashwini (1): day star Bharani (2) is the 2nd tara (Sampat — favourable), so use
        // Krittika (3) which is the 3rd tara (Vipat — unfavourable).
        val general =
            scoreMuhurta(
                MuhurtaActivity.NAMKARAN,
                Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                Nakshatra(number = 3, name = "Krittika"),
                Vara.GURUVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 2, name = "Bava"),
            )
        val personalised =
            scoreMuhurta(
                MuhurtaActivity.NAMKARAN,
                Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                Nakshatra(number = 3, name = "Krittika"),
                Vara.GURUVARA,
                Yoga(number = 1, name = "Vishkambha"),
                Karana(number = 2, name = "Bava"),
                personal =
                    DayPersonalisation(
                        people = listOf(personOf("a", birthNakshatra = 1, birthMoonRasi = 0)),
                        dayMoonRasiIndex = null,
                    ),
            )

        assertThat(personalised.score).isLessThan(general.score)
        assertThat(personalised.reasons.any { !it.favourable && it.text.contains("Weak tara") }).isTrue()
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

    @Test
    fun `two people take each factor at its worst`() {
        // Krittika (3) is the Vipat tara (weak) from Ashwini (1), and the Sampat tara (strong) from
        // Revati (27). Ranked for both, the weak one has to govern: a day is favourable for a pair
        // only when it is favourable for each.
        val strongOnly = scoreFor(listOf(personOf("ravi", birthNakshatra = 27, birthMoonRasi = 0)))
        val weakOnly = scoreFor(listOf(personOf("meera", birthNakshatra = 1, birthMoonRasi = 0)))
        val both =
            scoreFor(
                listOf(
                    personOf("ravi", birthNakshatra = 27, birthMoonRasi = 0),
                    personOf("meera", birthNakshatra = 1, birthMoonRasi = 0),
                ),
            )

        assertThat(strongOnly.score).isGreaterThan(weakOnly.score)
        assertThat(both.score).isEqualTo(weakOnly.score)
    }

    @Test
    fun `the governing reason names whose it is`() {
        val both =
            scoreFor(
                listOf(
                    personOf("ravi", birthNakshatra = 27, birthMoonRasi = 0),
                    personOf("meera", birthNakshatra = 1, birthMoonRasi = 0),
                ),
            )

        val tara = both.reasons.single { it.text.contains("tara") }
        assertThat(tara.favourable).isFalse()
        // Attributed by id, never by name -- the engine holds no display copy.
        assertThat(tara.personId).isEqualTo("meera")
    }

    @Test
    fun `one person scores exactly as it did before people were a list`() {
        // A guard on the refactor rather than on the rule: a single person must be untouched by the
        // combination logic, since every existing personalised ranking is a list of one.
        val alone = scoreFor(listOf(personOf("solo", birthNakshatra = 1, birthMoonRasi = 0)))
        val none = scoreFor(emptyList())

        assertThat(alone.score).isNotEqualTo(none.score)
        assertThat(alone.reasons.any { it.personId == "solo" }).isTrue()
    }

    @Test
    fun `nobody selected is the general ranking`() {
        val none = scoreFor(emptyList())

        assertThat(none.reasons.none { it.personId != null }).isTrue()
    }

    private fun scoreFor(people: List<MuhurtaPerson>): DayMuhurtaScore =
        scoreMuhurta(
            MuhurtaActivity.NAMKARAN,
            Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
            Nakshatra(number = 3, name = "Krittika"),
            Vara.GURUVARA,
            Yoga(number = 1, name = "Vishkambha"),
            Karana(number = 2, name = "Bava"),
            personal = people.takeIf { it.isNotEmpty() }?.let { DayPersonalisation(it, dayMoonRasiIndex = null) },
        )
}

/** A [MuhurtaPerson] with the two numbers the scorer actually reads. */
private fun personOf(
    id: String,
    birthNakshatra: Int,
    birthMoonRasi: Int,
): MuhurtaPerson =
    MuhurtaPerson(
        id = id,
        context =
            PersonalMuhurtaContext(
                birthNakshatraNumber = birthNakshatra,
                birthMoonRasiIndex = birthMoonRasi,
            ),
    )
