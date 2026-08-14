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
)
