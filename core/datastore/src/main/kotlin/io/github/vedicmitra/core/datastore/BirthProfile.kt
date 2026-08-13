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

/** How a [BirthProfile] relates to the app's user. The primary profile is always [SELF]. */
enum class ProfileRelation(
    val displayName: String,
) {
    SELF("Self"),
    SPOUSE("Spouse"),
    CHILD("Child"),
    PARENT("Parent"),
    FRIEND("Friend"),
    OTHER("Other"),
}

/**
 * One person's birth details — a chart the app can cast. The user keeps several (their own plus
 * family/friends); exactly one is the primary "Self" profile the personalised experiences key off.
 *
 * @property id stable unique id (assigned once, on creation).
 * @property name the person's name.
 * @property relation how they relate to the user.
 * @property dateOfBirth date of birth, or `null` if not yet set.
 * @property timeOfBirth exact local time of birth, or `null` if not yet set.
 * @property placeOfBirth free-text place of birth (city, country).
 */
data class BirthProfile(
    val id: String,
    val name: String = "",
    val relation: ProfileRelation = ProfileRelation.SELF,
    val dateOfBirth: LocalDate? = null,
    val timeOfBirth: LocalTime? = null,
    val placeOfBirth: String = "",
) {
    /** Whether every field needed for a birth chart is present. */
    val isComplete: Boolean
        get() = name.isNotBlank() && dateOfBirth != null && timeOfBirth != null && placeOfBirth.isNotBlank()
}
