/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.muhurat

import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.MuhurtaPerson
import io.github.vedicmitra.core.astronomy.PersonalMuhurtaContext
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import java.time.ZoneId
import kotlin.time.Instant

/**
 * A profile's birth chart, reduced to the two numbers muhurta scoring reads: the birth nakshatra and
 * the birth Moon sign.
 *
 * Shared by both muhurta screens. The ranked list needs it to score days, and the day detail needs it
 * to say where that day stands — and casting the same chart two slightly different ways is how the
 * two would drift apart.
 *
 * Returns null rather than failing loudly, for the same reason every caller filters on
 * `isChartReady` first: a profile that cannot be cast is one the user has not finished entering, and
 * the screens above treat that as "not personalised" rather than as an error.
 *
 * Note the wider duplication this does *not* fix: `birthMomentOf` is copy-pasted across kundali,
 * rashifal, matchmaking and japa too. Pulling it into a shared use case would touch five features and
 * belongs in its own change.
 */
internal suspend fun AstronomyEngine.muhurtaPersonFor(profile: BirthProfile): MuhurtaPerson? {
    val birth = birthMomentOf(profile) ?: return null
    val chart = (natalChartAt(birth.first, birth.second) as? AppResult.Success)?.data ?: return null
    val moonRasiIndex =
        chart.grahas
            .firstOrNull { it.graha == Graha.MOON }
            ?.rasi
            ?.index ?: return null
    return MuhurtaPerson(
        id = profile.id,
        context =
            PersonalMuhurtaContext(
                birthNakshatraNumber = chart.moonNakshatra.number,
                birthMoonRasiIndex = moonRasiIndex,
            ),
    )
}

/** The birth instant and birthplace for [profile], or null when any part of it is missing. */
private fun birthMomentOf(profile: BirthProfile): Pair<Instant, GeoCoordinates>? {
    val date = profile.dateOfBirth
    val time = profile.timeOfBirth
    val zone = profile.birthZoneId
    val coordinates = profile.birthCoordinates
    if (date == null || time == null || zone == null || coordinates == null) return null
    val millis =
        date
            .atTime(time)
            .atZone(ZoneId.of(zone))
            .toInstant()
            .toEpochMilli()
    return Instant.fromEpochMilliseconds(millis) to coordinates
}
