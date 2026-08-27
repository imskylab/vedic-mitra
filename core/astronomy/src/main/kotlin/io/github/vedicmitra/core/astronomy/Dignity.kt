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
 * How well a graha sits in the rashi it occupies — its dignity, or *avastha* of place.
 *
 * This is the oldest and plainest statement of planetary strength: the same Mars means one thing in
 * Makara, where it is exalted, and another in Karka, where it is debilitated. Everything else this
 * app computes about a graha — its house, its aspects, its ashtakavarga bindus — is read against it.
 *
 * ## Where this came from
 *
 * Most of it was already being computed and thrown away. Exaltation lived privately inside the yoga
 * code, the sign lords inside the matchmaking code, the friendship matrix beside them, and Mars's
 * own-and-exaltation signs were written out a **third** time inside the Mangal dosha rule. Three
 * statements of one fact, none of them reachable from a chart. They are consolidated here, and the
 * former sites now delegate.
 *
 * ## What is deliberately not here
 *
 * - **Moolatrikona**, the portion of a graha's own sign where it is stronger still. It is a real
 *   sixth state, but it is defined by *degree ranges* rather than by sign, and no such table exists
 *   anywhere in this repo. Adding it means deriving and validating new data, not surfacing what is
 *   already computed, so it is its own piece of work.
 * - **Temporal (tatkalika) friendship**, which depends on where the two grahas sit relative to each
 *   other in the chart rather than on their natures, and the five-fold compound of the two. What is
 *   here is the **naisargika** — natural, permanent — relationship alone.
 *
 * Both omissions mean a reader comparing against a source that folds them in will see a difference
 * on some placements. Saying so is better than quietly shipping a narrower rule under a broader name.
 */
enum class Dignity(
    val displayName: String,
    /** The short form the Spashta Graha table shows, where a column is a few characters wide. */
    val abbreviation: String,
) {
    EXALTED("Exalted", "Ex"),
    OWN("Own sign", "Own"),
    FRIEND("Friend's sign", "Fr"),
    NEUTRAL("Neutral sign", "Neu"),
    ENEMY("Enemy's sign", "En"),
    DEBILITATED("Debilitated", "Deb"),
}

/**
 * The dignity of [graha] in the rashi at [rasiIndex] (0 = Mesha), or `null` for Rahu and Ketu.
 *
 * **The nodes genuinely have none.** They own no sign, so there is no lord to be friendly or hostile
 * to, and the exaltation signs attributed to them are not agreed between authorities — sources that
 * name one disagree with each other. Returning `null` and rendering a dash says that; inventing a
 * value would put a number on a disagreement. [Drishti] already declines the same way, returning
 * `false` for the nodes rather than guessing at their aspects.
 *
 * Precedence runs exaltation, debilitation, own sign, then the lord's natural relationship. The
 * first three are properties of the graha and the sign together; only the last consults the lord.
 */
fun dignityOf(
    graha: Graha,
    rasiIndex: Int,
): Dignity? {
    val exaltation = EXALTATION[graha] ?: return null
    return when {
        rasiIndex == exaltation -> Dignity.EXALTED
        rasiIndex == debilitationOf(graha) -> Dignity.DEBILITATED
        RASI_LORD[rasiIndex] == graha -> Dignity.OWN
        else -> naturalRelation(graha, RASI_LORD[rasiIndex])
    }
}

/**
 * The rashi index where [graha] is debilitated — **derived, not tabulated**.
 *
 * Debilitation is exactly opposite exaltation, so writing out a second twelve-entry table would only
 * create somewhere for the two to disagree. Deriving it makes that classical relationship a property
 * of the code, and [DignityTest] asserts it holds for all seven.
 */
internal fun debilitationOf(graha: Graha): Int? = EXALTATION[graha]?.let { (it + OPPOSITE_SIGN) % RASHI_COUNT }

/**
 * How [of] naturally regards the graha [to] — friend, neutral or enemy.
 *
 * **This relation is not symmetric, and that is the tradition rather than a transcription slip.**
 * Budha counts the Moon an enemy while the Moon counts Budha a friend; the Moon has no enemies at
 * all. Anyone tidying these tables into a symmetric matrix would be changing the rule, so
 * [DignityTest] asserts the asymmetry deliberately.
 *
 * Only ever called with the seven classical grahas: [dignityOf] returns before reaching it for the
 * nodes, and [RASI_LORD] never names one.
 */
internal fun naturalRelation(
    of: Graha,
    to: Graha,
): Dignity =
    when (to) {
        in PLANET_FRIENDS.getValue(of) -> Dignity.FRIEND
        in PLANET_ENEMIES.getValue(of) -> Dignity.ENEMY
        else -> Dignity.NEUTRAL
    }

/** Whether [graha] is strong by place in [rasiIndex] — exalted or in its own sign. */
internal fun isStrongByPlace(
    graha: Graha,
    rasiIndex: Int,
): Boolean = dignityOf(graha, rasiIndex) in STRONG_BY_PLACE

private val STRONG_BY_PLACE = setOf(Dignity.EXALTED, Dignity.OWN)

private const val RASHI_COUNT = 12

/** Debilitation sits this many signs from exaltation — half the zodiac. */
private const val OPPOSITE_SIGN = 6

/**
 * Exaltation rashi index per graha, classical. The seven only; see [dignityOf] on the nodes.
 *
 * Membership doubles as the "is this a graha with a dignity at all" test, which is why [dignityOf]
 * reads it first.
 */
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

/**
 * The lord of each rashi, Mesha first.
 *
 * The five non-luminaries take two signs each and the Sun and Moon one apiece, which is what makes
 * the twelve come out even — an invariant [DignityTest] asserts, since a mistyped entry here would
 * otherwise shift an own-sign verdict silently.
 */
internal val RASI_LORD =
    listOf(
        Graha.MANGALA,
        Graha.SHUKRA,
        Graha.BUDHA,
        Graha.MOON,
        Graha.SUN,
        Graha.BUDHA,
        Graha.SHUKRA,
        Graha.MANGALA,
        Graha.GURU,
        Graha.SHANI,
        Graha.SHANI,
        Graha.GURU,
    )

private val PLANET_FRIENDS =
    mapOf(
        Graha.SUN to setOf(Graha.MOON, Graha.MANGALA, Graha.GURU),
        Graha.MOON to setOf(Graha.SUN, Graha.BUDHA),
        Graha.MANGALA to setOf(Graha.SUN, Graha.MOON, Graha.GURU),
        Graha.BUDHA to setOf(Graha.SUN, Graha.SHUKRA),
        Graha.GURU to setOf(Graha.SUN, Graha.MOON, Graha.MANGALA),
        Graha.SHUKRA to setOf(Graha.BUDHA, Graha.SHANI),
        Graha.SHANI to setOf(Graha.BUDHA, Graha.SHUKRA),
    )

private val PLANET_ENEMIES =
    mapOf(
        Graha.SUN to setOf(Graha.SHUKRA, Graha.SHANI),
        Graha.MOON to emptySet(),
        Graha.MANGALA to setOf(Graha.BUDHA),
        Graha.BUDHA to setOf(Graha.MOON),
        Graha.GURU to setOf(Graha.BUDHA, Graha.SHUKRA),
        Graha.SHUKRA to setOf(Graha.SUN, Graha.MOON),
        Graha.SHANI to setOf(Graha.SUN, Graha.MOON, Graha.MANGALA),
    )
