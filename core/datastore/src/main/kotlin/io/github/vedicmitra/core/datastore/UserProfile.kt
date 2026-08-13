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

import java.time.LocalDate
import java.time.LocalTime

/**
 * The user's birth profile — the foundation the astrology features (Kundali, Rashifal, Muhurta)
 * build on. Fields are nullable/blank until the user fills them in.
 *
 * @property name the person's name (for display; not used in any calculation).
 * @property dateOfBirth date of birth, or `null` if not yet set.
 * @property timeOfBirth exact local time of birth, or `null` if not yet set. Ascendant, houses and
 *   divisional charts collapse without an accurate time, so it is captured explicitly.
 * @property placeOfBirth free-text place of birth (city, country). Geocoding to coordinates and a
 *   time zone is a later refinement.
 */
data class UserProfile(
    val name: String = "",
    val dateOfBirth: LocalDate? = null,
    val timeOfBirth: LocalTime? = null,
    val placeOfBirth: String = "",
) {
    /** Whether every field needed for a birth chart is present. */
    val isComplete: Boolean
        get() = name.isNotBlank() && dateOfBirth != null && timeOfBirth != null && placeOfBirth.isNotBlank()
}
