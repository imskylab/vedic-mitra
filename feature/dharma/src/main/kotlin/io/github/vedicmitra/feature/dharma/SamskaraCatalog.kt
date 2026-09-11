/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.dharma

import androidx.annotation.StringRes
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.common.model.ContentSource

/**
 * One samskara, as the grhyasutras describe it.
 *
 * @property kind the identity. Everything else hangs off it.
 * @property source the passage this entry was written from. **No default** — a new entry cannot
 *   compile without deciding, and the honest answer is never invented. Unlike the stotra and mantra
 *   catalogs, nothing here may be [ContentSource.NotRecorded]: those predate the rule and carry a
 *   ratchet; this content was written after it and a test holds the count at zero.
 * @property oneLine shown in the list, without tapping. It carries a whole idea on its own — if
 *   clarity only arrives on tap, most readers never get it.
 * @property body a paragraph, for the reader who tapped because they wanted more.
 * @property muhurta the electional preset for this rite, where the app has one — the bridge the
 *   roadmap calls "where the two domains meet". Null for most of them, and the nulls say something:
 *   the gap runs **both ways**. Muhurta offers a date for Karnavedha, which this catalog cannot
 *   explain; this catalog describes Garbhadhana, Keshanta, Samavartana and Antyeshti, for which
 *   Muhurta offers no date. Neither half is papered over by inventing the other.
 */
data class SamskaraEntry(
    val kind: Samskara,
    val source: ContentSource,
    @param:StringRes val oneLine: Int,
    @param:StringRes val body: Int,
    val muhurta: MuhurtaActivity?,
)

/**
 * The bundled samskara reference.
 *
 * **Mode: Cite + Teach.** Every entry reports what a named sutra says and explains it in plain
 * language. Nothing here instructs: the grhyasutras are described, never prescribed, and the copy is
 * held to the same voice test as the panchanga primer — no sentence may address the reader.
 *
 * ## Where the sutras disagree
 *
 * They disagree often, and the entries say so rather than picking quietly. `docs/knowledge-standards.md`
 * requires it: *"Disagreement is reported, not resolved. Where authorities differ, say so, and say
 * which one this screen follows."* Simantonnayana is the sharpest case — four sutras give four
 * different times, and two of them hold the rite to a first pregnancy.
 *
 * ## Where the citations came from
 *
 * Oldenberg's translation in *Sacred Books of the East* vols 29 and 30 (1886, 1892), which is public
 * domain: Sankhayana, Ashvalayana, Paraskara and Khadira in the first volume, Gobhila, Hiranyakesin
 * and Apastamba in the second. Each locus below was read in that text before it was written down;
 * none was recalled. The project has a specific reason for that habit — see
 * `docs/knowledge-standards.md` on why the stotra and mantra catalogs ship unsourced rather than
 * plausibly sourced.
 */
