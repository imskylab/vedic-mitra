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
 * The three numbered era years for one lunar year — what a printed panchanga carries on its cover
 * beside the sixty-name [Samvatsara].
 *
 * @property vikrama the Vikrama Samvat year (epoch 57 BCE).
 * @property shaka the Shaka Samvat year (epoch 78 CE) — the same value [Samvatsara.shakaYear]
 *   carries, repeated here so all three eras can be read together.
 * @property kali the Kali Yuga year (traditional epoch 3102 BCE).
 */
data class EraYears(
    val vikrama: Int,
    val shaka: Int,
    val kali: Int,
)

// Vikrama's epoch is 57 BCE and Shaka's 78 CE, so the two run 135 apart. Kali's epoch is 3102 BCE,
// putting it 3180 ahead of Shaka. Both are differences between epochs, not adjustable constants.
private const val VIKRAMA_MINUS_SHAKA = 135
private const val KALI_MINUS_SHAKA = 3180

/**
 * The era years for the lunar year whose elapsed Shaka year is [shakaYear] — as produced by
 * [samvatsaraOf], which is what anchors this to a lunar year rather than a Gregorian one.
 *
 * ## The convention this follows
 *
 * All three are **Chaitradi**: the year turns at Chaitra Shukla Pratipada (Ugadi), not at any
 * Gregorian date. A day in January therefore belongs to the era year that began the previous March.
 * That boundary is inherited whole from [samvatsaraOf], which walks back new moon by new moon to
 * the Chaitra opening the current lunar year — so an era year and the samvatsara can never turn on
 * different days.
 *
 * The numbers are the ones a panchanga prints for the lunar year in progress. Shaka and Kali are
 * conventionally counted as years **elapsed** since their epochs; Vikrama is conventionally quoted
 * as the year **current**. Those conventions are already folded into the offsets above, which is
 * why Vikrama exceeds Shaka by the full 135-year gap between their epochs rather than by 134.
 *
 * The month-naming scheme does not move this boundary. Amanta and purnimanta disagree about what
 * the dark fortnight is called, not about where the year starts — Chaitra Shukla Pratipada is the
 * same instant in both — so adding purnimanta naming would leave every value here unchanged.
 *
 * ## What is deliberately not modelled
 *
 * - **Kartikadi Vikrama.** In Gujarat the Vikrama year begins at Kartika Shukla Pratipada instead,
 *   so between Chaitra and Kartika a Kartikadi almanac reads one year behind this one. Supporting
 *   it means a second year boundary and a per-user choice of which to follow, which is its own
 *   piece of work; reporting one reckoning under a name that covers two would be worse.
 * - **Elapsed-year Vikrama.** Some sources print the elapsed Vikrama year, one less than this.
 * - The Kali Yuga epoch is the **traditional** reckoning, not an observed event.
 *
 * A reader comparing against a source that makes any of those choices differently will see a
 * difference of one year. Saying so is better than quietly shipping one reading as the only one.
 */
fun eraYearsOf(shakaYear: Int): EraYears =
    EraYears(
        vikrama = shakaYear + VIKRAMA_MINUS_SHAKA,
        shaka = shakaYear,
        kali = shakaYear + KALI_MINUS_SHAKA,
    )
