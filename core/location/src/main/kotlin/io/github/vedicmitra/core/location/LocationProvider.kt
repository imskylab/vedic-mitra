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
import kotlinx.coroutines.flow.Flow

/**
 * Port for reading the device's location.
 *
 * **No location access is implemented in Phase 1** — this declares only the contract. The concrete
 * implementation (permission handling, fused location provider) is added in the location
 * implementation phase.
 */
interface LocationProvider {
    /** Returns the last known [GeoCoordinates], or a failure if none is available. */
    suspend fun currentLocation(): AppResult<GeoCoordinates>

    /** Emits location updates over time. Collection begins location tracking; cancellation stops it. */
    fun locationUpdates(): Flow<GeoCoordinates>
}
