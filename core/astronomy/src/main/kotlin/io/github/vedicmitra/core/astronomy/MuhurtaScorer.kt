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

/**
 * A five-step verdict for a candidate muhurta day, derived from its [DayMuhurtaScore.score].
 *
 * @property stars a 1..5 rating for display.
 * @property label a short human-readable verdict.
 */
enum class MuhurtaRating(
    val stars: Int,
    val label: String,
) {
    EXCELLENT(5, "Excellent"),
    GOOD(4, "Good"),
    FAIR(3, "Fair"),
    WEAK(2, "Weak"),
    AVOID(1, "Avoid"),
}

/**
 * One contributing factor in a day's muhurta score — the reasons shown to the user.
 *
 * @property favourable whether this factor helped (`true`) or hurt (`false`) the day.
 * @property text a short human-readable explanation.
 * @property personId which person this factor is about, for the factors that are about a person at
 *   all; `null` for the ones that are about the day itself. An **id**, never a name — the engine does
 *   not hold display copy (ADR 0021), and the screen joins the two into a sentence with a string
 *   resource so a translator can reorder it.
 */
data class MuhurtaReason(
    val favourable: Boolean,
    val text: String,
    val personId: String? = null,
)

/**
 * The overall suitability of a day for a chosen activity.
 *
 * @property score the combined score, 0..100.
 * @property rating the [MuhurtaRating] the [score] falls into.
 * @property reasons the favourable and unfavourable factors that produced the [score].
 */
data class DayMuhurtaScore(
    val score: Int,
    val rating: MuhurtaRating,
    val reasons: List<MuhurtaReason>,
)

/**
 * The personalisation key for muhurta scoring, taken from a person's natal chart. When supplied, the
 * scorer layers Tarabala (the day's nakshatra counted from the birth star) and Chandrabala (the day's
 * Moon sign counted from the birth Moon sign) on top of the general day score.
 *
 * @property birthNakshatraNumber the birth nakshatra, 1..27.
 * @property birthMoonRasiIndex the birth Moon sign, 0..11 (0 = Mesha).
 */
data class PersonalMuhurtaContext(
    val birthNakshatraNumber: Int,
    val birthMoonRasiIndex: Int,
)

/**
 * One person a muhurta is being chosen for: their birth chart key, and an [id] to attribute a reason
 * back to them.
 *
 * A wrapper rather than an id on [PersonalMuhurtaContext] itself, because the rashifal uses that type
 * for a single anonymous reading and has no use for an identity.
 *
 * @property id opaque and caller-chosen — the profile id, in practice. Never a name.
 */
data class MuhurtaPerson(
    val id: String,
    val context: PersonalMuhurtaContext,
)

/**
 * The day-plus-people inputs the scorer needs to add Tarabala and Chandrabala: whoever the day is
 * being chosen for, and the day's Moon sign ([dayMoonRasiIndex], 0..11) — the latter needed for
 * Chandrabala, and `null` when the day's Moon sign isn't known (Chandrabala is then skipped).
 *
 * [people] is empty for a general, unpersonalised ranking.
 */
data class DayPersonalisation(
    val people: List<MuhurtaPerson>,
    val dayMoonRasiIndex: Int?,
)

private const val BASE_SCORE = 50

/**
 * Scores a day's panchanga for [activity] from the general rules, then — when [personal] names
 * anyone — layers their Tarabala and Chandrabala on top, worst case first where there is more than
 * one of them. The nakshatra is weighted most heavily, then
 * the weekday and tithi, with universal doshas (Rikta/Amavasya tithi, the Vyatipata/Vaidhriti yogas,
 * and the Vishti/Bhadra karana) penalised regardless of activity. The result is clamped to 0..100 and
 * mapped to a [MuhurtaRating], with the contributing [reasons].
 */
