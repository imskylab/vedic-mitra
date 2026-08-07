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

/** What kind of entry a [Festival] is, so the UI can group or style them. */
enum class FestivalType {
    /** A named festival (Diwali, Holi, Ganesh Chaturthi, …). */
    FESTIVAL,

    /** A recurring lunar observance (Ekadashi, Purnima, Amavasya). */
    OBSERVANCE,

    /** A solar month transition (the Sun entering a new rashi). */
    SANKRANTI,
}

/**
 * A festival or observance falling on a particular day, as computed from the panchanga.
 *
 * @property name the display name (e.g. "Diwali", "Ekadashi", "Makara Sankranti").
 * @property atSunrise the instant of sunrise on the festival day — the UI formats this to a local
 *   date in the location's time zone.
 * @property type what kind of entry this is.
 */
data class Festival(
    val name: String,
    val atSunrise: Instant,
    val type: FestivalType,
)
