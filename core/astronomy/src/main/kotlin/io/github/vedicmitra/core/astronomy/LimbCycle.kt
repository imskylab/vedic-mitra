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
 * A panchanga value that repeats on a fixed cycle, so the one before and the one after can be named.
 *
 * This is what makes "previous / current / next" possible without touching the ephemeris. Every one
 * of these limbs is a numbered position in a closed loop, and the names are a pure function of that
 * number — the tithi before Chaturdashi is Trayodashi whatever the sky is doing. Only the *times*
 * come from the engine, and those are already carried by [LimbWindow]: the previous value ended when
 * the current one began, and the next begins when the current one ends.
 *
 * Deliberately not every row of a panchanga. Sunrise, the muhurtas and the like are instants and
 * spans rather than positions in a cycle — "the previous Rahu Kalam" is yesterday's, not a step
 * back in a loop — and maasa, samvatsara, ayana and ritu turn over on scales where a neighbour is
 * months or years away and tells a reader nothing about today.
 *
 * @property displayName what the row is called.
 * @property cycleLength how many values the loop holds before it repeats.
 */
enum class PanchangaLimb(
    val displayName: String,
    val cycleLength: Int,
) {
    VARA("Vara", 7),
    TITHI("Tithi", 30),
    NAKSHATRA("Nakshatra", 27),
    PADA("Pada", 4),
    YOGA("Yoga", 27),
    KARANA("Karana", 60),
    MOON_RASHI("Chandra Rashi", 12),
    SUN_RASHI("Surya Rashi", 12),
    MOON_PHASE("Moon Phase", 8),
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
 */
data class LimbStep(
    val limb: PanchangaLimb,
    val position: Int,
    val window: LimbWindow?,
) {
    val previous: String get() = limb.previousTo(position)
    val current: String get() = limb.nameAt(position)
    val next: String get() = limb.nextAfter(position)

    /** How far through the current value we are, `[0, 1)`, or `null` if unknown. */
    val fraction: Double? get() = window?.angularFraction
}

/**
 * The nine cycling rows of a day's panchanga, in the order they are traditionally recited — vara,
 * tithi, nakshatra, yoga, karana — with the Moon's finer and slower positions after them.
 *
 * Returns an empty list when the snapshot has no limb windows, since without them there are no
 * boundaries to show and the row would be a bare name.
 */
fun AstronomySnapshot.limbSteps(): List<LimbStep> {
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
    }
}
