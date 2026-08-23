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

/**
 * A named combination present in a birth chart.
 *
 * Distinct from [Yoga], which is the panchanga's daily Sun–Moon yoga — same word, unrelated idea.
 *
 * @property name the classical name.
 * @property rule what had to be true for it to appear, so a reader can check the claim.
 * @property summary what the combination is traditionally said to indicate.
 */
data class ChartYoga(
    val name: String,
    val rule: String,
    val summary: String,
)

/**
 * The yogas this engine can decide, from a chart's sign, house and lordship placements.
 *
 * **Deliberately a short list.** Classical texts name hundreds, and most need aspects (drishti),
 * dignity beyond own-sign and exaltation, or divisional charts — none of which this engine has. The
 * rules below are the ones that follow from where the grahas sit, so each is checkable by eye
 * against the chart pages, and each carries the [ChartYoga.rule] that produced it rather than
 * asserting a result the reader cannot verify.
 *
 * Yogas are claims people repeat about themselves, so a wrong one is worse than a missing one.
 */
internal fun chartYogasOf(chart: NatalChart): List<ChartYoga> {
    val byGraha = chart.grahas.associateBy { it.graha }
    val moon = byGraha.getValue(Graha.MOON)
    val sun = byGraha.getValue(Graha.SUN)
    return buildList {
        gajakesari(byGraha, moon)?.let(::add)
        conjunction(byGraha, Graha.SUN, Graha.BUDHA, BUDHADITYA)?.let(::add)
        conjunction(byGraha, Graha.MOON, Graha.MANGALA, CHANDRA_MANGALA)?.let(::add)
        addAll(panchamahapurusha(byGraha, chart.lagna.rasi.index))
        addAll(lunarAttendants(chart.grahas, moon))
        addAll(solarAttendants(chart.grahas, sun))
    }
}

/** Jupiter in a kendra (1st, 4th, 7th or 10th) from the Moon. */
private fun gajakesari(
    byGraha: Map<Graha, NatalGraha>,
    moon: NatalGraha,
): ChartYoga? {
    val guru = byGraha.getValue(Graha.GURU)
    return if (houseFrom(guru, moon) in KENDRA) {
        ChartYoga(
            name = "Gajakesari",
            rule = "Guru is in the ${ordinal(houseFrom(guru, moon))} from the Moon, a kendra.",
            summary = "Traditionally read as lasting standing, good judgement and support from elders.",
        )
    } else {
        null
    }
}

/** Two grahas sharing a rashi. */
private fun conjunction(
    byGraha: Map<Graha, NatalGraha>,
    first: Graha,
    second: Graha,
    template: ChartYoga,
): ChartYoga? {
    val a = byGraha.getValue(first)
    val b = byGraha.getValue(second)
    return if (a.rasi.index == b.rasi.index) {
        template.copy(rule = "${a.graha.displayName} and ${b.graha.displayName} share ${a.rasi.name}.")
    } else {
        null
    }
}

/**
 * The five Panchamahapurusha yogas: one of the five non-luminaries in its own sign or exaltation,
 * *and* in a kendra from the lagna. Both conditions are required; either alone is not the yoga.
 */
private fun panchamahapurusha(
    byGraha: Map<Graha, NatalGraha>,
    lagnaRasiIndex: Int,
): List<ChartYoga> =
    PANCHAMAHAPURUSHA.mapNotNull { (graha, name) ->
        val placement = byGraha.getValue(graha)
        val house = ((placement.rasi.index - lagnaRasiIndex + RASHI_COUNT) % RASHI_COUNT) + 1
        val ownSign = RASI_LORD[placement.rasi.index] == graha
        val exalted = EXALTATION[graha] == placement.rasi.index
        if (house in KENDRA && (ownSign || exalted)) {
            ChartYoga(
                name = name,
                rule =
                    "${graha.displayName} is ${if (exalted) "exalted" else "in its own sign"} in " +
                        "${placement.rasi.name}, in the ${ordinal(house)} house.",
                summary = "One of the five Panchamahapurusha yogas, read as a marked strength of character.",
            )
        } else {
            null
        }
    }

/**
 * Sunapha, Anapha, Durudhara and Kemadruma — what sits either side of the Moon.
 *
 * The Sun and the nodes are excluded from the count, which is the usual convention: the Sun is never
 * far from the Moon's neighbouring signs, and the nodes are shadows rather than bodies.
 */
