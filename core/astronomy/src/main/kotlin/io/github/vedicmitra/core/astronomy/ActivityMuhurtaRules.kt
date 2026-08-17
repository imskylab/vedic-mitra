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
 * The electional (muhurta) rules for one activity: the panchanga limbs that favour it. These are the
 * standard, widely-taught classical preferences (nakshatra is the primary factor, with the weekday
 * and tithi as supporting ones); empirical refinements are applied separately, outside the app.
 *
 * Nakshatra and tithi are matched by their 1-based numbers ([Nakshatra.number] 1..27, [Tithi.number]
 * 1..30 across both pakshas). A day's overall suitability is computed by `scoreMuhurta`.
 *
 * @property favorableNakshatras nakshatras classically recommended for the activity.
 * @property favorableVaras weekdays that suit the activity.
 * @property favorableTithis tithis that suit the activity; defaults to the generally auspicious ones.
 * @property unfavorableNakshatras nakshatras to specifically avoid for the activity, if any.
 */
data class ActivityMuhurtaRules(
    val favorableNakshatras: Set<Int>,
    val favorableVaras: Set<Vara>,
    val favorableTithis: Set<Int> = AUSPICIOUS_TITHIS,
    val unfavorableNakshatras: Set<Int> = emptySet(),
)

// The gentle/benefic weekdays preferred for most auspicious beginnings (Mon, Wed, Thu, Fri).
// Tuesday, Saturday and Sunday are generally avoided and so are simply absent here.
internal val BENEFIC_VARAS: Set<Vara> =
    setOf(Vara.SOMAVARA, Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA)

// Generally auspicious tithis in either paksha (global numbering): the Nanda/Bhadra/Jaya/Purna
// tithis 2,3,5,7,10,11,13 in each fortnight, plus Purnima (15). Rikta and Amavasya are excluded.
internal val AUSPICIOUS_TITHIS: Set<Int> =
    setOf(2, 3, 5, 7, 10, 11, 13, 15, 17, 18, 20, 22, 25, 26, 28)

// Rikta ("empty") tithis — the 4th, 9th and 14th of each paksha — avoided for auspicious starts.
internal val RIKTA_TITHIS: Set<Int> = setOf(4, 9, 14, 19, 24, 29)

// Amavasya (new moon), global tithi 30 — avoided for most auspicious beginnings.
internal const val AMAVASYA_TITHI: Int = 30

// Inauspicious yogas by 1-based number: Vyatipata (17) and Vaidhriti (27).
internal val INAUSPICIOUS_YOGAS: Set<Int> = setOf(17, 27)

// The karana to avoid — Vishti (also called Bhadra) — matched by name.
internal const val VISHTI_KARANA_NAME: String = "Vishti"

// A broad set of generally-auspicious nakshatras, used for activities without a specific rule yet.
private val GENERALLY_AUSPICIOUS_NAKSHATRAS: Set<Int> =
    setOf(1, 4, 5, 7, 8, 12, 13, 14, 15, 17, 21, 22, 23, 24, 26, 27)

private val DEFAULT_RULES =
    ActivityMuhurtaRules(favorableNakshatras = GENERALLY_AUSPICIOUS_NAKSHATRAS, favorableVaras = BENEFIC_VARAS)

/**
 * The electional rules for [activity]. A refined table covers the first activities; the rest fall
 * back to a sensible generally-auspicious default until their own rules are added.
 */
internal fun muhurtaRulesFor(activity: MuhurtaActivity): ActivityMuhurtaRules =
    when (activity) {
        MuhurtaActivity.GRIHA_PRAVESH ->
            ActivityMuhurtaRules(
                favorableNakshatras = setOf(4, 5, 8, 12, 14, 15, 17, 21, 23, 24, 26, 27),
                favorableVaras = BENEFIC_VARAS,
            )

        MuhurtaActivity.VIVAH ->
            ActivityMuhurtaRules(
                favorableNakshatras = setOf(4, 5, 10, 12, 13, 15, 17, 19, 21, 26, 27),
                favorableVaras = BENEFIC_VARAS,
            )

        MuhurtaActivity.NAMKARAN ->
            ActivityMuhurtaRules(
                favorableNakshatras = setOf(1, 4, 5, 7, 8, 13, 14, 15, 17, 22, 23, 24, 27),
                favorableVaras = BENEFIC_VARAS,
            )

        MuhurtaActivity.VEHICLE_PURCHASE ->
            ActivityMuhurtaRules(
                favorableNakshatras = setOf(1, 7, 8, 13, 14, 15, 17, 22, 23, 24, 27),
                favorableVaras = BENEFIC_VARAS,
            )

        MuhurtaActivity.BHOOMI_POOJAN ->
            ActivityMuhurtaRules(
                favorableNakshatras = setOf(4, 5, 12, 14, 17, 21, 22, 23, 24, 26),
                favorableVaras = BENEFIC_VARAS,
            )

        else -> DEFAULT_RULES
    }
