/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.astronomy

import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlin.time.Instant

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
 * Phase 2 provides the daily essentials: sun times plus the tithi, nakshatra, and vara. The
 * remaining panchanga limbs (yoga, karana) and muhurta windows are added in later increments.
 *
 * @property instant the instant the snapshot was computed for.
 * @property location the observer's coordinates.
 * @property sunTimes sunrise/sunset for the location and local date.
 * @property tithi the current lunar day.
 * @property nakshatra the Moon's current lunar mansion.
 * @property yoga the current Sun–Moon yoga.
 * @property karana the current karana (half-tithi).
 * @property vara the weekday (sunrise-to-sunrise).
 * @property muhurtas the day's auspicious/inauspicious time windows.
 */
data class AstronomySnapshot(
    val instant: Instant,
    val location: GeoCoordinates,
    val sunTimes: SunTimes,
    val tithi: Tithi,
    val nakshatra: Nakshatra,
    val yoga: Yoga,
    val karana: Karana,
    val vara: Vara,
    val muhurtas: List<Muhurta>,
)
