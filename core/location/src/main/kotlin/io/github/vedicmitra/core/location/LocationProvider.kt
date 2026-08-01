/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
