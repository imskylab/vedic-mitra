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

/**
 * How a given day stands for a given person — Tarabala and Chandrabala, reported rather than scored.
 *
 * **Mode: Compute (rule-transcribed), Cite.** The counting is arithmetic and exact; the *grading* —
 * which taras are favourable, which Chandrabala positions are strong — is a rule the tradition holds
 * rather than something derivable, and it is transcribed in [Bala]'s tables. Per
 * `docs/knowledge-standards.md` the two must not be presented at the same confidence, so this type
 * reports the tara and the position and leaves the verdict to be worded by the screen.
 *
 * This is the same pair of measures the muhurta scorer folds into a day's score, exposed without the
 * scoring. The ranked list needs one number; the day a reader opens needs to say what it is made of,
 * and for whom.
 */
data class PersonalStanding(
    val personId: String,
    val tara: Tara,
    val chandrabala: Chandrabala?,
)

/**
 * The day's Moon sign counted from a person's birth Moon sign.
 *
 * @property position 1..12, where 1 is the birth sign itself.
 * @property strength the grade the tradition gives that position.
 */
data class Chandrabala(
    val position: Int,
    val strength: Bala,
)

/**
 * How [dayNakshatraNumber] (1..27) and [dayMoonRasiIndex] (0..11) stand for [person].
 *
 * [dayMoonRasiIndex] is null when the day's Moon sign is not known, and Chandrabala is then omitted
 * rather than guessed — the same choice the scorer makes.
 */
fun personalStandingOn(
    person: MuhurtaPerson,
    dayNakshatraNumber: Int,
    dayMoonRasiIndex: Int?,
): PersonalStanding =
    PersonalStanding(
        personId = person.id,
        tara = taraBetween(dayNakshatraNumber, person.context.birthNakshatraNumber),
        chandrabala =
            dayMoonRasiIndex?.let {
                val position = chandraPosition(it, person.context.birthMoonRasiIndex)
                Chandrabala(position = position, strength = chandraStrength(position))
            },
    )