object SamskaraCatalog {
    /** Every samskara the app can cite, in the order a life meets them. */
    val all: List<SamskaraEntry> =
        listOf(
            SamskaraEntry(
                // Paraskara I, 11, 1-8: the fourth night, the expiation oblations that precede it,
                // and the two answers the sutra gives on what follows.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.11.1-8"),
                kind = Samskara.GARBHADHANA,
                oneLine = R.string.samskara_garbhadhana_one_line,
                body = R.string.samskara_garbhadhana_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Ashvalayana I, 13, 1-7. Sankhayana I, 20, 1 also gives the third month.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "1.13.1-7"),
                kind = Samskara.PUMSAVANA,
                oneLine = R.string.samskara_pumsavana_one_line,
                body = R.string.samskara_pumsavana_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Ashvalayana I, 14, 1-9. Sankhayana I, 22, 1, Paraskara I, 15, 3 and Khadira II, 2,
                // 24 each give a different month; the body reports all four.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "1.14.1-9"),
                kind = Samskara.SIMANTONNAYANA,
                oneLine = R.string.samskara_simantonnayana_one_line,
                body = R.string.samskara_simantonnayana_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Ashvalayana I, 15, 1-8, which folds the naming into this rite rather than giving
                // it a day of its own. Paraskara I, 17, 1 does the opposite; both are reported.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "1.15.1-8"),
                kind = Samskara.JATAKARMAN,
                oneLine = R.string.samskara_jatakarman_one_line,
                body = R.string.samskara_jatakarman_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Paraskara I, 17, 1-3: the tenth day, and the shape a name should take.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.17.1"),
                kind = Samskara.NAMAKARANA,
                oneLine = R.string.samskara_namakarana_one_line,
                body = R.string.samskara_namakarana_body,
                muhurta = MuhurtaActivity.NAMKARAN,
            ),
            SamskaraEntry(
                // Paraskara I, 17, 5-6. Two sutras, and the whole of what that sutra says about it.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.17.5-6"),
                kind = Samskara.NISHKRAMANA,
                oneLine = R.string.samskara_nishkramana_one_line,
                body = R.string.samskara_nishkramana_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Paraskara I, 19, 1. Ashvalayana I, 16, 1 and Sankhayana I, 27, 1 open identically.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.19.1"),
                kind = Samskara.ANNAPRASANA,
                oneLine = R.string.samskara_annaprasana_one_line,
                body = R.string.samskara_annaprasana_body,
                muhurta = MuhurtaActivity.ANNAPRASHANA,
            ),
            SamskaraEntry(
                // Ashvalayana I, 17, 1. Sankhayana I, 28, 2-3 and Paraskara II, 1, 1-2 differ; the
                // body says so.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "1.17.1"),
                kind = Samskara.CHUDAKARANA,
                oneLine = R.string.samskara_chudakarana_one_line,
                body = R.string.samskara_chudakarana_body,
                muhurta = MuhurtaActivity.MUNDAN,
            ),
            SamskaraEntry(
                // Ashvalayana I, 19, 1-8: the year by varna, and the outer limit beyond which the
                // right to learn the Savitri is held to lapse.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "1.19.1-8"),
                kind = Samskara.UPANAYANA,
                oneLine = R.string.samskara_upanayana_one_line,
                body = R.string.samskara_upanayana_body,
                muhurta = MuhurtaActivity.UPANAYANA,
            ),
            SamskaraEntry(
                // Ashvalayana I, 18, 1-8, which declares the rite by reference to the tonsure at
                // I, 17 rather than setting it out again.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "1.18.1-8"),
                kind = Samskara.KESHANTA,
                oneLine = R.string.samskara_keshanta_one_line,
                body = R.string.samskara_keshanta_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Ashvalayana III, 8, 1-21: what has to be got, and the altering of the texts at
                // sutra 7 that turns the one spoken over into the one who speaks.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "3.8.1-21"),
                kind = Samskara.SAMAVARTANA,
                oneLine = R.string.samskara_samavartana_one_line,
                body = R.string.samskara_samavartana_body,
                muhurta = null,
            ),
            SamskaraEntry(
                // Paraskara I, 8, 1: the seven steps, with the count spoken over them.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.8.1"),
                kind = Samskara.VIVAHA,
                oneLine = R.string.samskara_vivaha_one_line,
                body = R.string.samskara_vivaha_body,
                muhurta = MuhurtaActivity.VIVAH,
            ),
            SamskaraEntry(
                // Ashvalayana IV, 1, 1-11, which opens with the illness and only then turns to the
                // death. Deliberately no muhurta: nobody elects the day of this one.
                source = ContentSource.Text(work = "Ashvalayana Grhyasutra", locus = "4.1.1-11"),
                kind = Samskara.ANTYESHTI,
                oneLine = R.string.samskara_antyeshti_one_line,
                body = R.string.samskara_antyeshti_body,
                muhurta = null,
            ),
        )

    private val byKind: Map<Samskara, SamskaraEntry> = all.associateBy { it.kind }

    /** The entry for [kind]. Never null; every [Samskara] has one, and a test iterates `entries`. */
    fun of(kind: Samskara): SamskaraEntry = byKind.getValue(kind)
}
