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
 */
data class MuhurtaReason(
    val favourable: Boolean,
    val text: String,
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
 * The day-plus-person inputs the scorer needs to add Tarabala and Chandrabala: the [person]'s birth
 * chart key and the day's Moon sign ([dayMoonRasiIndex], 0..11) — the latter needed for Chandrabala,
 * and `null` when the day's Moon sign isn't known (Chandrabala is then skipped).
 */
data class DayPersonalisation(
    val person: PersonalMuhurtaContext,
    val dayMoonRasiIndex: Int?,
)

private const val BASE_SCORE = 50

/**
 * Scores a day's panchanga for [activity] from the general rules, then — when [personal] is supplied —
 * layers that person's Tarabala and Chandrabala on top. The nakshatra is weighted most heavily, then
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

/** The Tarabala and Chandrabala adjustments for [personal], or empty when no personalisation is given. */
private fun personalContributions(
    dayNakshatraNumber: Int,
    personal: DayPersonalisation?,
): List<ScoreContribution> {
    if (personal == null) return emptyList()
    val person = personal.person
    return listOfNotNull(
        tarabalaContribution(dayNakshatraNumber, person.birthNakshatraNumber),
        personal.dayMoonRasiIndex?.let { chandrabalaContribution(it, person.birthMoonRasiIndex) },
    )
}

// The nine taras, in order, counted from the birth star. Favourable: Sampat, Kshema, Sadhaka, Mitra,
// Ati-Mitra; unfavourable: Vipat, Pratyari, Vadha; Janma (the birth star itself) is neutral.
private val TARA_NAMES =
    listOf("Janma", "Sampat", "Vipat", "Kshema", "Pratyari", "Sadhaka", "Vadha", "Mitra", "Ati-Mitra")
private val FAVOURABLE_TARAS = setOf(2, 4, 6, 8, 9)
private val UNFAVOURABLE_TARAS = setOf(3, 5, 7)

/** Tarabala: which of the nine taras the [dayNakshatra] is, counted from [birthNakshatra] (both 1..27). */
private fun tarabalaContribution(
    dayNakshatra: Int,
    birthNakshatra: Int,
): ScoreContribution? {
    val count = ((dayNakshatra - birthNakshatra + 27) % 27) + 1
    val tara = ((count - 1) % 9) + 1
    val name = TARA_NAMES[tara - 1]
    return when (tara) {
        in FAVOURABLE_TARAS -> ScoreContribution(15, MuhurtaReason(true, "Favourable tara ($name)"))
        in UNFAVOURABLE_TARAS -> ScoreContribution(-20, MuhurtaReason(false, "Weak tara ($name)"))
        else -> null
    }
}

// Chandrabala positions (1..12) of the day's Moon sign counted from the birth Moon sign. The 1, 3, 6,
// 7, 10 and 11 positions are strong; 4, 8 and 12 are weak; the rest are neutral.
private val FAVOURABLE_CHANDRA = setOf(1, 3, 6, 7, 10, 11)
private val WEAK_CHANDRA = setOf(4, 8, 12)

/** Chandrabala: the day's Moon sign [dayMoonRasi] counted from the birth Moon sign [birthMoonRasi] (0..11). */
private fun chandrabalaContribution(
    dayMoonRasi: Int,
    birthMoonRasi: Int,
): ScoreContribution? {
    val position = ((dayMoonRasi - birthMoonRasi + 12) % 12) + 1
    return when (position) {
        in FAVOURABLE_CHANDRA -> ScoreContribution(10, MuhurtaReason(true, "Strong Chandrabala (position $position)"))
        in WEAK_CHANDRA -> ScoreContribution(-12, MuhurtaReason(false, "Weak Chandrabala (position $position)"))
        else -> null
    }
}
