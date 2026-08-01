/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.astronomy

import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.datetime.Instant

/**
 * Port for astronomical / panchanga calculations.
 *
 * Consumers (features, use cases) depend on this abstraction so the concrete ephemeris-backed
 * engine can be swapped and tested. **No calculation is implemented in Phase 1** — this file
 * declares only the contract. The concrete [AstronomyEngine] implementation, and the full shape of
 * [AstronomySnapshot], arrive in the astronomy implementation phase.
 */
interface AstronomyEngine {
    /**
     * Computes an astronomy snapshot for the given [instant] observed from [location].
     *
     * @return [AppResult.Success] with the snapshot, or [AppResult.Failure] if it cannot be
     *   computed. Implementations must not throw for expected failures.
     */
    suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot>
}

/**
 * Immutable result of an astronomy computation for a single instant and location.
 *
 * Phase-1 skeleton: it carries only the query inputs so the contract is exercisable end to end.
 * Concrete astronomical fields (sunrise/sunset, tithi, nakshatra, yoga, karana, ...) are added when
 * the engine is implemented.
 */
data class AstronomySnapshot(
    val instant: Instant,
    val location: GeoCoordinates,
)
