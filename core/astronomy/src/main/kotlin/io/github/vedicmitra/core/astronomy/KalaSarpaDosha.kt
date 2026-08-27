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
 * Kala Sarpa dosha — every graha hemmed between Rahu and Ketu.
 *
 * ## The rule, and the part that is easy to get wrong
 *
 * The dosha stands when all seven classical grahas fall on one side of the Rahu–Ketu axis, so the
 * nodes bracket the whole chart. The nodes themselves are the bracket and are not counted among the
 * seven.
 *
 * **The containment is by whole sign, not by longitude, and this was tested rather than assumed.**
 * Nearly every statement of the rule says the grahas must lie *between* Rahu and Ketu, which reads
 * as a comparison of longitudes. Checked against an independent implementation over 56 charts, 18
 * of them carrying the dosha:
 *
 * | Predicate | Agreement |
 * |---|---|
 * | By longitude — the textbook phrasing | wrong on **every** positive |
 * | By whole sign | all 56 |
 *
 * A graha in the *same sign* as a node counts as inside the arc even when its longitude has already
 * passed the node. On one of the sampled charts both the Moon and Saturn sat just past Ketu by a
 * degree or two and the dosha was still reported. Implementing the remembered rule would have
 * produced a false negative on exactly those charts, and — this being an uncommon dosha — silently.
 *
 * Whole sign is also what this engine already does everywhere else: houses are whole-sign, and so is
 * [Drishti]. The rule that turned out to be right is the one consistent with the rest of the app.
 *
 * ## The type
 *
 * The twelve named types are read off **Rahu's house**, and all twelve were confirmed by holding a
 * chart's date fixed and sweeping the birth time, which turns the lagna through all twelve houses in
 * a day and so names each type in turn.
 *
 * ## What is deliberately not modelled
 *
 * Sources describe a *partial* or "Kala Amrita" variant when the axis is reversed (grahas running
 * from Ketu to Rahu rather than Rahu to Ketu), and others soften the verdict when a graha sits
 * exactly on a node. Neither is applied here: the reference reported the plain dosha in both
 * directions, and a distinction this engine cannot check is not one worth inventing.
 */
fun kalaSarpaDoshaOf(chart: NatalChart): ChartDosha {
    val byGraha = chart.grahas.associateBy { it.graha }
    val rahu = byGraha[Graha.RAHU]
    val ketu = byGraha[Graha.KETU]
    val hemmed = CLASSICAL_SEVEN.mapNotNull { byGraha[it] }

    // The lightweight charts test fixtures build can omit grahas; a real chart never does.
    if (rahu == null || ketu == null || hemmed.size < CLASSICAL_SEVEN.size) return NO_KALA_SARPA

    val outside = hemmed.filterNot { it.rasi.index in arcFrom(rahu.rasi.index) }
    val outsideReversed = hemmed.filterNot { it.rasi.index in arcFrom(ketu.rasi.index) }
    val present = outside.isEmpty() || outsideReversed.isEmpty()
    val type = KALA_SARPA_TYPES[rahu.house - 1]

    return ChartDosha(
        name = if (present) "Kala Sarpa ($type)" else "Kala Sarpa",
        present = present,
        rule =
            if (present) {
                "Every graha falls in the seven signs from Rahu in ${rahu.rasi.name} to Ketu in " +
                    "${ketu.rasi.name}. Rahu occupies the ${ordinal(rahu.house)} house, which names " +
                    "this the $type type."
            } else {
                val stray = outside.takeIf { it.size <= outsideReversed.size } ?: outsideReversed
                "Rahu is in ${rahu.rasi.name} and Ketu in ${ketu.rasi.name}, and " +
                    "${stray.joinToString(", ") { it.graha.displayName }} " +
                    "${if (stray.size == 1) "falls" else "fall"} outside the arc between them."
            },
        summary =
            if (present) {
                "With the nodes bracketing every graha, the chart is classically read as one where " +
                    "progress comes through sustained effort rather than easily, and standing " +
                    "fluctuates. The tradition prescribes remedial observances, and treats the " +
                    "effect as varying with which axis the nodes fall on."
            } else {
                null
            },
    )
}

/** Whether [rasiIndex] lies in the seven-sign arc beginning at [nodeSign] — the node's own sign first. */
private fun arcFrom(nodeSign: Int): Set<Int> = (0 until ARC_SIGNS).mapTo(mutableSetOf()) { (nodeSign + it) % 12 }

/**
 * Rahu and Ketu are opposite, six signs apart, so the arc from one to the other spans seven signs
 * counting both ends. The two arcs therefore overlap in exactly the two node signs, which is what
 * makes a graha sharing a sign with a node count as inside whichever way the chart is read.
 */
private const val ARC_SIGNS = 7

private val NO_KALA_SARPA =
    ChartDosha(
        name = "Kala Sarpa",
        present = false,
        rule = "Not raised: the chart does not place every graha on one side of the Rahu–Ketu axis.",
    )

/** The seven the nodes must bracket. Rahu and Ketu are the bracket, so they are not among them. */
private val CLASSICAL_SEVEN =
    listOf(
        Graha.SUN,
        Graha.MOON,
        Graha.MANGALA,
        Graha.BUDHA,
        Graha.GURU,
        Graha.SHUKRA,
        Graha.SHANI,
    )

/**
 * The twelve types, indexed by Rahu's house: the 1st house first.
 *
 * Every entry was confirmed against an independent implementation, not recalled — one chart's date
 * held fixed while the birth time swept a full day, which moves the lagna through all twelve houses
 * and names each type in turn.
 */
private val KALA_SARPA_TYPES =
    listOf(
        "Ananta",
        "Kulika",
        "Vasuki",
        "Shankhapala",
        "Padma",
        "Mahapadma",
        "Takshaka",
        "Karkotaka",
        "Shankhachooda",
        "Ghataka",
        "Vishdhara",
        "Sheshanaga",
    )
