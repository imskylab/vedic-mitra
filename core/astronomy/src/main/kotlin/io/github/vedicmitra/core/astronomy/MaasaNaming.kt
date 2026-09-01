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

import io.github.vedicmitra.core.common.model.MaasaReckoning

/**
 * This month's name under [reckoning], given the fortnight [paksha] the day falls in.
 *
 * ## What actually differs
 *
 * Nothing about the astronomy. The engine computes amanta throughout — new moon to new moon, the
 * month named for the solar rashi at the new moon that opens it (see [Maasa] and ADR 0005) — and
 * purnimanta is a **relabelling of the same days**, not a second calculation.
 *
 * A purnimanta month ends at the full moon rather than the new moon, so it is half a month out of
 * step: it takes the dark fortnight that amanta puts at the *end* of month M and puts it at the
 * *start* of month M+1. Which gives the whole rule:
 *
 * - **Shukla paksha** — the two agree. Chaitra Shukla Saptami is that in both.
 * - **Krishna paksha** — purnimanta uses the **following** month's name. What amanta calls Phalguna
 *   Krishna Ashtami, purnimanta calls Chaitra Krishna Ashtami.
 *
 * So for roughly a fortnight in every lunar month the two name the same day differently, which is
 * why a reader in Varanasi and a reader in Bengaluru can both be right and disagree. Tithi, paksha,
 * festival dates and the year boundary are untouched: Chaitra Shukla Pratipada opens the year in
 * both schemes, so [EraYears] and [Samvatsara] need no adjustment.
 *
 * ## What is deliberately not settled here
 *
 * **The adhika (intercalary) month.** During a leap month's dark fortnight this returns the base
 * name without the "Adhika" prefix, because the month that follows an Adhika Jyeshtha is the nija
 * (true) Jyeshtha rather than Ashadha. That is the rule applied mechanically, and it is **not
 * verified against a reference** — sources differ on how an intercalary month is labelled in
 * purnimanta usage, and this app has no independent implementation to check it against. A reader
 * comparing an almanac during a leap month may see a different name, and that is the case to
 * distrust first. Everything outside an adhika month follows the plain rule above.
 */
fun Maasa.nameIn(
    reckoning: MaasaReckoning,
    paksha: Paksha,
): String =
    when (reckoning) {
        MaasaReckoning.AMANTA -> displayName
        MaasaReckoning.PURNIMANTA ->
            when (paksha) {
                Paksha.SHUKLA -> displayName
                // The next month along -- except after a leap month, where the next month is that
                // month's nija counterpart and carries the same name without the prefix.
                Paksha.KRISHNA -> if (adhika) name else maasaNameOf(number + 1)
            }
    }
