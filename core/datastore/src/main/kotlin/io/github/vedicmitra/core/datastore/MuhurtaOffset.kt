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
 * A user-configured lead time for one muhurta, keyed by its display [name] (e.g. "Brahma
 * Muhurta"). Only names the user has explicitly customized are stored; a name absent from
 * [ReminderRepository.offsetMinutesByName] has not been overridden and callers should fall back to
 * [ReminderRepository.DEFAULT_OFFSET_MINUTES].
 *
 * @property name the muhurta's traditional name, matching [io.github.vedicmitra.core.astronomy.Muhurta.name].
 * @property offsetMinutes minutes before the window start the reminder should fire (0 = at start).
 */
data class MuhurtaOffset(
    val name: String,
    val offsetMinutes: Int,
)
