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

import io.github.vedicmitra.core.common.model.MaasaReckoning

/**
 * One coordinate of a [SankalpaFrame] — the name of a measure, and its value now.
 *
 * @property label the measure, e.g. "Tithi".
 * @property value what it reads at this instant, e.g. "Chaturdashi".
 */
data class SankalpaCoordinate(
    val label: String,
    val value: String,
)

/**
 * Where and when a moment sits, in the ten measures a sankalpa names.
 *
 * A sankalpa traditionally opens by fixing the place and then the time — the year, the ayana, the
 * season, the month, the fortnight, the tithi, the weekday, the nakshatra, the yoga and the karana.
 * Those ten are precisely the values a panchanga reports, which is much of *why* a panchanga reports
 * them, and this app already computes every one. Assembling them is therefore not new astronomy: it
 * is the existing snapshot, read out in the order the tradition reads it.
 *
 * ## What this is, in the terms of `docs/knowledge-standards.md`
 *
 * The **values are computed** — each comes straight from [AstronomySnapshot] and is covered by the
 * validation the rest of the engine is. The **ordering is a cited claim**, and the citation is owed:
 * the sequence below is the one in common use, and this file does not yet name the text it follows.
 * Recitation order varies by sampradaya and region. Until a source is named here, the ordering
 * should be read as customary rather than authoritative — the same gap the stotra and mantra
 * catalogs carry, tracked as foundation work **F3** on the roadmap.
 *
 * ## What is deliberately not done
 *
 * - **No declension.** A sankalpa recites these in the locative — *Parābhava-nāma saṃvatsare*,
 *   *caitra-māse* — and generating correct forms for some 170 names is Sanskrit grammar this app has
 *   no basis for. The values appear as the app names them everywhere else. Producing plausible-
 *   looking Sanskrit that is wrong would be worse than producing none.
 * - **No Devanagari.** The panchanga vocabulary exists only in transliteration in this codebase.
 * - **No cosmological prefix.** The kalpa, manvantara and yuga a sankalpa opens with are fixed
 *   liturgical text rather than anything computed, and reciting them is where assembling a frame
 *   would turn into composing the formula itself.
 * - **No personal elements and no intent.** The gotra, the name and the purpose belong to whoever is
 *   speaking. This states *when and where*, which is a fact about the moment; it does not compose a
 *   sankalpa for anyone, and it does not suggest that one be made.
 *
 * @property place where the moment is being located, or `null` if unknown. Rendered first, because
 *   desha precedes kala.
 * @property coordinates the ten measures, in recitation order.
 */
data class SankalpaFrame(
    val place: String?,
    val coordinates: List<SankalpaCoordinate>,
) {
    /**
     * The frame as plain lines, for copying out. Place first, then the ten measures — someone
     * writing this down wants it in the order it is spoken, not the order a table happens to use.
     */
    val asText: String
        get() =
            (listOfNotNull(place?.let { SankalpaCoordinate("Place", it) }) + coordinates)
                .joinToString(separator = "\n") { "${it.label}: ${it.value}" }
}

/**
 * Reads this snapshot as a [SankalpaFrame], optionally naming [place], with month names under
 * [reckoning].
 *
 * Pure: every value is already on the snapshot, so this adds no ephemeris work and cannot disagree
 * with what the rest of the screen shows — including the month name, which follows the same
 * [nameIn] the day card uses.
 */
fun AstronomySnapshot.sankalpaFrame(
    place: String? = null,
    reckoning: MaasaReckoning = MaasaReckoning.AMANTA,
): SankalpaFrame =
    SankalpaFrame(
        place = place,
        coordinates =
            listOf(
                SankalpaCoordinate("Samvatsara", samvatsara.name),
                SankalpaCoordinate("Ayana", ayana.displayName),
                SankalpaCoordinate("Ritu", ritu.displayName),
                SankalpaCoordinate("Maasa", maasa.nameIn(reckoning, tithi.paksha)),
                SankalpaCoordinate("Paksha", tithi.paksha.displayName),
                SankalpaCoordinate("Tithi", tithi.name),
                SankalpaCoordinate("Vara", vara.displayName),
                SankalpaCoordinate("Nakshatra", nakshatra.name),
                SankalpaCoordinate("Yoga", yoga.name),
                SankalpaCoordinate("Karana", karana.name),
            ),
    )
