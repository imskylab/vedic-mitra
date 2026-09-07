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
//
// [dayRule] is how the day was chosen, carried through to the model so a screen can say so. It is
// SUNRISE_TITHI for almost everything; the three exceptions are the festivals a reader's almanac is
// most likely to place a day either side of ours, and they are named rather than lumped together
// because "traditionally timed to midnight" and "to pradosh" are different facts.
private data class FestivalRule(
    val kind: FestivalKind,
    val maasa: String,
    val tithi: Int,
    val dayRule: DayRule = DayRule.SUNRISE_TITHI,
) {
    val name: String get() = kind.label
}

// Major festivals as amanta maasa + tithi rules, keyed off the tithi prevailing at sunrise. Krishna
// tithis are 15 + their number in the fortnight (Ashtami = 23, Chaturdashi = 29, Amavasya = 30).
// Cross-checked against published 2026 dates; timing-sensitive ones (Janmashtami/midnight,
// Shivaratri/nishita, Diwali/pradosh) use the same sunrise-tithi rule and may differ by a day from
// almanacs that time them to night — see docs/adr/0008.
private val FESTIVAL_RULES =
    listOf(
        FestivalRule(FestivalKind.UGADI, "Chaitra", 1),
        FestivalRule(FestivalKind.RAMA_NAVAMI, "Chaitra", 9),
        FestivalRule(FestivalKind.AKSHAYA_TRITIYA, "Vaishakha", 3),
        FestivalRule(FestivalKind.BUDDHA_PURNIMA, "Vaishakha", 15),
        FestivalRule(FestivalKind.GURU_PURNIMA, "Ashadha", 15),
        FestivalRule(FestivalKind.RAKSHA_BANDHAN, "Shravana", 15),
        FestivalRule(FestivalKind.KRISHNA_JANMASHTAMI, "Shravana", 23, DayRule.NIGHT_MIDNIGHT),
        FestivalRule(FestivalKind.GANESH_CHATURTHI, "Bhadrapada", 4),
        FestivalRule(FestivalKind.NAVARATRI, "Ashwina", 1),
        FestivalRule(FestivalKind.VIJAYADASHAMI, "Ashwina", 10),
        FestivalRule(FestivalKind.DIWALI, "Kartika", 30, DayRule.NIGHT_PRADOSH),
        FestivalRule(FestivalKind.MAHA_SHIVARATRI, "Magha", 29, DayRule.NIGHT_NISHITA),
        FestivalRule(FestivalKind.HOLI, "Phalguna", 15),
    )

// Rashi names for naming a Sankranti (e.g. "Makara Sankranti") come from the shared RASHI_NAMES.

private const val DAY_MILLIS = 86_400_000L

// Fallback "sunrise" (06:00 into the day) used only when the real sunrise is unavailable (polar).
private const val FALLBACK_SUNRISE_OFFSET_MILLIS = 21_600_000L

/**
 * Finds up to [limit] upcoming festivals and observances within [windowDays] of [fromEpochMillis],
 * in date order. Each civil day is judged by its **sunrise** panchanga and can contribute all three
 * kinds independently: a named festival if the amanta month and tithi match a rule, a recurring
 * observance if the tithi is one (Ekadashi/Purnima/Amavasya), and a Sankranti whenever the Sun has
 * entered a new rashi since the previous day. Each name appears at most once (its next occurrence).
 *
 * **A named festival does not displace the observance sharing its tithi**, so Shravana Purnima
 * yields both "Raksha Bandhan" and "Purnima". It used to, which was right while this fed one
 * combined list and wrong once the UI split festivals and observances onto separate screens: the
 * suppression then deleted a row from the Events screen because of a row on the Festivals screen,
 * leaving the reader with the *next* month's Purnima. Contrast [festivalOn], which still names
 * exactly one thing per day and should.
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
            addUnique(
                results,
                seen,
                sankrantiKind(rashi),
                "${RASHI_NAMES[rashi]} Sankranti",
                sunrise,
                FestivalType.SANKRANTI,
                DayRule.SUNRISE_INGRESS,
            )
        }
        previousRashi = rashi

        namedFestivalAt(tithi) { source.maasa(sunrise) }
            ?.let { addUnique(results, seen, it.kind, it.name, sunrise, FestivalType.FESTIVAL, it.dayRule) }
        FestivalKind.observanceAt(tithi)?.let {
            addUnique(results, seen, it, it.label, sunrise, FestivalType.OBSERVANCE)
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
): FestivalRule? {
    val candidates = FESTIVAL_RULES.filter { it.tithi == tithi }
    if (candidates.isEmpty()) return null
    val maasa = maasaProvider()
    if (maasa.adhika) return null
    return candidates.firstOrNull { it.maasa == maasa.name }
}

/**
 * The single most notable entry on the civil day containing [dayEpochMillis] — a named festival,
 * else a recurring observance, else a Sankranti — judged by that day's sunrise panchanga, or `null`
 * for an ordinary day. Used to highlight days in the calendar.
 *
 * **The precedence here is deliberate and differs from [upcomingFestivals]**, which emits every
 * match. A one-line day badge has room for one name and should read "Raksha Bandhan", not
 * "Purnima"; a list feeding two separate screens must not let one screen's entry suppress the
 * other's. Same day, two questions, two answers.
 */
