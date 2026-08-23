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

import kotlin.time.Instant

/**
 * One mahadasha (major planetary period) in the Vimshottari system: the ruling [lord] governs the
 * span [start]..[end]. The birth instant falls inside the first period of a computed timeline.
 *
 * @property lord the graha ruling this period.
 * @property start when the period begins.
 * @property end when the period ends (and the next lord's period begins).
 */
data class MahadashaPeriod(
    val lord: Graha,
    val start: Instant,
    val end: Instant,
) {
    /**
     * The nine antardashas (sub-periods) this mahadasha divides into.
     *
     * Derived rather than stored, for the same reason the Spashta Graha columns are: they are a pure
     * function of the lord and the span, and storing them would allow a period whose sub-periods
     * disagree with it.
     */
    val antardashas: List<AntardashaPeriod> get() = antardashasOf(this)
}

/**
 * One antardasha: a sub-period within a [MahadashaPeriod], ruled by [lord].
 *
 * The nine antardashas of a mahadasha run through the same lord sequence, beginning with the
 * mahadasha's own lord, each lasting a share of the parent period proportional to its own dasha
 * years.
 *
 * @property lord the graha ruling this sub-period.
 * @property start when it begins.
 * @property end when it ends.
 */
data class AntardashaPeriod(
    val lord: Graha,
    val start: Instant,
    val end: Instant,
)
