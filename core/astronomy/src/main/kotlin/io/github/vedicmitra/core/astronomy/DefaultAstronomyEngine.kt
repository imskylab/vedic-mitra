/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.astronomy

import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Default [AstronomyEngine] backed by the low-precision [Ephemeris]. Computation is pure and
 * CPU-bound, so it runs on the injected default dispatcher.
 */
class DefaultAstronomyEngine
    @Inject
    constructor(
        private val dispatchers: DispatcherProvider,
    ) : AstronomyEngine {
        override suspend fun snapshotAt(
            instant: Instant,
            location: GeoCoordinates,
        ): AppResult<AstronomySnapshot> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                val epochMillis = instant.toEpochMilliseconds()
                val t = Ephemeris.julianCenturies(epochMillis)

                val sunLongitude = Ephemeris.sunApparentLongitude(t)
                val moonLongitude = Ephemeris.moonLongitude(t)
                val ayanamsa = Ephemeris.lahiriAyanamsa(t)
                val elongation = Ephemeris.norm360(moonLongitude - sunLongitude)
                val sunSidereal = Ephemeris.norm360(sunLongitude - ayanamsa)
                val moonSidereal = Ephemeris.norm360(moonLongitude - ayanamsa)
                val yogaSum = Ephemeris.norm360(sunSidereal + moonSidereal)
                val sunTimes = SolarDay.sunTimes(epochMillis, location.latitude, location.longitude)
                val vara = varaOf(instant, epochMillis, location.longitude, sunTimes.sunrise)

                AppResult.Success(
                    AstronomySnapshot(
                        instant = instant,
                        location = location,
                        sunTimes = sunTimes,
                        tithi = tithiOf(elongation),
                        nakshatra = nakshatraOf(moonSidereal),
                        yoga = yogaOf(yogaSum),
                        karana = karanaOf(elongation),
                        vara = vara,
                        muhurtas = muhurtasOf(sunTimes, vara.ordinal),
                    ),
                )
            }
        }
    }
