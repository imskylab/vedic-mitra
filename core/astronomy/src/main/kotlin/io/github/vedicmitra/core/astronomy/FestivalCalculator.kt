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

import kotlin.time.Instant

// A named festival: the amanta [maasa] and the global tithi number (1..30) on which it falls.
private data class FestivalRule(
    val name: String,
    val maasa: String,
    val tithi: Int,
)

// Major festivals as amanta maasa + tithi rules, keyed off the tithi prevailing at sunrise. Krishna
// tithis are 15 + their number in the fortnight (Ashtami = 23, Chaturdashi = 29, Amavasya = 30).
// Cross-checked against published 2026 dates; timing-sensitive ones (Janmashtami/midnight,
// Shivaratri/nishita, Diwali/pradosh) use the same sunrise-tithi rule and may differ by a day from
// almanacs that time them to night — see docs/adr/0008.
private val FESTIVAL_RULES =
    listOf(
        FestivalRule("Ugadi / Gudi Padwa", "Chaitra", 1),
        FestivalRule("Rama Navami", "Chaitra", 9),
        FestivalRule("Akshaya Tritiya", "Vaishakha", 3),
        FestivalRule("Buddha Purnima", "Vaishakha", 15),
        FestivalRule("Guru Purnima", "Ashadha", 15),
        FestivalRule("Raksha Bandhan", "Shravana", 15),
        FestivalRule("Krishna Janmashtami", "Shravana", 23),
        FestivalRule("Ganesh Chaturthi", "Bhadrapada", 4),
        FestivalRule("Navaratri begins", "Ashwina", 1),
        FestivalRule("Vijayadashami", "Ashwina", 10),
        FestivalRule("Diwali", "Kartika", 30),
        FestivalRule("Maha Shivaratri", "Magha", 29),
        FestivalRule("Holi", "Phalguna", 15),
    )

// Rashi (zodiac sign) names, index 0 = Mesha, used to name a Sankranti (e.g. "Makara Sankranti").
private val RASHI_NAMES =
    listOf(
        "Mesha",
        "Vrishabha",
        "Mithuna",
        "Karka",
        "Simha",
        "Kanya",
        "Tula",
        "Vrishchika",
        "Dhanu",
        "Makara",
        "Kumbha",
        "Meena",
    )

private const val DAY_MILLIS = 86_400_000L

// Fallback "sunrise" (06:00 into the day) used only when the real sunrise is unavailable (polar).
private const val FALLBACK_SUNRISE_OFFSET_MILLIS = 21_600_000L

/**
 * Finds up to [limit] upcoming festivals and observances within [windowDays] of [fromEpochMillis],
 * in date order. Each civil day is judged by its **sunrise** panchanga: a named festival if the
 * amanta month and tithi match a rule, else a recurring observance (Ekadashi/Purnima/Amavasya), plus
 * a Sankranti whenever the Sun has entered a new rashi since the previous day. Each name appears at
 * most once (its next occurrence).
 */
internal fun upcomingFestivals(
    fromEpochMillis: Long,
    windowDays: Int,
    limit: Int,
    source: FestivalPanchangaSource,
): List<Festival> {
    val results = mutableListOf<Festival>()
    val seen = mutableSetOf<String>()
    var previousRashi: Int? = null

    var day = 0
    while (day < windowDays && results.size < limit) {
        val dayMillis = fromEpochMillis + day * DAY_MILLIS
        val sunrise = source.sunrise(dayMillis) ?: (dayMillis + FALLBACK_SUNRISE_OFFSET_MILLIS)
        val tithi = source.tithiNumber(sunrise)
        val rashi = source.sunRashi(sunrise)

        if (previousRashi != null && rashi != previousRashi) {
            addUnique(results, seen, "${RASHI_NAMES[rashi]} Sankranti", sunrise, FestivalType.SANKRANTI)
        }
        previousRashi = rashi

        val festivalName = namedFestivalAt(tithi) { source.maasa(sunrise) }
        if (festivalName != null) {
            addUnique(results, seen, festivalName, sunrise, FestivalType.FESTIVAL)
        } else {
            observanceAt(tithi)?.let { addUnique(results, seen, it, sunrise, FestivalType.OBSERVANCE) }
        }
        day++
    }

    return results.sortedBy { it.atSunrise }.take(limit)
}

/**
 * The sunrise instant (epoch millis) of the next civil day within [windowDays] of [fromEpochMillis]
 * whose sunrise tithi is one of [tithis] (global 1..30) and — when [maasa] is non-null — whose
 * amanta month name matches it, or `null` if no such day falls in the window.
 *
 * A `null` [maasa] makes the match recur every lunar month (any month with that tithi); a specific
 * month name pins it to that month's occurrence (roughly annual). [tithis] carries a set so a single
 * reminder can span both fortnights — e.g. Ekadashi is `{11, 26}`.
 */
internal fun nextTithiOccurrence(
    fromEpochMillis: Long,
    windowDays: Int,
    maasa: String?,
    tithis: Set<Int>,
    source: FestivalPanchangaSource,
): Long? {
    var day = 0
    while (day < windowDays) {
        val dayMillis = fromEpochMillis + day * DAY_MILLIS
        val sunrise = source.sunrise(dayMillis) ?: (dayMillis + FALLBACK_SUNRISE_OFFSET_MILLIS)
        if (source.tithiNumber(sunrise) in tithis && (maasa == null || source.maasa(sunrise).name == maasa)) {
            return sunrise
        }
        day++
    }
    return null
}

/**
 * The named festival on a day whose sunrise tithi is [tithi], or `null`. [maasaProvider] is only
 * consulted when some rule shares the tithi, so the (relatively expensive) month lookup is skipped
 * on the vast majority of days.
 */
private inline fun namedFestivalAt(
    tithi: Int,
    maasaProvider: () -> Maasa,
): String? {
    val candidates = FESTIVAL_RULES.filter { it.tithi == tithi }
    if (candidates.isEmpty()) return null
    val maasa = maasaProvider()
    if (maasa.adhika) return null
    return candidates.firstOrNull { it.maasa == maasa.name }?.name
}

/** The recurring observance for a sunrise tithi, or `null`. */
private fun observanceAt(tithi: Int): String? =
    when (tithi) {
        11, 26 -> "Ekadashi"
        15 -> "Purnima"
        30 -> "Amavasya"
        else -> null
    }

private fun addUnique(
    results: MutableList<Festival>,
    seen: MutableSet<String>,
    name: String,
    sunriseMillis: Long,
    type: FestivalType,
) {
    if (seen.add(name)) {
        results.add(Festival(name = name, atSunrise = Instant.fromEpochMilliseconds(sunriseMillis), type = type))
    }
}
