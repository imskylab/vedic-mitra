/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

/**
 * One completed japa sitting.
 *
 * @property completedAtEpochMillis when the sitting was logged (also its unique key and sort order).
 * @property dateEpochDay the local date it was logged on (`LocalDate.toEpochDay`), for day-grouping
 *   and streaks — kept separate from the millis so grouping never depends on a time zone at read time.
 * @property mantraId the id of the mantra chanted (see the feature's mantra catalog).
 * @property beads the total beads counted in the sitting.
 * @property rounds the number of completed malas (108-bead rounds) in the sitting.
 * @property nakshatraNumber the Moon's nakshatra (1..27) when logged, or `null` if it wasn't recorded.
 * @property tithiNumber the tithi (1..30) when logged, or `null` if it wasn't recorded.
 */
data class JapaSession(
    val completedAtEpochMillis: Long,
    val dateEpochDay: Long,
    val mantraId: String,
    val beads: Int,
    val rounds: Int,
    val nakshatraNumber: Int? = null,
    val tithiNumber: Int? = null,
)

/**
 * An in-progress mala that survives leaving the screen, so a sitting can be resumed rather than lost.
 *
 * @property mantraId the mantra being chanted.
 * @property beads the beads counted so far (completed rounds plus the current partial mala).
 */
data class JapaProgress(
    val mantraId: String,
    val beads: Int,
)
