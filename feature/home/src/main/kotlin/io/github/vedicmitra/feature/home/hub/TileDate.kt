/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home.hub

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A date as the [TileIcon.Today] tile draws it: [day] over [month], with [spoken] read aloud in
 * place of the two.
 *
 * @property spoken the whole date in one phrase. The two drawn lines are a layout, not a reading —
 *   a screen reader given them separately announces "8" and "SEP" as loose nodes, and an
 *   abbreviation in capitals is a coin-flip between "sep" and the letters spelled out.
 */
internal data class TileDate(
    val day: String,
    val month: String,
    val spoken: String,
)

/**
 * The three strings for [date].
 *
 * Pure and separate from the composable so it can be tested on the JVM, which is also why [locale]
 * is a parameter rather than read from the default inside.
 *
 * Not extracted to `strings.xml`: a formatted date is a computed value rather than copy (ADR 0021),
 * and it already follows the reader's language through the formatter — which is what the
 * localization work in #181 will want of it.
 */
internal fun tileDate(
    date: LocalDate,
    locale: Locale = Locale.getDefault(),
): TileDate =
    TileDate(
        day = date.dayOfMonth.toString(),
        // Uppercased against the same locale it was formatted in. Bare uppercase() would use the
        // default, which is the Turkish-i bug waiting to happen.
        month = monthFormatter.withLocale(locale).format(date).uppercase(locale),
        spoken = spokenFormatter.withLocale(locale).format(date),
    )

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
private val spokenFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
