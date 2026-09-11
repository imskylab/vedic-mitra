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

import io.github.vedicmitra.core.common.model.ContentSource

/**
 * The electional (muhurta) rules for one activity: the panchanga limbs that favour it.
 *
 * **Mode: Compute (rule-transcribed), Cite.** The code can be shown to implement the rule; that the
 * rule is the one the tradition holds can only be *cited*. Hence [source], which has **no default** —
 * a new rule cannot be added without deciding where it came from.
 *
 * Nakshatra and tithi are matched by their 1-based numbers ([Nakshatra.number] 1..27, [Tithi.number]
 * 1..30 across both pakshas); karanas by [KaranaKind] rather than by the name printed for them. A
 * day's overall suitability is computed by `scoreMuhurta`.
 *
 * @property source where this rule set came from. [ContentSource.NotRecorded] is an admission, not a
 *   category — `ActivityMuhurtaRulesTest` pins how many rule sets may carry it, so the count can
 *   shrink but never grow.
 * @property favorableNakshatras nakshatras recommended for the activity.
 * @property favorableVaras weekdays that suit the activity.
 * @property favorableTithis tithis that suit the activity; defaults to the generally auspicious ones.
 * @property favorableKaranas karanas in which the texts direct this kind of work to be done. Empty
 *   for most activities, because most are not named in a karana verse.
 * @property unfavorableNakshatras nakshatras to specifically avoid for the activity, if any.
 */
data class ActivityMuhurtaRules(
    val source: ContentSource,
    val favorableNakshatras: Set<Int>,
    val favorableVaras: Set<Vara>,
    val favorableTithis: Set<Int> = AUSPICIOUS_TITHIS,
    val favorableKaranas: Set<KaranaKind> = emptySet(),
    val unfavorableNakshatras: Set<Int> = emptySet(),
)

// The gentle/benefic weekdays preferred for most auspicious beginnings (Mon, Wed, Thu, Fri).
// Tuesday, Saturday and Sunday are generally avoided and so are simply absent here.
internal val BENEFIC_VARAS: Set<Vara> =
    setOf(Vara.SOMAVARA, Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA)

// Generally auspicious tithis in either paksha (global numbering). Compare Brihat Samhita 99.2,
// which sorts the fifteen into Nanda (1, 6, 11), Bhadra (2, 7, 12), Vijaya (3, 8, 13), Rikta
// (4, 9, 14) and Poorna (5, 10, 15) and sets aside only the Rikta ones. This set is *narrower* than
// that -- it also omits 1, 6, 8 and 12 -- so it is not a transcription of that verse and is not cited
// as one. Widening it to match would move every ranking in the app, which is a decision of its own
// rather than a side effect of this one. Recorded in ADR 0025.
internal val AUSPICIOUS_TITHIS: Set<Int> =
    setOf(2, 3, 5, 7, 10, 11, 13, 15, 17, 18, 20, 22, 25, 26, 28)

// Rikta ("empty") tithis -- the 4th, 9th and 14th of each paksha -- avoided for auspicious starts.
// Brihat Samhita 99.2 names the same three: "the 4th, 9th, and 14th lunar days are known as Rikta".
internal val RIKTA_TITHIS: Set<Int> = setOf(4, 9, 14, 19, 24, 29)

// Amavasya (new moon), global tithi 30 -- avoided for most auspicious beginnings.
internal const val AMAVASYA_TITHI: Int = 30

// Inauspicious yogas by 1-based number: Vyatipata (17) and Vaidhriti (27).
internal val INAUSPICIOUS_YOGAS: Set<Int> = setOf(17, 27)

// A broad set of generally-auspicious nakshatras, used for activities without a specific rule yet.
private val GENERALLY_AUSPICIOUS_NAKSHATRAS: Set<Int> =
    setOf(1, 4, 5, 7, 8, 12, 13, 14, 15, 17, 21, 22, 23, 24, 26, 27)

/**
 * The fallback: what every activity with no rules of its own is ranked by.
 *
 * Its [ContentSource.NotRecorded] is the honest answer, and [hasActivityRules] is why it matters —
 * the results screen says when a ranking came from here rather than from something specific to what
 * the reader asked about.
 */
private val DEFAULT_RULES =
    ActivityMuhurtaRules(
        source = ContentSource.NotRecorded,
        favorableNakshatras = GENERALLY_AUSPICIOUS_NAKSHATRAS,
        favorableVaras = BENEFIC_VARAS,
    )

// "In a Gara Karana lands shall be tilled, seeds sown, houses built and the like" -- Brihat Samhita
// 99.7. Shared by the two activities the verse names between them.
private val TILLING_AND_SOWING =
    ActivityMuhurtaRules(
        source = ContentSource.Text(work = "Brihat Samhita", locus = "99.7"),
        favorableNakshatras = GENERALLY_AUSPICIOUS_NAKSHATRAS,
        favorableVaras = BENEFIC_VARAS,
        favorableKaranas = setOf(KaranaKind.GARA),
    )

// "In a Vanik Karana, works of a fixed nature shall be done as well as dealings with merchants"
// -- Brihat Samhita 99.7.
private val TRADE =
    ActivityMuhurtaRules(
        source = ContentSource.Text(work = "Brihat Samhita", locus = "99.7"),
        favorableNakshatras = GENERALLY_AUSPICIOUS_NAKSHATRAS,
        favorableVaras = BENEFIC_VARAS,
        favorableKaranas = setOf(KaranaKind.VANIJA),
    )

