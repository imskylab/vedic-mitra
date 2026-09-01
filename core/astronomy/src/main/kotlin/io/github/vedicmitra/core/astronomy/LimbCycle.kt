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

import io.github.vedicmitra.core.common.model.MaasaReckoning

/**
 * A panchanga value that repeats on a fixed cycle, so the one before and the one after can be named.
 *
 * This is what makes "previous / current / next" possible without touching the ephemeris. For all
 * but one of these limbs the names are a pure function of a position in a closed loop — the tithi
 * before Chaturdashi is Trayodashi whatever the sky is doing — and only the *times* come from the
 * engine, already carried by [LimbWindow]. The lunar month is the exception; see [LimbStep.names].
 *
 * Deliberately not every row of a panchanga. Sunrise, the muhurtas and the like are instants and
 * spans rather than positions in a cycle — "the previous Rahu Kalam" is yesterday's, not a step
 * back in a loop.
 *
 * **Ayana and samvatsara are excluded on their own merits, not for being slow.** An earlier version
 * of this rule ruled out everything slower than a day, which was wrong twice over: it took maasa
 * out despite it turning on the same ~30-day scale as the Sun's rashi, which was kept *because its
 * boundary is useful*; and it conflated the neighbour with the boundary. The neighbour is what
 * fails for those two — there are only two ayanas, so it is always the other one, and last year's
 * samvatsara name is trivia. Their boundaries are still worth showing, and are, as a table row.
 *
 * @property displayName what the row is called.
 * @property cycleLength how many values the loop holds before it repeats.
 * @property concept the idea a reader tapping this row wants explained, in [PanchangaPrimer]. Held
 *   here rather than resolved in the UI so that the mapping is total by construction: a limb cannot
 *   be added without naming the concept that explains it, and a concept cannot be named without
 *   copy, because [PanchangaConcept] is closed and covered by a test over `entries`.
 */
enum class PanchangaLimb(
    val displayName: String,
    val cycleLength: Int,
    val concept: PanchangaConcept,
) {
    VARA("Vara", 7, PanchangaConcept.VARA),
    TITHI("Tithi", 30, PanchangaConcept.TITHI),
    NAKSHATRA("Nakshatra", 27, PanchangaConcept.NAKSHATRA),
    PADA("Pada", 4, PanchangaConcept.PADA),
    YOGA("Yoga", 27, PanchangaConcept.YOGA),
    KARANA("Karana", 60, PanchangaConcept.KARANA),

    // Both rashi rows explain the same idea: the sign, not which body is in it.
    MOON_RASHI("Chandra Rashi", 12, PanchangaConcept.RASHI),
    SUN_RASHI("Surya Rashi", 12, PanchangaConcept.RASHI),
    MOON_PHASE("Moon Phase", 8, PanchangaConcept.MOON_PHASE),

    // The two slow limbs whose neighbours are worth naming. Ayana and samvatsara are deliberately
    // not here: there are only two ayanas, so the neighbour is always the other one, and last
    // year's samvatsara name is trivia. Both still get their boundary shown, as a table row.
    MAASA("Maasa", 12, PanchangaConcept.LUNAR_MONTH),
    RITU("Ritu", 6, PanchangaConcept.RITU),
    ;

    /**
     * The value at [position] in this limb's cycle, **1-based**, wrapping at either end.
     *
     * Wrapping is the point: the tithi after Amavasya (30) is Pratipada (1), and asking for the one
     * before Pratipada must give Amavasya rather than an index of zero. Callers therefore never need
     * to handle the boundary themselves, which is where this would otherwise go wrong once a month.
     */
    fun nameAt(position: Int): String {
        val wrapped = ((position - 1).mod(cycleLength)) + 1
        return when (this) {
            VARA -> Vara.entries[wrapped - 1].displayName
            TITHI -> tithiNameAt(wrapped)
            NAKSHATRA -> NAKSHATRA_NAMES[wrapped - 1]
            PADA -> "Pada $wrapped"
            YOGA -> yogaNameAt(wrapped)
            KARANA -> karanaNameAt(wrapped)
            MOON_RASHI, SUN_RASHI -> RASHI_NAMES[wrapped - 1]
            MOON_PHASE -> MoonPhase.entries[wrapped - 1].displayName
            // Only ever a fallback: a maasa row is built with explicit names, because stepping the
            // number is wrong in an adhika year. See LimbStep.names.
            MAASA -> maasaNameOf(wrapped)
            RITU -> Ritu.entries[wrapped - 1].displayName
        }
    }

    /** The value one step back from [position]. */
    fun previousTo(position: Int): String = nameAt(position - 1)

    /** The value one step on from [position]. */
    fun nextAfter(position: Int): String = nameAt(position + 1)
}

