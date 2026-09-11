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
 * @property muhurta the electional preset for this rite, where the app has one. The bridge the
 *   roadmap calls "where the two domains meet" — and the reason this domain was built next, since
 *   Muhurta was already offering dates for rites nothing explained.
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
 * which one this screen follows."* Chudakarana is the clearest case — three sutras give three
 * different years.
 *
 * ## Where the citations came from
 *
 * Oldenberg's translation in *Sacred Books of the East* vols 29–30 (1886), which is public domain.
 * Each locus below was read in that text before it was written down; none was recalled. The project
 * has a specific reason for that habit — see `docs/knowledge-standards.md` on why the stotra and
 * mantra catalogs ship unsourced rather than plausibly sourced.
 */
object SamskaraCatalog {
    /** Every samskara the app can cite, in the order a life meets them. */
    val all: List<SamskaraEntry> =
        listOf(
            SamskaraEntry(
                // Paraskara I, 17, 1-3: the tenth day, and the shape a name should take.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.17.1"),
                kind = Samskara.NAMAKARANA,
                oneLine = R.string.samskara_namakarana_one_line,
                body = R.string.samskara_namakarana_body,
                muhurta = MuhurtaActivity.NAMKARAN,
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
                // Paraskara I, 8, 1: the seven steps, with the count spoken over them.
                source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.8.1"),
                kind = Samskara.VIVAHA,
                oneLine = R.string.samskara_vivaha_one_line,
                body = R.string.samskara_vivaha_body,
                muhurta = MuhurtaActivity.VIVAH,
            ),
        )

    private val byKind: Map<Samskara, SamskaraEntry> = all.associateBy { it.kind }

    /** The entry for [kind]. Never null; every [Samskara] has one, and a test iterates `entries`. */
    fun of(kind: Samskara): SamskaraEntry = byKind.getValue(kind)
}
