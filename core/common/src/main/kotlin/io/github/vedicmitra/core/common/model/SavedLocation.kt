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
 * A place the user has saved, with everything the panchanga needs to compute for it: its
 * coordinates and the IANA time-zone id that places day boundaries (sunrise, "today") in that
 * location's own local time rather than the device's.
 *
 * A plain, framework-agnostic value type — it holds data but performs no calculation.
 *
 * @property id stable, app-generated identifier (never contains the codec separator).
 * @property label human-readable name shown in the UI (e.g. "Home", "Varanasi").
 * @property coordinates the geographic point.
 * @property zoneId IANA time-zone id for the location (e.g. "Asia/Kolkata").
 * @property source how the coordinates were obtained.
 */
data class SavedLocation(
    val id: String,
    val label: String,
    val coordinates: GeoCoordinates,
    val zoneId: String,
    val source: LocationSource,
)

/** How the coordinates of a [SavedLocation] were obtained. */
enum class LocationSource {
    /** Captured from the device's GPS / fused location. */
    DEVICE,

    /** Chosen from a city / place search. */
    CITY,

    /** Entered manually as latitude/longitude. */
    MANUAL,
}