/**
 * One limb as a reader meets it: what it was, what it is, and what it becomes.
 *
 * @property limb which row this is.
 * @property position the current value's place in the cycle, 1-based.
 * @property previous the value that just ended.
 * @property current the value now running.
 * @property next the value that follows.
 * @property window when the current value began and ends, or `null` if that is not known — which
 *   happens for vara at latitudes where the Sun does not rise. The previous value ended at
 *   [LimbWindow.start] and the next begins at [LimbWindow.end]; they are the same two instants,
 *   which is why no extra computation is needed to show all three.
 * @property names explicit names, for the one limb whose neighbours are **not** a pure function of
 *   position: the lunar month. An adhika year holds thirteen lunations, and under purnimanta the
 *   name also depends on the fortnight, so maasa's three values are computed where the ephemeris
 *   and the user's chosen scheme are both in reach, and carried here rather than derived.
 */
data class LimbStep(
    val limb: PanchangaLimb,
    val position: Int,
    val window: LimbWindow?,
    val names: LimbNames? = null,
) {
    val previous: String get() = names?.previous ?: limb.previousTo(position)
    val current: String get() = names?.current ?: limb.nameAt(position)
    val next: String get() = names?.next ?: limb.nextAfter(position)

    /** How far through the current value we are, `[0, 1)`, or `null` if unknown. */
    val fraction: Double? get() = window?.angularFraction
}

/** Three names for one row, when they cannot be derived from a position. See [LimbStep.names]. */
data class LimbNames(
    val previous: String,
    val current: String,
    val next: String,
)

/**
 * The cycling rows of a day's panchanga, in the order they are traditionally recited — vara, tithi,
 * nakshatra, yoga, karana — with the Moon's finer positions after them, and the two slow limbs
 * whose neighbours mean something (the lunar month and the season) last.
 *
 * Month names follow [reckoning]; every other row is scheme-independent.
 *
 * Returns an empty list when the snapshot has no limb windows, since without them there are no
 * boundaries to show and the row would be a bare name.
 */
fun AstronomySnapshot.limbSteps(reckoning: MaasaReckoning = MaasaReckoning.AMANTA): List<LimbStep> {
    val limbs = limbs ?: return emptyList()
    return buildList {
        add(LimbStep(PanchangaLimb.VARA, vara.ordinal + 1, limbs.vara))
        add(LimbStep(PanchangaLimb.TITHI, tithi.number, limbs.tithi))
        add(LimbStep(PanchangaLimb.NAKSHATRA, nakshatra.number, limbs.nakshatra))
        moonPada?.let { add(LimbStep(PanchangaLimb.PADA, it, limbs.moonPada)) }
        add(LimbStep(PanchangaLimb.YOGA, yoga.number, limbs.yoga))
        add(LimbStep(PanchangaLimb.KARANA, karana.number, limbs.karana))
        moonRasi?.let { add(LimbStep(PanchangaLimb.MOON_RASHI, it.index + 1, limbs.moonRashi)) }
        sunRasi?.let { add(LimbStep(PanchangaLimb.SUN_RASHI, it.index + 1, limbs.sunRashi)) }
        add(LimbStep(PanchangaLimb.MOON_PHASE, moonPhase.ordinal + 1, limbs.moonPhase))
        maasaCycle?.let { cycle ->
            // Each of the three read through nameIn, which is what makes the row correct under
            // purnimanta: on a Krishna day every one of them shifts forward together, so the row
            // reads as that scheme's own sequence rather than a mix of the two.
            add(
                LimbStep(
                    limb = PanchangaLimb.MAASA,
                    position = cycle.current.number,
                    window = cycle.window,
                    names =
                        LimbNames(
                            previous = cycle.previous.nameIn(reckoning, tithi.paksha),
                            current = cycle.current.nameIn(reckoning, tithi.paksha),
                            next = cycle.next.nameIn(reckoning, tithi.paksha),
                        ),
                ),
            )
        }
        limbs.ritu?.let { add(LimbStep(PanchangaLimb.RITU, ritu.ordinal + 1, it)) }
    }
}
