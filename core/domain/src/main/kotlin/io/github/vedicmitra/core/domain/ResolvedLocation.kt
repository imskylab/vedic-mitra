/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.domain

import io.github.vedicmitra.core.common.model.GeoCoordinates

/**
 * The location the app should compute the panchanga for right now, after applying the resolution
 * order (selected saved location, else device location, else a built-in default). Carries the
 * time-zone that day boundaries should be placed in and a label to show the user.
 *
 * @property coordinates the coordinates to compute for.
 * @property zoneId IANA time-zone id used to place day boundaries in the location's local time.
 * @property label human-readable name for the location.
 * @property isDefault whether this is the built-in fallback (no selection and no device location).
 */
data class ResolvedLocation(
    val coordinates: GeoCoordinates,
    val zoneId: String,
    val label: String,
    val isDefault: Boolean,
)
