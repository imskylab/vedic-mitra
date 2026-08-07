/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.location

import io.github.vedicmitra.core.common.model.GeoCoordinates

/**
 * Port for resolving the IANA time-zone id that governs a set of coordinates — offline. The zone id
 * (e.g. "Asia/Kolkata", "America/New_York") is what carries the daylight-saving rules, so day
 * boundaries computed with it are correct year-round.
 */
interface TimeZoneResolver {
    /**
     * Returns the IANA zone id for [coordinates]. Never fails: for points with no time-zone polygon
     * (e.g. open ocean) it falls back to a longitude-based estimate (see [TimeZoneEstimator]).
     */
    suspend fun resolve(coordinates: GeoCoordinates): String
}
