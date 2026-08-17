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

private const val BASE_SCORE = 50

/**
 * Scores a day's panchanga for [activity] from the general (non-personalised) rules. The nakshatra
 * is weighted most heavily, then the weekday and tithi, with universal doshas (Rikta/Amavasya tithi,
 * the Vyatipata/Vaidhriti yogas, and the Vishti/Bhadra karana) penalised regardless of activity.
 * The result is clamped to 0..100 and mapped to a [MuhurtaRating], with the contributing [reasons].
 */
internal fun scoreMuhurta(
    activity: MuhurtaActivity,
    tithi: Tithi,
    nakshatra: Nakshatra,
    vara: Vara,
    yoga: Yoga,
    karana: Karana,
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
