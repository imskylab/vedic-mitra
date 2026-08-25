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
 * The four porutham read alongside the thirty-six gunas: Mahendra, Vedha, Rajju and Sthree Dheerga.
 *
 * Ashtakoota answers "how well matched", as a score. These four answer "is anything wrong", as yes or
 * no, and they are not folded into the total — a match can score well and still fail Rajju, which is
 * exactly the case the score alone would hide. They belong to the South Indian porutham tradition but
 * are read beside Ashtakoota across the north as well.
 *
 * Every table here was **derived from an independent implementation rather than from memory**, by
 * sweeping one partner's nakshatra across a full lunar month and reading back its verdicts. That was
 * worth doing: two of the four came out differently from the textbook summary this would otherwise
 * have been written from. See each rule for what the data actually said.
 *
 * Each result carries **why**, not just whether. A verdict a reader cannot check against another
 * source is worth very little here, since sources genuinely differ on these rules — and the count or
 * the pair of limbs is the thing they differ about.
 *
 * @property mahendra progeny and longevity.
 * @property vedha the absence of mutual affliction. `held` means no vedha, matching the sense of the
 *   other three, where `held` is always the good outcome.
 * @property rajju the couple falling on different limbs of the body.
 * @property sthreeDheerga the bride's welfare and longevity.
 */
data class AdditionalPorutham(
    val mahendra: PoruthamResult,
    val vedha: PoruthamResult,
    val rajju: PoruthamResult,
    val sthreeDheerga: PoruthamResult,
) {
    /** The four in reading order, for a UI that wants to render them uniformly. */
    val all: List<PoruthamResult> get() = listOf(mahendra, vedha, rajju, sthreeDheerga)

    /** How many of the four hold, 0..4. */
    val matched: Int get() = all.count { it.held }
}

/**
 * One porutham: whether it holds, and the working behind that.
 *
 * @property name what the rule is called.
 * @property held whether it holds. Always `true` for the good outcome, including Vedha, where the
 *   good outcome is an absence.
 * @property governs what the rule is traditionally read for, in a few words.
 * @property working the arithmetic that produced the verdict — the count, the limbs, the pair of
 *   nakshatras — phrased so it can be checked against a printed table.
 */
data class PoruthamResult(
    val name: String,
    val held: Boolean,
    val governs: String,
    val working: String,
)

/**
 * The limb of the body a nakshatra belongs to, for Rajju.
 *
 * The twenty-seven nakshatras are laid along a body and back again — foot, waist, stomach, neck,
 * head, then neck, stomach, waist, foot — repeating three times over. A couple sharing a limb is the
 * affliction; the limb they share is held to say what suffers, which is why the limb is named rather
 * than reduced to a boolean.
 */
enum class Rajju(
    val displayName: String,
) {
    PADA("Foot"),
    KATI("Waist"),
    UDARA("Stomach"),
    KANTHA("Neck"),
    SIRO("Head"),
}

/** The four porutham between a [groom] and a [bride], each with its working. */
fun additionalPorutham(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): AdditionalPorutham {
    val g = groom.nakshatraNumber
    val b = bride.nakshatraNumber
    val gName = NAKSHATRA_NAMES[g - 1]
    val bName = NAKSHATRA_NAMES[b - 1]

    val mahendraCount = countBetween(g, b)
    val mahendraHolds = mahendraCount in MAHENDRA_COUNTS
    val sthreeCount = countBetween(b, g)
    val sthreeHolds = sthreeCount > STHREE_DHEERGA_MINIMUM
    val pierced = hasVedha(g, b)
    val gRajju = rajjuOf(g)
    val bRajju = rajjuOf(b)

    return AdditionalPorutham(
        mahendra =
            PoruthamResult(
                name = "Mahendra",
                held = mahendraHolds,
                governs = "Progeny and longevity",
                working =
                    "$mahendraCount between $gName and $bName — " +
                        (if (mahendraHolds) "one of " else "wants one of ") +
                        MAHENDRA_COUNTS.sorted().joinToString(", "),
            ),
        vedha =
            PoruthamResult(
                name = "Vedha",
                held = !pierced,
                governs = "Absence of mutual affliction",
                working =
                    if (pierced) {
                        "$gName and $bName pierce one another"
                    } else {
                        "$gName and $bName do not pierce"
                    },
            ),
        rajju =
            PoruthamResult(
                name = "Rajju",
                held = gRajju != bRajju,
                governs = "The husband's longevity",
                working =
                    if (gRajju != bRajju) {
                        "${gRajju.displayName} and ${bRajju.displayName} — different limbs"
                    } else {
                        "Both fall on the ${gRajju.displayName.lowercase()}"
                    },
            ),
        sthreeDheerga =
            PoruthamResult(
                name = "Sthree Dheerga",
                held = sthreeHolds,
                governs = "The bride's welfare and longevity",
                working =
                    "$sthreeCount from her star to his — " +
                        (if (sthreeHolds) "more than " else "wants more than ") +
                        STHREE_DHEERGA_MINIMUM,
            ),
    )
}

