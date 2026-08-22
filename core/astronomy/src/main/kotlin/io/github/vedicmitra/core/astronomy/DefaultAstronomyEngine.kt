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
                // The night Choghadiya run from sunset to the *following* sunrise, so resolve it too.
                val nextSunrise =
                    SolarDay
                        .sunTimes(epochMillis + 86_400_000L, location.latitude, location.longitude)
                        .sunrise
                val moonTimes = LunarDay.moonTimes(epochMillis, location.latitude, location.longitude)
                val goldenHour = SolarDay.goldenHour(epochMillis, location.latitude, location.longitude)
                val vara = varaOf(instant, epochMillis, location.longitude, sunTimes.sunrise)
                val varjyam =
                    varjyamOf(epochMillis, moonSidereal) { atEpochMillis ->
                        val atT = Ephemeris.julianCenturies(atEpochMillis)
                        Ephemeris.norm360(Ephemeris.moonLongitude(atT) - Ephemeris.lahiriAyanamsa(atT))
                    }
                val elongationAt: (Long) -> Double = { at ->
                    val atT = Ephemeris.julianCenturies(at)
                    Ephemeris.norm360(Ephemeris.moonLongitude(atT) - Ephemeris.sunApparentLongitude(atT))
                }
                val sunSiderealAt: (Long) -> Double = { at ->
                    val atT = Ephemeris.julianCenturies(at)
                    Ephemeris.norm360(Ephemeris.sunApparentLongitude(atT) - Ephemeris.lahiriAyanamsa(atT))
                }
                val maasa = maasaOf(epochMillis, elongationAt, sunSiderealAt)
                val samvatsara = samvatsaraOf(epochMillis, elongationAt, sunSiderealAt)

                AppResult.Success(
                    AstronomySnapshot(
                        instant = instant,
                        location = location,
                        sunTimes = sunTimes,
                        moonTimes = moonTimes,
                        tithi = tithiOf(elongation),
                        nakshatra = nakshatraOf(moonSidereal),
                        moonRasi = rasiOf(moonSidereal),
                        moonPada = AngularBuckets.pada(moonSidereal),
                        sunRasi = rasiOf(sunSidereal),
                        yoga = yogaOf(yogaSum),
                        karana = karanaOf(elongation),
                        vara = vara,
                        maasa = maasa,
                        samvatsara = samvatsara,
                        ayana = ayanaOf(sunSidereal),
                        ritu = rituOf(sunSidereal),
                        moonPhase = moonPhaseOf(elongation),
                        goldenHour = goldenHour,
                        muhurtas = muhurtasOf(sunTimes, vara.ordinal) + varjyam,
                        choghadiya = choghadiyaOf(sunTimes, nextSunrise, vara.ordinal),
                        limbs = limbWindowsAt(epochMillis, sunTimes.sunrise, nextSunrise),
                    ),
                )
            }
        }

        override suspend fun panchangaNowAt(instant: Instant): AppResult<PanchangaNow> =
            withContext(dispatchers.default) {
                val epochMillis = instant.toEpochMilliseconds()
                val t = Ephemeris.julianCenturies(epochMillis)
                val sunLongitude = Ephemeris.sunApparentLongitude(t)
                val moonLongitude = Ephemeris.moonLongitude(t)
                val ayanamsa = Ephemeris.lahiriAyanamsa(t)
                val elongation = Ephemeris.norm360(moonLongitude - sunLongitude)
                val sunSidereal = Ephemeris.norm360(sunLongitude - ayanamsa)
                val moonSidereal = Ephemeris.norm360(moonLongitude - ayanamsa)

                AppResult.Success(
                    PanchangaNow(
                        instant = instant,
                        tithi = tithiOf(elongation),
                        nakshatra = nakshatraOf(moonSidereal),
                        yoga = yogaOf(Ephemeris.norm360(sunSidereal + moonSidereal)),
                        karana = karanaOf(elongation),
                        limbs = limbWindowsAt(epochMillis, sunrise = null, nextSunrise = null),
                    ),
                )
            }

        override suspend fun upcomingFestivals(
            instant: Instant,
            location: GeoCoordinates,
            withinDays: Int,
            limit: Int,
        ): AppResult<List<Festival>> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                val source = ephemerisFestivalSource(location)
                AppResult.Success(upcomingFestivals(instant.toEpochMilliseconds(), withinDays, limit, source))
            }
        }

        override suspend fun nextTithiOccurrence(
            instant: Instant,
            location: GeoCoordinates,
            maasa: String?,
            tithis: Set<Int>,
            withinDays: Int,
        ): AppResult<Instant?> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                val source = ephemerisFestivalSource(location)
                val sunriseMillis =
                    nextTithiOccurrence(instant.toEpochMilliseconds(), withinDays, maasa, tithis, source)
                AppResult.Success(sunriseMillis?.let { Instant.fromEpochMilliseconds(it) })
            }
        }

        override suspend fun festivalOn(
            instant: Instant,
            location: GeoCoordinates,
        ): AppResult<String?> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                AppResult.Success(festivalOn(instant.toEpochMilliseconds(), ephemerisFestivalSource(location)))
            }
        }

        override suspend fun planetaryPositionsAt(instant: Instant): AppResult<PlanetaryPositions> =
            withContext(dispatchers.default) {
                AppResult.Success(planetaryPositions(instant.toEpochMilliseconds()))
            }

        override suspend fun natalChartAt(
            instant: Instant,
            location: GeoCoordinates,
        ): AppResult<NatalChart?> =
            withContext(dispatchers.default) {
                val chart = natalChart(instant.toEpochMilliseconds(), location.latitude, location.longitude)
                AppResult.Success<NatalChart?>(chart)
            }

        override suspend fun sunriseAt(
            instant: Instant,
            location: GeoCoordinates,
        ): AppResult<Instant?> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                AppResult.Success(
                    SolarDay
                        .sunTimes(instant.toEpochMilliseconds(), location.latitude, location.longitude)
                        .sunrise,
                )
            }
        }

        override suspend fun bestMuhurtasFor(
            activity: MuhurtaActivity,
            instant: Instant,
            days: Int,
            location: GeoCoordinates,
            person: PersonalMuhurtaContext?,
        ): AppResult<List<RankedMuhurtaDay>> {
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            return withContext(dispatchers.default) {
                val startMillis = instant.toEpochMilliseconds()
                // Sample each day's identity at its own sunrise — the convention by which panchangas
                // name the day — then score the full snapshot there.
                val snapshots =
                    (0 until days).mapNotNull { offset ->
                        val dayMillis = startMillis + offset * 86_400_000L
                        val sunrise =
                            SolarDay
                                .sunTimes(dayMillis, location.latitude, location.longitude)
                                .sunrise
                                ?: Instant.fromEpochMilliseconds(dayMillis)
                        (snapshotAt(sunrise, location) as? AppResult.Success)?.data
                    }
                AppResult.Success(rankMuhurtaDays(activity, snapshots, person))
            }
        }

        override suspend fun rashiOutlook(
            rasiIndex: Int,
            instant: Instant,
            location: GeoCoordinates,
            person: PersonalMuhurtaContext?,
            days: Int,
        ): AppResult<RashiOutlook> {
            if (rasiIndex !in 0..11) {
                return AppResult.Failure(IllegalArgumentException("rasiIndex out of range: $rasiIndex"))
            }
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return AppResult.Failure(IllegalArgumentException("Coordinates out of range: $location"))
            }

            val span = days.coerceIn(1, 30)
            return withContext(dispatchers.default) {
                val startMillis = instant.toEpochMilliseconds()
                // Sample each day's Moon at its own sunrise — the convention by which panchangas name
                // the day — then grade it by Chandrabala (and Tarabala when personalised).
                val outlookDays =
                    (0 until span).mapNotNull { offset ->
                        val dayMillis = startMillis + offset * 86_400_000L
                        val sunrise =
                            SolarDay
                                .sunTimes(dayMillis, location.latitude, location.longitude)
                                .sunrise
                                ?: Instant.fromEpochMilliseconds(dayMillis)
                        val snapshot =
                            (snapshotAt(sunrise, location) as? AppResult.Success)?.data ?: return@mapNotNull null
                        val moonRasi = snapshot.moonRasi ?: return@mapNotNull null
                        val position = chandraPosition(moonRasi.index, rasiIndex)
                        val chandrabala = chandraStrength(position)
                        val tara = person?.let { taraBetween(snapshot.nakshatra.number, it.birthNakshatraNumber) }
                        RashiDay(
                            atSunrise = snapshot.instant,
                            moonRasi = moonRasi,
                            nakshatra = snapshot.nakshatra,
                            vara = snapshot.vara,
                            chandraPosition = position,
                            chandrabala = chandrabala,
                            tara = tara,
                            band = outlookBand(chandrabala, tara?.strength),
                        )
                    }
                val today = outlookDays.firstOrNull()
                if (today == null) {
                    AppResult.Failure(IllegalStateException("No outlook days could be computed"))
                } else {
                    AppResult.Success(
                        RashiOutlook(
                            rasi = Rasi(index = rasiIndex, name = RASHI_NAMES[rasiIndex]),
                            personalized = person != null,
                            today = today,
                            week = outlookDays,
                        ),
                    )
                }
            }
        }

        private fun ephemerisFestivalSource(location: GeoCoordinates): FestivalPanchangaSource =
            object : FestivalPanchangaSource {
                override fun sunrise(dayEpochMillis: Long): Long? =
                    SolarDay
                        .sunTimes(dayEpochMillis, location.latitude, location.longitude)
                        .sunrise
                        ?.toEpochMilliseconds()

                override fun tithiNumber(epochMillis: Long): Int {
                    val t = Ephemeris.julianCenturies(epochMillis)
                    val elongation = Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.sunApparentLongitude(t))
                    return tithiOf(elongation).number
                }

                override fun sunRashi(epochMillis: Long): Int {
                    val t = Ephemeris.julianCenturies(epochMillis)
                    val sunSidereal = Ephemeris.norm360(Ephemeris.sunApparentLongitude(t) - Ephemeris.lahiriAyanamsa(t))
                    return (sunSidereal / 30.0).toInt() % 12
                }

                override fun maasa(epochMillis: Long): Maasa {
                    val elongationAt: (Long) -> Double = { at ->
                        val atT = Ephemeris.julianCenturies(at)
                        Ephemeris.norm360(Ephemeris.moonLongitude(atT) - Ephemeris.sunApparentLongitude(atT))
                    }
                    val sunSiderealAt: (Long) -> Double = { at ->
                        val atT = Ephemeris.julianCenturies(at)
                        Ephemeris.norm360(Ephemeris.sunApparentLongitude(atT) - Ephemeris.lahiriAyanamsa(atT))
                    }
                    return maasaOf(epochMillis, elongationAt, sunSiderealAt)
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
