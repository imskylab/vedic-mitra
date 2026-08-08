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

package io.github.vedicmitra.feature.alarm

/** Reminder-key prefix for tithi (lunar-day) reminders, e.g. `tithi:*:30` or `tithi:Kartika:30`. */
const val TITHI_PREFIX = "tithi:"

/** The waxing/waning fortnight chosen in the picker; [EITHER] targets the tithi in both. */
enum class PickPaksha {
    SHUKLA,
    KRISHNA,
    EITHER,
}

/** The twelve amanta lunar months, in order, for the "which month" picker. */
val MAASA_OPTIONS =
    listOf(
        "Chaitra",
        "Vaishakha",
        "Jyeshtha",
        "Ashadha",
        "Shravana",
        "Bhadrapada",
        "Ashwina",
        "Kartika",
        "Margashirsha",
        "Pausha",
        "Magha",
        "Phalguna",
    )

// Tithi names 1..14 within a fortnight; the 15th is Purnima (Shukla) or Amavasya (Krishna).
private val TITHI_NAMES =
    listOf(
        "Pratipada",
        "Dwitiya",
        "Tritiya",
        "Chaturthi",
        "Panchami",
        "Shashthi",
        "Saptami",
        "Ashtami",
        "Navami",
        "Dashami",
        "Ekadashi",
        "Dwadashi",
        "Trayodashi",
        "Chaturdashi",
    )

private const val FORTNIGHT = 15

/**
 * A tithi-based reminder target: which global tithis (1..30; 1..15 Shukla, 16..30 Krishna) to fire
 * on, optionally pinned to a [maasa]. A `null` [maasa] recurs every lunar month; a specific month is
 * (roughly) annual. [tithis] is a set so one reminder can span both fortnights — e.g. Ekadashi.
 */
data class TithiTarget(
    val maasa: String?,
    val tithis: Set<Int>,
) {
    /** The self-describing reminder key, e.g. `tithi:*:11,26` or `tithi:Kartika:30`. */
    val key: String
        get() = "$TITHI_PREFIX${maasa ?: "*"}:${tithis.sorted().joinToString(",")}"

    /** How the reminder recurs, for display: the month name, or "Every month". */
    val recurrence: String
        get() = maasa ?: "Every month"

    /** The event's display name, e.g. "Amavasya", "Ekadashi", or "Shukla Panchami". */
    val eventName: String
        get() = eventNameFor(tithis)

    companion object {
        /** Parses a `tithi:` reminder key back into a target, or `null` if it isn't one/is malformed. */
        fun fromKey(key: String): TithiTarget? {
            if (!key.startsWith(TITHI_PREFIX)) return null
            val rest = key.removePrefix(TITHI_PREFIX)
            val separator = rest.indexOf(':')
            if (separator < 0) return null
            val tithis =
                rest
                    .substring(separator + 1)
                    .split(",")
                    .mapNotNull { it.toIntOrNull() }
                    .toSet()
            if (tithis.isEmpty()) return null
            return TithiTarget(maasa = rest.substring(0, separator).takeIf { it != "*" }, tithis = tithis)
        }

        /** Builds a target from the custom picker: a [paksha] fortnight and a 1..15 [tithi]. */
        fun custom(
            maasa: String?,
            paksha: PickPaksha,
            tithi: Int,
        ): TithiTarget {
            val tithis =
                when (paksha) {
                    PickPaksha.SHUKLA -> setOf(tithi)
                    PickPaksha.KRISHNA -> setOf(FORTNIGHT + tithi)
                    PickPaksha.EITHER -> setOf(tithi, FORTNIGHT + tithi)
                }
            return TithiTarget(maasa, tithis)
        }
    }
}

/** The popular one-tap presets shown at the top of the picker (all recur every month). */
val TITHI_PRESETS =
    listOf(
        TithiTarget(maasa = null, tithis = setOf(30)), // Amavasya
        TithiTarget(maasa = null, tithis = setOf(15)), // Purnima
        TithiTarget(maasa = null, tithis = setOf(11, 26)), // Ekadashi
        TithiTarget(maasa = null, tithis = setOf(19)), // Sankashti Chaturthi (Krishna Chaturthi)
        TithiTarget(maasa = null, tithis = setOf(13, 28)), // Pradosh (Trayodashi)
        TithiTarget(maasa = null, tithis = setOf(29)), // Masik Shivaratri (Krishna Chaturdashi)
    )

/** Human name for a set of global tithis: a known observance if it matches, else the raw name(s). */
private fun eventNameFor(tithis: Set<Int>): String =
    when (tithis.sorted()) {
        listOf(30) -> "Amavasya"
        listOf(15) -> "Purnima"
        listOf(11, 26) -> "Ekadashi"
        listOf(19) -> "Sankashti Chaturthi"
        listOf(13, 28) -> "Pradosh"
        listOf(29) -> "Shivaratri"
        else -> tithis.sorted().joinToString(" / ", transform = ::globalTithiName)
    }

/** The name of a single global tithi 1..30, e.g. 5 → "Shukla Panchami", 30 → "Amavasya". */
private fun globalTithiName(global: Int): String {
    val shukla = global <= FORTNIGHT
    val index = if (shukla) global - 1 else global - (FORTNIGHT + 1)
    return when {
        index == FORTNIGHT - 1 && shukla -> "Purnima"
        index == FORTNIGHT - 1 -> "Amavasya"
        shukla -> "Shukla ${TITHI_NAMES[index]}"
        else -> "Krishna ${TITHI_NAMES[index]}"
    }
}

/** The 1..15 tithi labels for the custom picker (15 shown as Purnima or Amavasya per [shukla]). */
fun pickerTithiLabels(shukla: Boolean): List<String> = TITHI_NAMES + if (shukla) "Purnima" else "Amavasya"
