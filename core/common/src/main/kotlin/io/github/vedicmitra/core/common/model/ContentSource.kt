/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.common.model

/**
 * Where a piece of traditional content came from.
 *
 * Required on every bundled text by `docs/knowledge-standards.md`: computation is checked against an
 * independent reference, and knowledge is checked by **citation**. There is no golden test that can
 * say a verse follows the right recension, so the source *is* the check — which means it has to be
 * data on the model, visible to the reader who wants it, rather than a comment only a maintainer
 * ever sees.
 *
 * It is deliberately not a plain `String`. [NotRecorded] has to be a value the code can count, so a
 * test can hold the line against new unsourced content; a nullable string or an empty one would let
 * that debt grow quietly. And [Text] keeps the work, the place in it and the recension apart,
 * because a citation that cannot name the recension is not much of a citation for material that has
 * more than one.
 */
sealed interface ContentSource {
    /**
     * A named text this is taken from.
     *
     * @property work the text, e.g. "Rigveda".
     * @property locus where in it, e.g. "3.62.10" — omitted when the work is the whole passage.
     * @property recension which recension, where they differ and the choice matters.
     */
    data class Text(
        val work: String,
        val locus: String? = null,
        val recension: String? = null,
    ) : ContentSource

    /**
     * No source has been identified yet.
     *
     * This is an admission, not a category. It exists because the content bundled before the rule
     * came in has no recorded source, and **inventing plausible citations for it would be far worse
     * than admitting the gap** — a wrong attribution is a claim, and the whole point of citing is to
     * let a reader check. Content in this state says so on screen.
     */
    data object NotRecorded : ContentSource

    /** One line naming the source, for display beside the content. */
    val label: String
        get() =
            when (this) {
                is Text ->
                    listOfNotNull(work, locus, recension?.let { "$it recension" })
                        .joinToString(separator = " ")

                NotRecorded -> "Source not recorded"
            }
}