internal fun scoreMuhurta(
    activity: MuhurtaActivity,
    tithi: Tithi,
    nakshatra: Nakshatra,
    vara: Vara,
    yoga: Yoga,
    karana: Karana,
    personal: DayPersonalisation? = null,
): DayMuhurtaScore {
    val rules = muhurtaRulesFor(activity)
    val reasons = mutableListOf<MuhurtaReason>()
    var score = BASE_SCORE

    when {
        nakshatra.number in rules.favorableNakshatras -> {
            score += 25
            reasons += MuhurtaReason(true, "Favourable nakshatra (${nakshatra.name})")
        }

        nakshatra.number in rules.unfavorableNakshatras -> {
            score -= 30
            reasons += MuhurtaReason(false, "Avoided nakshatra (${nakshatra.name})")
        }
    }

    if (vara in rules.favorableVaras) {
        score += 12
        reasons += MuhurtaReason(true, "Favourable weekday (${vara.displayName})")
    } else {
        score -= 8
        reasons += MuhurtaReason(false, "Weekday not ideal (${vara.displayName})")
    }

    when {
        tithi.number == AMAVASYA_TITHI -> {
            score -= 15
            reasons += MuhurtaReason(false, "Amavasya (new moon)")
        }

        tithi.number in RIKTA_TITHIS -> {
            score -= 15
            reasons += MuhurtaReason(false, "Rikta tithi (${tithi.name})")
        }

        tithi.number in rules.favorableTithis -> {
            score += 10
            reasons += MuhurtaReason(true, "Auspicious tithi (${tithi.name})")
        }
    }

    if (tithi.paksha == Paksha.SHUKLA) {
        score += 5
        reasons += MuhurtaReason(true, "Waxing (Shukla) fortnight")
    }

    if (yoga.number in INAUSPICIOUS_YOGAS) {
        score -= 15
        reasons += MuhurtaReason(false, "Inauspicious yoga (${yoga.name})")
    }

    if (karana.name == VISHTI_KARANA_NAME) {
        score -= 15
        reasons += MuhurtaReason(false, "Vishti (Bhadra) karana")
    }

    personalContributions(nakshatra.number, personal).forEach {
        score += it.delta
        reasons += it.reason
    }

    val clamped = score.coerceIn(0, 100)
    return DayMuhurtaScore(score = clamped, rating = ratingFor(clamped), reasons = reasons)
}

/** Maps a 0..100 [score] to its [MuhurtaRating]. */
private fun ratingFor(score: Int): MuhurtaRating =
    when {
        score >= 80 -> MuhurtaRating.EXCELLENT
        score >= 65 -> MuhurtaRating.GOOD
        score >= 50 -> MuhurtaRating.FAIR
        score >= 35 -> MuhurtaRating.WEAK
        else -> MuhurtaRating.AVOID
    }

/** One personal (Tarabala/Chandrabala) adjustment to the day score, with its explanation. */
private data class ScoreContribution(
    val delta: Int,
    val reason: MuhurtaReason,
)

/**
 * The Tarabala and Chandrabala adjustments for everyone the day is being chosen for, or empty when it
 * is being chosen for nobody in particular.
 *
 * **With more than one person, each factor is taken at its worst.** A day is favourable for a pair
 * only when it is favourable for each of them; a strong tara for one does not buy off a weak tara for
 * the other. That is a stated convention rather than a derived result — the tradition grades a tara
 * for *a* person, and how to weigh two people against each other is a judgement. Choosing to weigh
 * them would be asserting whose fortune matters more, which this app will not do without a source
 * for the weighting. See ADR 0023.
 *
 * Only the governing contribution is emitted, so a row can say *whose* tara pulled the day down
 * without listing everyone. The day screen shows all of them; ranking needs the binding one.
 */
private fun personalContributions(
    dayNakshatraNumber: Int,
    personal: DayPersonalisation?,
): List<ScoreContribution> {
    val people = personal?.people.orEmpty()
    if (people.isEmpty()) return emptyList()
    val moonRasi = personal?.dayMoonRasiIndex
    val tara = people.mapNotNull { tarabalaContribution(dayNakshatraNumber, it) }.minByOrNull { it.delta }
    val chandra =
        moonRasi?.let { rasi -> people.mapNotNull { chandrabalaContribution(rasi, it) } }
            ?.minByOrNull { it.delta }
    return listOfNotNull(tara, chandra)
}

/**
 * Tarabala: which of the nine taras the [dayNakshatra] is, counted from [person]'s birth star. The
 * tara counting and grading live in [taraBetween]; this maps the grade to a muhurta score delta.
 */
private fun tarabalaContribution(
    dayNakshatra: Int,
    person: MuhurtaPerson,
): ScoreContribution? {
    val tara = taraBetween(dayNakshatra, person.context.birthNakshatraNumber)
    return when (tara.strength) {
        Bala.STRONG -> ScoreContribution(15, MuhurtaReason(true, "Favourable tara (${tara.name})", person.id))
        Bala.WEAK -> ScoreContribution(-20, MuhurtaReason(false, "Weak tara (${tara.name})", person.id))
        Bala.NEUTRAL -> null
    }
}

/**
 * Chandrabala: the day's Moon sign [dayMoonRasi] counted from [person]'s birth Moon sign (both
 * 0..11). The position counting and grading live in [chandraPosition]/[chandraStrength]; this maps
 * the grade to a muhurta score delta.
 */
private fun chandrabalaContribution(
    dayMoonRasi: Int,
    person: MuhurtaPerson,
): ScoreContribution? {
    val position = chandraPosition(dayMoonRasi, person.context.birthMoonRasiIndex)
    val id = person.id
    return when (chandraStrength(position)) {
        Bala.STRONG -> ScoreContribution(10, MuhurtaReason(true, "Strong Chandrabala (position $position)", id))
        Bala.WEAK -> ScoreContribution(-12, MuhurtaReason(false, "Weak Chandrabala (position $position)", id))
        Bala.NEUTRAL -> null
    }
}
