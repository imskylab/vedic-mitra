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
 * Mangal dosha — the affliction of Mars, also called Kuja dosha, Manglik, and Chevvai dosham.
 *
 * In practice this is the *first* question asked of a proposed match across much of India, which is
 * why it is computed and shown rather than left out as folklore. What the app can honestly do is
 * state a classical rule, apply it exactly, and show its working — so a reader who follows a
 * different tradition can see which placement produced the verdict and discount it themselves.
 *
 * ## The rule this app follows
 *
 * Mars in the **1st, 2nd, 4th, 7th, 8th or 12th** house, counted separately from **the lagna, the
 * Moon and Venus**.
 *
 * Both halves are convention choices, and authorities differ on both:
 *
 * - **The houses.** The classical verse names the lagna, the 4th, 7th, 8th and 12th. The 2nd is a
 *   South Indian addition — it is the house of family and speech, and Chevvai dosham counts it —
 *   while some modern sources drop the 1st but keep the 2nd. This app takes the **union**, because
 *   [MangalDosha.triggers] reports each placement separately: a reader following the stricter verse
 *   can discount a 2nd-house trigger, which they could not do if everything had been merged into a
 *   single yes or no.
 * - **The reference points.** The oldest statement counts from the lagna alone. Counting also from
 *   the Moon and from Venus is later and now near-universal: Venus signifies the spouse, so Mars
 *   afflicting Venus is the same idea applied to the significator rather than to the house.
 *
 * ## Cancellation (parihara)
 *
 * **A dosha computed without its cancellations is worse than no dosha at all.** Most charts trigger
 * something on the list above, so a bare "Manglik" on nearly everyone is alarming and useless. Two
 * kinds of parihara apply, and they are not interchangeable:
 *
 * - **General** — about Mars's own condition (its sign, or Jupiter's influence on it). These answer
 *   the affliction wherever it arises, so they clear every trigger at once.
 * - **Per-trigger** — the house-and-sign rules, which lift the affliction of *one* house only. Mars
 *   in the 7th in Karka is answered; the same Mars in the 8th from the Moon is not. Applying one of
 *   these to the whole chart would clear a dosha that still stands.
 *
 * So the dosha stands when any trigger remains unanswered. The one rule deliberately omitted is the
 * claim that the dosha lapses with age: widely repeated, no classical basis, and stating it would be
 * inventing reassurance.
 *
 * @property triggers every placement raising the dosha, each carrying its own parihara if one
 *   applies. Empty means Mars falls nowhere that raises it.
 * @property cancellations general parihara, in plain words. Non-empty answers every trigger.
 */
data class MangalDosha(
    val triggers: List<MangalTrigger>,
    val cancellations: List<String>,
) {
    /** Whether Mars sits anywhere that raises the dosha, before any parihara is considered. */
    val afflicted: Boolean get() = triggers.isNotEmpty()

    /** Whether the dosha stands: raised, and at least one trigger left unanswered. */
    val present: Boolean
        get() = afflicted && cancellations.isEmpty() && triggers.any { !it.cancelled }
}

/**
 * One placement raising the dosha.
 *
 * @property reference what the house was counted from.
 * @property house the house Mars occupies from it, 1..12.
 * @property cancellation the house-and-sign parihara lifting *this* trigger, or `null` if none does.
 */
data class MangalTrigger(
    val reference: MangalReference,
    val house: Int,
    val cancellation: String? = null,
) {
    /** Whether a per-trigger parihara answers this placement. */
    val cancelled: Boolean get() = cancellation != null

    /** "Mars in the 8th from the Moon" — the working, in a phrase. */
    val description: String get() = "Mars in the ${ordinal(house)} from ${reference.displayName}"
}

/** What a Mangal dosha house is counted from. */
enum class MangalReference(
    val displayName: String,
) {
    LAGNA("the lagna"),
    CHANDRA("the Moon"),
    SHUKRA("Venus"),
}

