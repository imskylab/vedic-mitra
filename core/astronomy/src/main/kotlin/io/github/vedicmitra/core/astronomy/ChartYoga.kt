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
 * **Deliberately a short list, and shortened further by measurement.** Classical texts name
 * hundreds, and most need aspects (drishti), dignity beyond own-sign and exaltation, or divisional
 * charts — none of which this engine has.
 *
 * The rules kept here were checked against an independent implementation over 75 charts:
 * Panchamahapurusha agreed on 75/75 and Gajakesari on 73/75. Budhaditya and Chandra-Mangala are true
 * by definition — two grahas sharing a rashi — and need no orbs or conventions to decide.
 *
 * The Sunapha/Anapha/Durudhara/Kemadruma and Vesi/Vasi/Ubhayachari families were **removed** after
 * that check: they agreed only 45% and 72% of the time. Most of the gap is convention rather than
 * arithmetic — whether the nodes count as attendants, and whether a graha lost in the Sun's glare
 * still attends — and even granting a perfect combustion oracle the lunar family only reached 81%.
 * A yoga is a claim a person repeats about themselves, so a rule that is wrong one time in four does
 * not belong on the page. They can return when the conventions are settled deliberately.
 */
internal fun chartYogasOf(chart: NatalChart): List<ChartYoga> {
    val byGraha = chart.grahas.associateBy { it.graha }
    val moon = byGraha.getValue(Graha.MOON)
    return buildList {
        gajakesari(byGraha, moon)?.let(::add)
        conjunction(byGraha, Graha.SUN, Graha.BUDHA, BUDHADITYA)?.let(::add)
        conjunction(byGraha, Graha.MOON, Graha.MANGALA, CHANDRA_MANGALA)?.let(::add)
        addAll(panchamahapurusha(byGraha, chart.lagna.rasi.index))
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
private val KENDRA = setOf(1, 4, 7, 10)

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
