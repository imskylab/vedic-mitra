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

import io.github.vedicmitra.core.common.model.GeoCoordinates
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
 * @property birthCoordinates the birthplace geocoded to coordinates, or `null` until resolved.
 * @property birthZoneId the birthplace's IANA time zone (e.g. "Asia/Kolkata"), or `null` until
 *   resolved. Together with [birthCoordinates] this is what a chart needs — the zone fixes the exact
 *   birth instant, the coordinates fix the ascendant/houses.
 */
data class BirthProfile(
    val id: String,
    val name: String = "",
    val relation: ProfileRelation = ProfileRelation.SELF,
    val dateOfBirth: LocalDate? = null,
    val timeOfBirth: LocalTime? = null,
    val placeOfBirth: String = "",
    val birthCoordinates: GeoCoordinates? = null,
    val birthZoneId: String? = null,
) {
    /** Whether the user-entered essentials (name, date, time, place) are all present. */
    val isComplete: Boolean
        get() = name.isNotBlank() && dateOfBirth != null && timeOfBirth != null && placeOfBirth.isNotBlank()

    /** Whether the profile has everything a birth chart needs, including geocoded coordinates + zone. */
    val isChartReady: Boolean
        get() = isComplete && birthCoordinates != null && birthZoneId != null
}