/** Mangal dosha in [chart], with its working and any parihara. */
fun mangalDoshaOf(chart: NatalChart): MangalDosha {
    val byGraha = chart.grahas.associateBy { it.graha }
    val mars = byGraha[Graha.MANGALA]
    val moon = byGraha[Graha.MOON]
    val venus = byGraha[Graha.SHUKRA]
    // The lightweight charts test fixtures build can omit grahas; a real chart never does.
    if (mars == null || moon == null || venus == null) return NO_DOSHA
    val marsSign = mars.rasi.index

    val references =
        listOf(
            MangalReference.LAGNA to chart.lagna.rasi.index,
            MangalReference.CHANDRA to moon.rasi.index,
            MangalReference.SHUKRA to venus.rasi.index,
        )
    val triggers =
        references.mapNotNull { (reference, fromIndex) ->
            val house = houseFrom(fromIndex, marsSign)
            if (house !in DOSHA_HOUSES) {
                null
            } else {
                MangalTrigger(
                    reference = reference,
                    house = house,
                    cancellation = houseParihara(house, mars),
                )
            }
        }
    return if (triggers.isEmpty()) {
        NO_DOSHA
    } else {
        MangalDosha(triggers = triggers, cancellations = generalParihara(chart, mars))
    }
}

/**
 * Whether a match cancels the dosha between the two charts.
 *
 * When both partners carry it, the affliction is classically held to answer itself — the objection
 * to a Manglik marrying is that the partner suffers for it, which does not arise when both do. This
 * is the most widely applied parihara of all, and the reason the question is properly asked of the
 * pair rather than of one chart alone.
 */
fun mangalDoshaCancelsBetween(
    groom: MangalDosha,
    bride: MangalDosha,
): Boolean = groom.present && bride.present

private val NO_DOSHA = MangalDosha(triggers = emptyList(), cancellations = emptyList())

/** Houses raising the dosha — see the KDoc above on why this is the union of two conventions. */
private val DOSHA_HOUSES = setOf(1, 2, 4, 7, 8, 12)

/** Simha and Kumbha — the fixed signs of the Sun and Saturn, where Mars is held to do no harm. */
private val MARS_HARMLESS_SIGNS = setOf(4, 10)

/**
 * Signs in which Mars is held harmless *in a particular dosha house* — the standard house-and-sign
 * parihara list. The 1st house has no entry, which is why only the general rules can answer it.
 */
private val HOUSE_SIGN_PARIHARA: Map<Int, Set<Int>> =
    mapOf(
        2 to setOf(2, 5), // Mithuna, Kanya
        4 to setOf(0, 7), // Mesha, Vrischika
        7 to setOf(3, 9), // Karka, Makara
        8 to setOf(8, 11), // Dhanu, Meena
        12 to setOf(1, 6), // Vrishabha, Tula
    )

/** Parihara from Mars's own condition, answering the affliction wherever it arises. */
private fun generalParihara(
    chart: NatalChart,
    mars: NatalGraha,
): List<String> {
    val cancellations = mutableListOf<String>()
    val marsSign = mars.rasi.index
    if (isStrongByPlace(Graha.MANGALA, marsSign)) {
        cancellations +=
            "Mars is in ${mars.rasi.name} — its own sign or exaltation — and a graha at full " +
            "strength is not held to afflict."
    }
    if (marsSign in MARS_HARMLESS_SIGNS) {
        cancellations += "Mars is in ${mars.rasi.name}, where it is classically held to do no harm."
    }
    val guru = chart.grahas.firstOrNull { it.graha == Graha.GURU }
    if (guru != null && Drishti.influences(Graha.GURU, guru.rasi.index, marsSign)) {
        val how = if (guru.rasi.index == marsSign) "sits with" else "aspects"
        cancellations += "Jupiter $how Mars, and its benefic influence answers the affliction."
    }
    return cancellations
}

/** Parihara lifting one house's affliction only. */
private fun houseParihara(
    house: Int,
    mars: NatalGraha,
): String? =
    if (mars.rasi.index in HOUSE_SIGN_PARIHARA[house].orEmpty()) {
        "Mars is in ${mars.rasi.name}, where the ${ordinal(house)} house's affliction is " +
            "classically lifted."
    } else {
        null
    }
