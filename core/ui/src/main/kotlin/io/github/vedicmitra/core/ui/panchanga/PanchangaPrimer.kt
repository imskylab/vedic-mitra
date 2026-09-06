/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.ui.panchanga

import androidx.annotation.StringRes
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.core.ui.R

/**
 * Plain-language explanations of the panchanga *ideas*, for a reader who has never met them.
 *
 * Lives here rather than in `:core:astronomy` because it is copy, not calculation. The engine is
 * pure Kotlin with no Android dependency and is meant to stay that way (F5, the portable engine);
 * text that has to be translated cannot live in it. [PanchangaConcept] itself stays in the engine,
 * because `LimbCycle` names one per row -- the *key* is the engine's, the words are not.
 *
 * ## Why this is not [PanchangaGlossary]
 *
 * That object explains **named items** -- "Rahu Kalam", "Ekadashi", "Diwali" -- keyed by the exact
 * string each is displayed with, and returns `null` for anything it does not know. That shape is
 * right for items and wrong for concepts, in two ways. Concept names collide with item names
 * ("Purnima" is both a tithi and an observance), and a missing key degrades silently to a fallback
 * string rather than failing loudly.
 *
 * So concepts are keyed by [PanchangaConcept] instead: a closed set, looked up with `getValue`, and
 * covered by a test that iterates the enum. **Adding a concept without writing its copy breaks the
 * build**, which is the only reliable way to keep explanatory text a first-class part of a feature
 * rather than the thing that gets cut last.
 *
 * ## The one-line / body split
 *
 * [PrimerEntry.oneLine] is shown **without tapping** -- in a legend, beside a ring, under a value.
 * If clarity only ever arrives on tap, most readers never get it, so the short form has to carry a
 * real idea on its own rather than being a teaser for the long one.
 */
object PanchangaPrimer {
    /** The explanation of [concept]. Never null; [PanchangaConcept] is covered exhaustively. */
    fun of(concept: PanchangaConcept): PrimerEntry = ENTRIES.getValue(concept)

    private val ENTRIES: Map<PanchangaConcept, PrimerEntry> =
        mapOf(
            PanchangaConcept.PANCHANGA to
                PrimerEntry(
                    title = R.string.primer_panchanga_title,
                    oneLine = R.string.primer_panchanga_one_line,
                    body = R.string.primer_panchanga_body,
                ),
            PanchangaConcept.TITHI to
                PrimerEntry(
                    title = R.string.primer_tithi_title,
                    oneLine = R.string.primer_tithi_one_line,
                    body = R.string.primer_tithi_body,
                ),
            PanchangaConcept.PAKSHA to
                PrimerEntry(
                    title = R.string.primer_paksha_title,
                    oneLine = R.string.primer_paksha_one_line,
                    body = R.string.primer_paksha_body,
                ),
            PanchangaConcept.VARA to
                PrimerEntry(
                    title = R.string.primer_vara_title,
                    oneLine = R.string.primer_vara_one_line,
                    body = R.string.primer_vara_body,
                ),
            PanchangaConcept.NAKSHATRA to
                PrimerEntry(
                    title = R.string.primer_nakshatra_title,
                    oneLine = R.string.primer_nakshatra_one_line,
                    body = R.string.primer_nakshatra_body,
                ),
            PanchangaConcept.PADA to
                PrimerEntry(
                    title = R.string.primer_pada_title,
                    oneLine = R.string.primer_pada_one_line,
                    body = R.string.primer_pada_body,
                ),
            PanchangaConcept.YOGA to
                PrimerEntry(
                    title = R.string.primer_yoga_title,
                    oneLine = R.string.primer_yoga_one_line,
                    body = R.string.primer_yoga_body,
                ),
            PanchangaConcept.KARANA to
                PrimerEntry(
                    title = R.string.primer_karana_title,
                    oneLine = R.string.primer_karana_one_line,
                    body = R.string.primer_karana_body,
                ),
            PanchangaConcept.LUNAR_MONTH to
                PrimerEntry(
                    title = R.string.primer_lunar_month_title,
                    oneLine = R.string.primer_lunar_month_one_line,
                    body = R.string.primer_lunar_month_body,
                ),
            PanchangaConcept.SUNRISE_DAY to
                PrimerEntry(
                    title = R.string.primer_sunrise_day_title,
                    oneLine = R.string.primer_sunrise_day_one_line,
                    body = R.string.primer_sunrise_day_body,
                ),
            PanchangaConcept.MOON_PHASE to
                PrimerEntry(
                    title = R.string.primer_moon_phase_title,
                    oneLine = R.string.primer_moon_phase_one_line,
                    body = R.string.primer_moon_phase_body,
                ),
            PanchangaConcept.RASHI to
                PrimerEntry(
                    title = R.string.primer_rashi_title,
                    oneLine = R.string.primer_rashi_one_line,
                    body = R.string.primer_rashi_body,
                ),
            PanchangaConcept.RITU to
                PrimerEntry(
                    title = R.string.primer_ritu_title,
                    oneLine = R.string.primer_ritu_one_line,
                    body = R.string.primer_ritu_body,
                ),
            PanchangaConcept.SANKALPA to
                PrimerEntry(
                    title = R.string.primer_sankalpa_title,
                    oneLine = R.string.primer_sankalpa_one_line,
                    body = R.string.primer_sankalpa_body,
                ),
        )
}

/**
 * One explanation, in two lengths, as string resources.
 *
 * Resource ids rather than resolved text: the strings are looked up where they are drawn, at the
 * locale in force then. That also keeps this object usable outside a composition.
 *
 * @property title what a detail sheet is headed with.
 * @property oneLine a single idea, short enough to sit beside a value without tapping.
 * @property body two to four sentences, for a reader who tapped because they wanted more.
 */
data class PrimerEntry(
    @param:StringRes val title: Int,
    @param:StringRes val oneLine: Int,
    @param:StringRes val body: Int,
)
