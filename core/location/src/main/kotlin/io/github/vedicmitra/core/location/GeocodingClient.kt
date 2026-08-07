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
import io.github.vedicmitra.core.common.result.AppResult

/**
 * Port for turning a place name (a city or address) into candidate coordinates — "forward
 * geocoding". Backs the city-search way of adding a location. The concrete implementation uses the
 * platform geocoder, which may require network access.
 */
interface GeocodingClient {
    /**
     * Searches for places matching [query], returning up to [maxResults] candidates ordered by the
     * geocoder's relevance.
     *
     * @return [AppResult.Success] with the (possibly empty) candidate list, or [AppResult.Failure]
     *   if the lookup could not be performed (e.g. no geocoder backend, or an I/O error).
     */
    suspend fun search(
        query: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
    ): AppResult<List<GeocodeResult>>

    companion object {
        /** Default number of candidates returned by [search]. */
        const val DEFAULT_MAX_RESULTS = 5
    }
}

/**
 * A single place returned by [GeocodingClient.search].
 *
 * @property label a human-readable name for the place (e.g. "Varanasi, Uttar Pradesh, India").
 * @property coordinates the place's coordinates.
 */
data class GeocodeResult(
    val label: String,
    val coordinates: GeoCoordinates,
)