/**
 * Every activity with rules of its own.
 *
 * A map rather than a `when`, because it is a table and grows like one — see
 * [#247](https://github.com/imskylab/vedic-mitra/issues/247), of which this is the first instalment.
 *
 * ## Two kinds of entry, and the difference matters
 *
 * The **nakshatra sets carry no source.** They were here before the rule about citing arrived, and no
 * public-domain text has been found that gives lists of that shape; they are left as they were,
 * declaring `NotRecorded`, rather than dressed with a plausible citation. See ADR 0025.
 *
 * The **karana sets are cited**, to the Brihat Samhita's chapter on lunar days and half lunar days.
 * Its verses name particular kinds of work for particular karanas, which is the uncommon case of a
 * classical text saying something activity-specific that this engine can already compute. Read in
 * N. Chidambaram Iyer's 1884 translation, which is public domain and prints the chapter as Part II
 * chapter 52.
 */
private val RULES: Map<MuhurtaActivity, ActivityMuhurtaRules> =
    mapOf(
        MuhurtaActivity.GRIHA_PRAVESH to
            ActivityMuhurtaRules(
                source = ContentSource.NotRecorded,
                favorableNakshatras = setOf(4, 5, 8, 12, 14, 15, 17, 21, 23, 24, 26, 27),
                favorableVaras = BENEFIC_VARAS,
            ),
        MuhurtaActivity.VIVAH to
            ActivityMuhurtaRules(
                source = ContentSource.NotRecorded,
                favorableNakshatras = setOf(4, 5, 10, 12, 13, 15, 17, 19, 21, 26, 27),
                favorableVaras = BENEFIC_VARAS,
            ),
        MuhurtaActivity.NAMKARAN to
            ActivityMuhurtaRules(
                source = ContentSource.NotRecorded,
                favorableNakshatras = setOf(1, 4, 5, 7, 8, 13, 14, 15, 17, 22, 23, 24, 27),
                favorableVaras = BENEFIC_VARAS,
            ),
        MuhurtaActivity.VEHICLE_PURCHASE to
            ActivityMuhurtaRules(
                source = ContentSource.NotRecorded,
                favorableNakshatras = setOf(1, 7, 8, 13, 14, 15, 17, 22, 23, 24, 27),
                favorableVaras = BENEFIC_VARAS,
            ),
        // Two verses name the building of a house: Taitila at 99.6, Gara at 99.7. Bhoomi Poojan is
        // the ground-breaking for one, so both apply. Its nakshatras remain unsourced.
        MuhurtaActivity.BHOOMI_POOJAN to
            ActivityMuhurtaRules(
                source = ContentSource.Text(work = "Brihat Samhita", locus = "99.6-7"),
                favorableNakshatras = setOf(4, 5, 12, 14, 17, 21, 22, 23, 24, 26),
                favorableVaras = BENEFIC_VARAS,
                favorableKaranas = setOf(KaranaKind.TAITILA, KaranaKind.GARA),
            ),
        MuhurtaActivity.SOWING to TILLING_AND_SOWING,
        MuhurtaActivity.GARDENING to TILLING_AND_SOWING,
        MuhurtaActivity.SHOP_OPENING to TRADE,
        MuhurtaActivity.BUSINESS_INAUGURATION to TRADE,
        MuhurtaActivity.PRODUCT_SALE to TRADE,
        // "In a Sakuni Karana a person shall ... take medicine" -- 99.8. Only Shakuni is taken: Bava
        // and Kimstughna speak of health and comfort in general, which is not the same as naming the
        // act, and a rule that cannot be defended is worse than no rule at all.
        MuhurtaActivity.AUSHADHI_SEVAN to
            ActivityMuhurtaRules(
                source = ContentSource.Text(work = "Brihat Samhita", locus = "99.8"),
                favorableNakshatras = GENERALLY_AUSPICIOUS_NAKSHATRAS,
                favorableVaras = BENEFIC_VARAS,
                favorableKaranas = setOf(KaranaKind.SHAKUNI),
            ),
        // "In a Chatushpada Karana a person shall do deeds connected with cows, Brahmins, the Pitris
        // and the king" -- 99.8.
        MuhurtaActivity.LIVESTOCK_PURCHASE to
            ActivityMuhurtaRules(
                source = ContentSource.Text(work = "Brihat Samhita", locus = "99.8"),
                favorableNakshatras = GENERALLY_AUSPICIOUS_NAKSHATRAS,
                favorableVaras = BENEFIC_VARAS,
                favorableKaranas = setOf(KaranaKind.CHATUSHPADA),
            ),
    )

/**
 * The electional rules for [activity], falling back to a generally-auspicious default for the
 * activities that have none of their own.
 */
internal fun muhurtaRulesFor(activity: MuhurtaActivity): ActivityMuhurtaRules = RULES[activity] ?: DEFAULT_RULES

/**
 * Whether [activity] has rules of its own, or is ranked by the general ones.
 *
 * Public because a screen has to be able to say which of the two it is showing. The picker offers 31
 * activities and the engine distinguishes a minority of them; an interface that offers a choice the
 * engine does not make is making a claim it cannot keep.
 */
fun hasActivityRules(activity: MuhurtaActivity): Boolean = activity in RULES

/** Where the rules used for [activity] came from, for display beside the ranking. */
fun muhurtaRuleSourceFor(activity: MuhurtaActivity): ContentSource = muhurtaRulesFor(activity).source
