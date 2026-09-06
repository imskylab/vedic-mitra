/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.ui.panchanga

import androidx.annotation.StringRes
import io.github.vedicmitra.core.astronomy.DayRule
import io.github.vedicmitra.core.ui.R

/**
 * How a festival's day was chosen, in words.
 *
 * Every screen that prints a festival date shows one of these. It is not a disclaimer and should not
 * read as one — the date is not in doubt, the *rule* behind it is one of several, and a reader whose
 * almanac disagrees is owed the means to tell which.
 */
@get:StringRes
val DayRule.explanationRes: Int
    get() =
        when (this) {
            DayRule.SUNRISE_TITHI -> R.string.festival_rule_sunrise_tithi
            DayRule.NIGHT_MIDNIGHT -> R.string.festival_rule_night_midnight
            DayRule.NIGHT_NISHITA -> R.string.festival_rule_night_nishita
            DayRule.NIGHT_PRADOSH -> R.string.festival_rule_night_pradosh
            DayRule.SUNRISE_INGRESS -> R.string.festival_rule_sunrise_ingress
        }

/**
 * The standing line under a list of festival dates.
 *
 * Shown untapped, on the same reasoning as a primer one-liner: a convention that only appears when
 * something is tapped is a convention most readers never meet. ADR 0017 settled the same question
 * for the month scheme — named on every reading, not only where it is contested — because the reader
 * who disagrees with the app is exactly the reader who would never think to look in Settings.
 */
@get:StringRes
val festivalDatesNoteRes: Int
    get() = R.string.festival_rule_list_note