private fun lunarAttendants(
    grahas: List<NatalGraha>,
    moon: NatalGraha,
): List<ChartYoga> {
    val companions = grahas.filter { it.graha !in NON_ATTENDANTS && it.graha != Graha.MOON }
    val second = companions.filter { houseFrom(it, moon) == SECOND }
    val twelfth = companions.filter { houseFrom(it, moon) == TWELFTH }
    val withMoon = companions.filter { it.rasi.index == moon.rasi.index }
    return when {
        second.isNotEmpty() && twelfth.isNotEmpty() ->
            listOf(attendant("Durudhara", "both the 2nd and the 12th from the Moon", second + twelfth))

        second.isNotEmpty() -> listOf(attendant("Sunapha", "the 2nd from the Moon", second))
        twelfth.isNotEmpty() -> listOf(attendant("Anapha", "the 12th from the Moon", twelfth))
        withMoon.isEmpty() ->
            listOf(
                ChartYoga(
                    name = "Kemadruma",
                    rule = "No graha sits with the Moon, or in the 2nd or 12th from it.",
                    summary = "The Moon stands unattended — traditionally a caution rather than a promise.",
                ),
            )

        else -> emptyList()
    }
}

/** Vesi, Vasi and Ubhayachari — the same idea measured from the Sun, with the Moon excluded. */
private fun solarAttendants(
    grahas: List<NatalGraha>,
    sun: NatalGraha,
): List<ChartYoga> {
    val companions = grahas.filter { it.graha !in NON_ATTENDANTS && it.graha != Graha.MOON && it.graha != Graha.SUN }
    val second = companions.filter { houseFrom(it, sun) == SECOND }
    val twelfth = companions.filter { houseFrom(it, sun) == TWELFTH }
    return when {
        second.isNotEmpty() && twelfth.isNotEmpty() ->
            listOf(attendant("Ubhayachari", "both the 2nd and the 12th from the Sun", second + twelfth))

        second.isNotEmpty() -> listOf(attendant("Vesi", "the 2nd from the Sun", second))
        twelfth.isNotEmpty() -> listOf(attendant("Vasi", "the 12th from the Sun", twelfth))
        else -> emptyList()
    }
}

private fun attendant(
    name: String,
    where: String,
    grahas: List<NatalGraha>,
): ChartYoga =
    ChartYoga(
        name = name,
        rule = "${grahas.joinToString { it.graha.displayName }} in $where.",
        summary = "A graha attending the luminary, read as steadying its significations.",
    )

/** Which house [graha] falls in, counted from [from]'s rashi. 1..12. */
private fun houseFrom(
    graha: NatalGraha,
    from: NatalGraha,
): Int = ((graha.rasi.index - from.rasi.index + RASHI_COUNT) % RASHI_COUNT) + 1

private fun ordinal(house: Int): String =
    when (house) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${house}th"
    }

private const val RASHI_COUNT = 12
private const val SECOND = 2
private const val TWELFTH = 12
private val KENDRA = setOf(1, 4, 7, 10)

/** The Sun and the nodes do not count as attendants of the Moon. */
private val NON_ATTENDANTS = setOf(Graha.SUN, Graha.RAHU, Graha.KETU)

/** Exaltation rashi index per graha, classical. */
private val EXALTATION =
    mapOf(
        Graha.SUN to 0,
        Graha.MOON to 1,
        Graha.MANGALA to 9,
        Graha.BUDHA to 5,
        Graha.GURU to 3,
        Graha.SHUKRA to 11,
        Graha.SHANI to 6,
    )

private val PANCHAMAHAPURUSHA =
    listOf(
        Graha.MANGALA to "Ruchaka",
        Graha.BUDHA to "Bhadra",
        Graha.GURU to "Hamsa",
        Graha.SHUKRA to "Malavya",
        Graha.SHANI to "Sasa",
    )

private val BUDHADITYA =
    ChartYoga(
        name = "Budhaditya",
        rule = "",
        summary = "The Sun with Mercury — traditionally read as a quick and articulate mind.",
    )

private val CHANDRA_MANGALA =
    ChartYoga(
        name = "Chandra-Mangala",
        rule = "",
        summary = "The Moon with Mars — read as drive and an instinct for enterprise.",
    )
