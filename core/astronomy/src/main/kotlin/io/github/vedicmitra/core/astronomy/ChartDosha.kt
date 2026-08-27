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

/**
 * A single-chart dosha that is either present or not, with the working that decided it.
 *
 * [MangalDosha] deliberately keeps its own richer shape: it can be raised from three reference points
 * independently and answered by two different kinds of parihara, so it needs a list of triggers and
 * cancellations rather than one verdict. Kala Sarpa and Ganda Moola are not like that — each is a
 * single geometric fact about the chart — and giving them the same structure would mean carrying
 * lists that always hold one item.
 *
 * The three fields match [ChartYoga]'s `name` / `rule` / `summary`, because a dosha is a named
 * combination like any other and the Yogas page renders them side by side.
 *
 * @property name what the dosha is called, with its type where it has one.
 * @property present whether it stands in this chart.
 * @property rule what had to be true, stated so a reader can check it against their own chart —
 *   which grahas, which signs, which nakshatra. Filled in whether or not the dosha is present, since
 *   "why not" is as much worth reading as "why".
 * @property summary what it is traditionally held to indicate, or `null` when it is absent and there
 *   is nothing to say.
 */
data class ChartDosha(
    val name: String,
    val present: Boolean,
    val rule: String,
    val summary: String? = null,
)