/** The limb [nakshatraNumber] (1..27) falls on. */
fun rajjuOf(nakshatraNumber: Int): Rajju = RAJJU_BY_NAKSHATRA[nakshatraNumber - 1]

/**
 * Mahendra porutham — progeny and longevity.
 *
 * Holds when the count between the two nakshatras is 4, 7, 10, 13, 16, 19, 22 or 25: every third
 * nakshatra from the fourth to the twenty-fifth.
 *
 * **The direction does not matter, and that is a property of the set rather than an assumption.**
 * Counting one way and the other always sums to 29, and this set is closed under `c -> 29 - c`
 * (4↔25, 7↔22, 10↔19, 13↔16). Sources that specify opposite directions therefore agree, which is
 * presumably why the disagreement has survived. Confirmed against the reference data, which reported
 * the same verdict whichever way it was read.
 */
internal fun mahendraPorutham(
    groomNak: Int,
    brideNak: Int,
): Boolean = countBetween(groomNak, brideNak) in MAHENDRA_COUNTS

/**
 * Sthree Dheerga porutham — the bride's welfare and longevity.
 *
 * Holds when the count **from the bride's nakshatra to the groom's** is more than thirteen. Unlike
 * Mahendra this is genuinely directional: the range 14..27 is not symmetric under `c -> 29 - c`, so
 * counting the wrong way inverts nearly every verdict. The reference data settles it — every pair
 * with fourteen or more from her star to his matched, and every pair with thirteen or fewer,
 * including the two sharing a nakshatra, did not.
 */
internal fun sthreeDheergaPorutham(
    groomNak: Int,
    brideNak: Int,
): Boolean = countBetween(brideNak, groomNak) > STHREE_DHEERGA_MINIMUM

/**
 * Whether the two nakshatras pierce one another (vedha).
 *
 * **This is not a set of disjoint pairs**, which is how the rule is usually summarised and how this
 * was first written. The reference data shows the relation is "the two nakshatra numbers sum to 19,
 * 28 or 37", which gives most nakshatras **two** partners and the nine from Magha to Jyeshtha
 * **three**. Written as thirteen pairs it would have missed roughly half of all vedha.
 *
 * One consequence worth stating because it looks like a bug: **Chitra vedhas itself** — 14 + 14 = 28
 * — so two people born under it are afflicted by the rule. That falls out of the same arithmetic
 * that produces every other pairing and is confirmed by the reference data, not an off-by-one here.
 */
internal fun hasVedha(
    groomNak: Int,
    brideNak: Int,
): Boolean = (groomNak + brideNak) in VEDHA_SUMS

/** Nakshatras counted inclusively from [fromNak] to [toNak], 1..27 — the same count Tara uses. */
private fun countBetween(
    fromNak: Int,
    toNak: Int,
): Int = ((toNak - fromNak + NAKSHATRA_COUNT) % NAKSHATRA_COUNT) + 1

private const val NAKSHATRA_COUNT = 27

/** Every third count from the 4th to the 25th. */
private val MAHENDRA_COUNTS = setOf(4, 7, 10, 13, 16, 19, 22, 25)

/** Sthree Dheerga wants strictly more than this, counted from the bride's star to the groom's. */
private const val STHREE_DHEERGA_MINIMUM = 13

/** Two nakshatras pierce one another when their numbers sum to one of these. */
private val VEDHA_SUMS = setOf(19, 28, 37)

/**
 * The limb each nakshatra falls on, Ashwini first.
 *
 * Foot, waist, stomach, neck, head, then back down neck, stomach, waist, foot — nine at a time,
 * three times over. Written out rather than generated so it can be read against a printed table.
 */
private val RAJJU_BY_NAKSHATRA =
    listOf(
        Rajju.PADA,
        Rajju.KATI,
        Rajju.UDARA,
        Rajju.KANTHA,
        Rajju.SIRO,
        Rajju.KANTHA,
        Rajju.UDARA,
        Rajju.KATI,
        Rajju.PADA,
        Rajju.PADA,
        Rajju.KATI,
        Rajju.UDARA,
        Rajju.KANTHA,
        Rajju.SIRO,
        Rajju.KANTHA,
        Rajju.UDARA,
        Rajju.KATI,
        Rajju.PADA,
        Rajju.PADA,
        Rajju.KATI,
        Rajju.UDARA,
        Rajju.KANTHA,
        Rajju.SIRO,
        Rajju.KANTHA,
        Rajju.UDARA,
        Rajju.KATI,
        Rajju.PADA,
    )
