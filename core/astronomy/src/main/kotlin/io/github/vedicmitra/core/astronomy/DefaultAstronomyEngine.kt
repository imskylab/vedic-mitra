/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
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
                val moonTimes = LunarDay.moonTimes(epochMillis, location.latitude, location.longitude)
                val goldenHour = SolarDay.goldenHour(epochMillis, location.latitude, location.longitude)
                val vara = varaOf(instant, epochMillis, location.longitude, sunTimes.sunrise)
                val varjyam =
                    varjyamOf(epochMillis, moonSidereal) { atEpochMillis ->
                        val atT = Ephemeris.julianCenturies(atEpochMillis)
                        Ephemeris.norm360(Ephemeris.moonLongitude(atT) - Ephemeris.lahiriAyanamsa(atT))
                    }

                AppResult.Success(
                    AstronomySnapshot(
                        instant = instant,
                        location = location,
                        sunTimes = sunTimes,
                        moonTimes = moonTimes,
                        tithi = tithiOf(elongation),
                        nakshatra = nakshatraOf(moonSidereal),
                        yoga = yogaOf(yogaSum),
                        karana = karanaOf(elongation),
                        vara = vara,
                        ayana = ayanaOf(sunSidereal),
                        ritu = rituOf(sunSidereal),
                        moonPhase = moonPhaseOf(elongation),
                        goldenHour = goldenHour,
                        muhurtas = muhurtasOf(sunTimes, vara.ordinal) + varjyam,
                    ),
                )
            }
        }

        override suspend fun daySummaryAt(
            instant: Instant,
            location: GeoCoordinates,
        ): AppResult<PanchangaDaySummary> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                val t = Ephemeris.julianCenturies(instant.toEpochMilliseconds())
                val sunLongitude = Ephemeris.sunApparentLongitude(t)
                val moonLongitude = Ephemeris.moonLongitude(t)
                val elongation = Ephemeris.norm360(moonLongitude - sunLongitude)
                val moonSidereal = Ephemeris.norm360(moonLongitude - Ephemeris.lahiriAyanamsa(t))

                AppResult.Success(
                    PanchangaDaySummary(
                        tithi = tithiOf(elongation),
                        nakshatra = nakshatraOf(moonSidereal),
                        moonPhase = moonPhaseOf(elongation),
                    ),
                )
            }
        }
    }
