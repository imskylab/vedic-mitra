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

/** What kind of entry a [Festival] is, so the UI can group or style them. */
enum class FestivalType {
    /** A named festival (Diwali, Holi, Ganesh Chaturthi, …). */
    FESTIVAL,

    /** A recurring lunar observance (Ekadashi, Purnima, Amavasya). */
    OBSERVANCE,

    /** A solar month transition (the Sun entering a new rashi). */
    SANKRANTI,
}

/**
 * How the app decided which civil day a [Festival] falls on.
 *
 * **This is not arithmetic.** Where a tithi spans two sunrises, which day carries the festival is a
 * *nirnaya* question settled by rule, and traditions answer it differently. The app answers it one
 * way; carrying the answer on the model is what lets a screen say so, instead of printing a date
 * that looks as derived as a tithi boundary and is not.
 *
 * The reasoning is in `docs/knowledge-standards.md` under "A fixed date is not a chosen one", and
 * the precedent is [ADR 0017](../../../../../../../docs/adr/0017-purnimanta-month-naming.md): the
 * month-scheme calculation was right and the silence around it was the fault.
 *
 * Identities, not copy — the wording lives in `:core:ui`.
 */
enum class DayRule {
    /** The tithi prevailing at sunrise carries the day. The ordinary case. */
    SUNRISE_TITHI,

    /** Judged at sunrise here, but traditionally timed to **midnight** — Krishna Janmashtami. */
    NIGHT_MIDNIGHT,

    /** Judged at sunrise here, but traditionally timed to **nishita**, the middle of the night. */
    NIGHT_NISHITA,

    /** Judged at sunrise here, but traditionally timed to **pradosh**, the hour after sunset. */
    NIGHT_PRADOSH,

    /** The day on which the Sun entered a new rashi, detected between successive sunrises. */
    SUNRISE_INGRESS,
    ;

    /**
     * Whether this festival is traditionally timed to a moment of night, and so is the kind most
     * likely to differ by a day from a reader's almanac.
     */
    val isNightTimed: Boolean
        get() = this == NIGHT_MIDNIGHT || this == NIGHT_NISHITA || this == NIGHT_PRADOSH
}

/**
 * A festival or observance falling on a particular day, as computed from the panchanga.
 *
 * @property kind which festival this is, and the only thing anything durable may refer to it by.
 * @property name the display name (e.g. "Diwali", "Ekadashi", "Makara Sankranti"). Usually
 *   [FestivalKind.label]; a Sankranti is named for its rashi, which the kind does not carry.
 * @property atSunrise the instant of sunrise on the festival day — the UI formats this to a local
 *   date in the location's time zone.
 * @property type what kind of entry this is.
 * @property dayRule how the day was chosen. Part of the claim, not metadata: a screen that shows
 *   [atSunrise] without it asserts more certainty than the app has.
 */
data class Festival(
    val kind: FestivalKind,
    val name: String,
    val atSunrise: Instant,
    val type: FestivalType,
    val dayRule: DayRule,
)
