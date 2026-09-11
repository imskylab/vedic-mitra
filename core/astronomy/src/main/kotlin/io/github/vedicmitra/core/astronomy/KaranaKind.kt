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

/**
 * Which karana a position in the lunar month falls in — its **identity**, as distinct from the name
 * printed for it.
 *
 * Eleven of them: seven *chara* (movable), which repeat eight times through the month, and four
 * *sthira* (fixed), which occur once each around the new moon.
 *
 * ## Why this exists
 *
 * The engine used to ask `karana.name == "Vishti"`. That is the repo's recorded failure — see
 * `docs/knowledge-standards.md`, "A claim is not its own key" — and the fifth instance of it: a rule
 * keyed on the string a screen displays, which fails *silently* the moment the spelling changes.
 * A karana's identity is its **position**, which is derived from the Moon's elongation and cannot be
 * renamed. [karanaKindAt] is the only mapping from one to the other, and [label] is derived from the
 * kind rather than the other way round.
 *
 * @property id frozen. Anything durable refers to a karana by this.
 * @property label the traditional Sanskrit name, as printed.
 * @property fixed whether this is one of the four *sthira* karanas, which occur once in the month.
 */
enum class KaranaKind(
    val id: String,
    val label: String,
    val fixed: Boolean = false,
) {
    BAVA("bava", "Bava"),
    BALAVA("balava", "Balava"),
    KAULAVA("kaulava", "Kaulava"),
    TAITILA("taitila", "Taitila"),
    GARA("gara", "Gara"),
    VANIJA("vanija", "Vanija"),
    VISHTI("vishti", "Vishti"),
    KIMSTUGHNA("kimstughna", "Kimstughna", fixed = true),
    SHAKUNI("shakuni", "Shakuni", fixed = true),
    CHATUSHPADA("chatushpada", "Chatushpada", fixed = true),
    NAGA("naga", "Naga", fixed = true),
    ;

    companion object {
        /** The seven movable karanas, in the order they repeat. */
        val movable: List<KaranaKind> = entries.filterNot { it.fixed }
    }
}

/**
 * The karana at position [number] (1..60) in the lunar month.
 *
 * Not a plain table lookup: the first position is Kimstughna, the next fifty-six cycle through the
 * seven movable karanas, and the last three are fixed.
 *
 * A consequence worth knowing when writing a test fixture by hand: **Vishti falls at positions 8, 15,
 * 22 … 57**, never at 7. A `Karana(number = 7, name = "Vishti")` describes a day that cannot happen.
 */
fun karanaKindAt(number: Int): KaranaKind {
    val index = number - 1
    return when {
        index == 0 -> KaranaKind.KIMSTUGHNA
        index <= LAST_MOVABLE_INDEX -> KaranaKind.movable[(index - 1) % KaranaKind.movable.size]
        index == LAST_MOVABLE_INDEX + 1 -> KaranaKind.SHAKUNI
        index == LAST_MOVABLE_INDEX + 2 -> KaranaKind.CHATUSHPADA
        else -> KaranaKind.NAGA
    }
}

private const val LAST_MOVABLE_INDEX = 56
