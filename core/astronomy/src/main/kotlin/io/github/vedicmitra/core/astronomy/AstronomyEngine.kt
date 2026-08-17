/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlin.time.Instant

/**
 * Port for astronomical / panchanga calculations.
 *
 * Consumers (features, use cases) depend on this abstraction so the concrete ephemeris-backed
 * engine can be swapped and tested. [DefaultAstronomyEngine] provides the concrete,
 * ephemeris-backed implementation; this file declares the contract and the shape of
 * [AstronomySnapshot] and [PanchangaDaySummary].
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

    /**
     * Computes a lightweight [PanchangaDaySummary] for the given [instant] and [location] — just
     * the elements a calendar grid shows per day (tithi, nakshatra, moon phase). Deliberately skips
     * the expensive sunrise/moonrise/muhurta work in [snapshotAt] so a whole month's worth of days
     * can be computed cheaply; use [snapshotAt] for a single day's full detail.
     *
     * @return [AppResult.Success] with the summary, or [AppResult.Failure] if it cannot be computed.
     */
    suspend fun daySummaryAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<PanchangaDaySummary>

    /**
     * Finds up to [limit] upcoming festivals and observances within [withinDays] of [instant] for
     * [location], in date order, judged by each day's sunrise panchanga (see [Festival]).
     *
     * @return [AppResult.Success] with the (possibly empty) list, or [AppResult.Failure] if it
     *   cannot be computed.
     */
    suspend fun upcomingFestivals(
        instant: Instant,
        location: GeoCoordinates,
        withinDays: Int,
        limit: Int,
    ): AppResult<List<Festival>>

    /**
     * The next civil day, within [withinDays] of [instant] for [location], whose sunrise tithi is
     * one of [tithis] (global 1..30) and — when [maasa] is non-null — whose amanta month matches it.
     * Returns the day's **sunrise** instant, `null` if none falls in the window, or [AppResult.Failure]
     * if it cannot be computed. A `null` [maasa] recurs every lunar month; a specific month is annual.
     *
     * Used to schedule tithi-based reminders (Amavasya, Purnima, Ekadashi, …); the default no-op lets
     * test doubles ignore it.
     */
    suspend fun nextTithiOccurrence(
        instant: Instant,
        location: GeoCoordinates,
        maasa: String?,
        tithis: Set<Int>,
        withinDays: Int,
    ): AppResult<Instant?> = AppResult.Success<Instant?>(null)

    /**
     * The single most notable entry on the day containing [instant] for [location] — a named
     * festival, else a recurring observance, else a Sankranti — or `null` for an ordinary day. Used
     * to highlight days in the calendar. The default no-op lets test doubles ignore it.
     */
    suspend fun festivalOn(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<String?> = AppResult.Success<String?>(null)

    /**
     * The rashi of each tracked graha (Sun, Moon, Guru, Shukra) at [instant], each with its next
     * rashi ingress (pravesh). Geocentric, so it does not depend on the observer's location. The
     * default returns no positions so test doubles can ignore it.
     */
    suspend fun planetaryPositionsAt(instant: Instant): AppResult<PlanetaryPositions> =
        AppResult.Success(PlanetaryPositions(emptyList()))

    /**
     * The birth [NatalChart] for [instant] at [location] — the nine grahas (with house and
     * retrograde state), the ascendant, whole-sign houses, the Moon's nakshatra/pada, and the
     * Vimshottari mahadasha timeline. The default returns `null` so test doubles can ignore it.
     */
    suspend fun natalChartAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<NatalChart?> = AppResult.Success<NatalChart?>(null)

    /**
     * The sunrise instant of the civil day containing [instant] at [location], or `null` when the
     * sun does not rise that day (polar). Callers use this to anchor a day's panchanga identity
     * (tithi, nakshatra, …) to sunrise — the convention by which panchangas name the day — rather
     * than to an arbitrary time. The default no-op lets test doubles ignore it.
     */
    suspend fun sunriseAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<Instant?> = AppResult.Success<Instant?>(null)

    /**
     * The candidate days for [activity] over the [days] civil days starting at [instant], for
     * [location], each scored from that day's sunrise panchanga and returned best-first (see
     * `scoreMuhurta`). Used by the "find best dates" muhurta finder. The default returns an empty
     * list so test doubles can ignore it.
     */
    suspend fun bestMuhurtasFor(
        activity: MuhurtaActivity,
        instant: Instant,
        days: Int,
        location: GeoCoordinates,
    ): AppResult<List<RankedMuhurtaDay>> = AppResult.Success(emptyList<RankedMuhurtaDay>())
}

/**
 * A lightweight, cheap-to-compute panchanga summary for one day, for a month-grid overview.
 *
 * @property tithi the day's lunar day.
 * @property nakshatra the Moon's lunar mansion.
 * @property moonPhase the Moon's phase.
 */
data class PanchangaDaySummary(
    val tithi: Tithi,
    val nakshatra: Nakshatra,
    val moonPhase: MoonPhase,
)

/**
 * Immutable result of an astronomy computation for a single instant and location.
 *
 * @property instant the instant the snapshot was computed for.
 * @property location the observer's coordinates.
 * @property sunTimes sunrise/sunset for the location and local date.
 * @property moonTimes moonrise/moonset for the location and local date.
 * @property tithi the current lunar day.
 * @property nakshatra the Moon's current lunar mansion.
 * @property yoga the current Sun–Moon yoga.
 * @property karana the current karana (half-tithi).
 * @property vara the weekday (sunrise-to-sunrise).
 * @property maasa the amanta lunar month.
 * @property samvatsara the year in the sixty-year Jovian cycle.
 * @property ayana the Sun's current half-year sidereal journey (Uttarayana/Dakshinayana).
 * @property ritu the current season, by the Sun's sidereal longitude.
 * @property moonPhase the Moon's current phase.
 * @property goldenHour the day's golden-hour windows.
 * @property muhurtas the day's auspicious/inauspicious time windows.
 * @property choghadiya the day's sixteen Choghadiya windows (eight day, eight night); empty when
 *   the sun does not rise/set. Defaults to empty so lightweight/synthetic snapshots need not
 *   provide it — the real engine always populates it.
 */
data class AstronomySnapshot(
    val instant: Instant,
    val location: GeoCoordinates,
    val sunTimes: SunTimes,
    val moonTimes: MoonTimes,
    val tithi: Tithi,
    val nakshatra: Nakshatra,
    val yoga: Yoga,
    val karana: Karana,
    val vara: Vara,
    val maasa: Maasa,
    val samvatsara: Samvatsara,
    val ayana: Ayana,
    val ritu: Ritu,
    val moonPhase: MoonPhase,
    val goldenHour: GoldenHour,
    val muhurtas: List<Muhurta>,
    val choghadiya: List<Choghadiya> = emptyList(),
)