internal fun festivalOn(
    dayEpochMillis: Long,
    source: FestivalPanchangaSource,
): Festival? {
    val sunrise = source.sunrise(dayEpochMillis) ?: (dayEpochMillis + FALLBACK_SUNRISE_OFFSET_MILLIS)
    val tithi = source.tithiNumber(sunrise)
    val at = Instant.fromEpochMilliseconds(sunrise)

    // Returns the whole Festival rather than its name: the calendar asserts "this day is Diwali",
    // which is the same claim the lists make and needs the same convention attached to it. Resolving
    // a bare name back to its rule would mean a lookup keyed on display copy, which is exactly what
    // knowledge-standards.md forbids.
    namedFestivalAt(tithi) { source.maasa(sunrise) }?.let {
        return Festival(it.kind, it.name, at, FestivalType.FESTIVAL, it.dayRule)
    }
    FestivalKind.observanceAt(tithi)?.let {
        return Festival(it, it.label, at, FestivalType.OBSERVANCE, DayRule.SUNRISE_TITHI)
    }
    return sankrantiOn(dayEpochMillis, sunrise, source)?.let { name ->
        val rashi = source.sunRashi(sunrise)
        Festival(sankrantiKind(rashi), name, at, FestivalType.SANKRANTI, DayRule.SUNRISE_INGRESS)
    }
}

/** Makara is called out separately; the other eleven rashis share one kind and one blurb. */
private fun sankrantiKind(rashi: Int): FestivalKind =
    if (RASHI_NAMES[rashi] == "Makara") FestivalKind.MAKARA_SANKRANTI else FestivalKind.SANKRANTI

/** "<Rashi> Sankranti" if the Sun entered a new rashi at [sunrise] versus the previous day, else null. */
private fun sankrantiOn(
    dayEpochMillis: Long,
    sunrise: Long,
    source: FestivalPanchangaSource,
): String? {
    val previousSunrise =
        source.sunrise(dayEpochMillis - DAY_MILLIS)
            ?: (dayEpochMillis - DAY_MILLIS + FALLBACK_SUNRISE_OFFSET_MILLIS)
    val rashi = source.sunRashi(sunrise)
    return if (rashi != source.sunRashi(previousSunrise)) "${RASHI_NAMES[rashi]} Sankranti" else null
}

private fun addUnique(
    results: MutableList<Festival>,
    seen: MutableSet<String>,
    kind: FestivalKind,
    name: String,
    sunriseMillis: Long,
    type: FestivalType,
    dayRule: DayRule = DayRule.SUNRISE_TITHI,
) {
    // Deduped on the name rather than the kind: eleven rashis share FestivalKind.SANKRANTI, and
    // suppressing all but the first would drop every Sankranti after the year's first one.
    if (seen.add(name)) {
        results.add(
            Festival(
                kind = kind,
                name = name,
                atSunrise = Instant.fromEpochMilliseconds(sunriseMillis),
                type = type,
                dayRule = dayRule,
            ),
        )
    }
}
