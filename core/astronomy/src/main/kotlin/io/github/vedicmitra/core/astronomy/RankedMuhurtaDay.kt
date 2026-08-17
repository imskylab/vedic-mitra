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

import kotlin.time.Instant

/**
 * One candidate day in a muhurta search: the day (anchored to its sunrise) and how well its
 * panchanga suits the chosen activity.
 *
 * @property atSunrise the sunrise instant of the day the score was computed for.
 * @property score the day's suitability for the activity, with its contributing reasons.
 */
data class RankedMuhurtaDay(
    val atSunrise: Instant,
    val score: DayMuhurtaScore,
)

/**
 * Scores each of the given [days] (their sunrise snapshots) for [activity] and returns them ordered
 * best-first — by descending [DayMuhurtaScore.score], ties broken by the earlier date. Pure: the
 * caller supplies the already-computed snapshots, so the ranking is independent of the ephemeris.
 */
internal fun rankMuhurtaDays(
    activity: MuhurtaActivity,
    days: List<AstronomySnapshot>,
): List<RankedMuhurtaDay> =
    days
        .map {
            RankedMuhurtaDay(
                atSunrise = it.instant,
                score = scoreMuhurta(activity, it.tithi, it.nakshatra, it.vara, it.yoga, it.karana),
            )
        }.sortedWith(compareByDescending<RankedMuhurtaDay> { it.score.score }.thenBy { it.atSunrise })
