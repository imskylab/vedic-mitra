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
 * One partner's inputs for Ashtakoota matching: the Moon's birth nakshatra, sign and pada, all read
 * from the natal chart ([NatalChart.moonNakshatra], the Moon graha's [Rasi], and [NatalChart.moonPada]).
 *
 * @property nakshatraNumber the Moon's nakshatra, 1..27 (Ashwini = 1).
 * @property moonRasiIndex the Moon's sign, 0..11 (Mesha = 0).
 * @property moonPada the Moon's nakshatra quarter, 1..4 — used only for the Nadi-dosha cancellation.
 */
data class GunaMilanProfile(
    val nakshatraNumber: Int,
    val moonRasiIndex: Int,
    val moonPada: Int = 1,
)

/** The eight kootas (koota) of North-Indian Ashtakoota matching, with their maximum guna weights. */
enum class Koota(
    val displayName: String,
    val maxPoints: Double,
) {
    VARNA("Varna", 1.0),
    VASHYA("Vashya", 2.0),
    TARA("Tara", 3.0),
    YONI("Yoni", 4.0),
    GRAHA_MAITRI("Graha Maitri", 5.0),
    GANA("Gana", 6.0),
    BHAKOOT("Bhakoot", 7.0),
    NADI("Nadi", 8.0),
}

/** One koota's contribution: the [points] earned (0..[Koota.maxPoints]) and a short explanation. */
data class KootaScore(
    val koota: Koota,
    val points: Double,
    val note: String,
)

/** The overall band a total (out of 36) falls into. */
enum class GunaMilanVerdict(
    val label: String,
) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    AVERAGE("Average"),
    POOR("Not recommended"),
}

/**
 * The result of an Ashtakoota (36-guna) match.
 *
 * @property scores the eight kootas' individual scores, in koota order.
 * @property total the sum of the earned points, 0..36.
 * @property verdict the band [total] falls into.
 * @property doshas notable cautions (Nadi / Bhakoot dosha) present regardless of the total.
 */
data class GunaMilanResult(
    val scores: List<KootaScore>,
    val total: Double,
    val verdict: GunaMilanVerdict,
    val doshas: List<String>,
) {
    /** The maximum attainable total, 36. */
    val maxTotal: Double get() = MAX_TOTAL

    private companion object {
        const val MAX_TOTAL = 36.0
    }
}

/**
 * Scores an Ashtakoota (36-guna) match between a [groom] and a [bride] from their Moon nakshatra,
 * sign and pada. Uses the standard classical tables (see the KDoc on each koota's scorer); the Nadi
 * and Bhakoot doshas carry their classical cancellation (parihara) rules. The well-known regional
 * variations in Yoni and Vashya are called out where they occur; empirical refinements are applied
 * outside the app, not here.
 */
fun gunaMilan(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): GunaMilanResult {
    val bhakoot = bhakootKoota(groom, bride)
    val nadi = nadiKoota(groom, bride)
    // A zeroed Bhakoot/Nadi is a dosha only when its cancellation doesn't apply; cancellation removes
    // the warning but not the lost points (the geometry/nadi clash still costs the koota).
    val bhakootDosha = bhakoot.points == 0.0 && !bhakootCancelled(groom, bride)
    val nadiDosha = nadi.points == 0.0 && !nadiCancelled(groom, bride)
    val scores =
        listOf(
            varnaKoota(groom, bride),
            vashyaKoota(groom, bride),
            taraKoota(groom, bride),
            yoniKoota(groom, bride),
            grahaMaitriKoota(groom, bride),
            ganaKoota(groom, bride),
            cancelledNote(bhakoot, cancelled = bhakoot.points == 0.0 && !bhakootDosha),
            cancelledNote(nadi, cancelled = nadi.points == 0.0 && !nadiDosha),
        )
    val total = scores.sumOf { it.points }
    val doshas =
        buildList {
            if (nadiDosha) add("Nadi dosha")
            if (bhakootDosha) add("Bhakoot dosha")
        }
    return GunaMilanResult(scores = scores, total = total, verdict = verdictFor(total), doshas = doshas)
}

/** Appends a "dosha cancelled" note to a zeroed Bhakoot/Nadi koota whose parihara applies. */
private fun cancelledNote(
    score: KootaScore,
    cancelled: Boolean,
): KootaScore = if (cancelled) score.copy(note = "${score.note} — dosha cancelled") else score

/**
 * Bhakoot-dosha cancellation: the two Moon signs are ruled by the same planet (Mesha/Vrishchika,
 * Vrishabha/Tula, Makara/Kumbha) or by mutually-friendly planets.
 */
private fun bhakootCancelled(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): Boolean {
    val lg = RASI_LORD[groom.moonRasiIndex]
    val lb = RASI_LORD[bride.moonRasiIndex]
    if (lg == lb) return true
    return relationOf(lg, lb) == Relation.FRIEND && relationOf(lb, lg) == Relation.FRIEND
}

/**
 * Nadi-dosha cancellation: same nakshatra but a different pada, same rashi with different nakshatras,
 * or same nakshatra falling in different rashis.
 */
private fun nadiCancelled(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): Boolean {
    val sameNakshatra = groom.nakshatraNumber == bride.nakshatraNumber
    val sameRasi = groom.moonRasiIndex == bride.moonRasiIndex
    val samePada = groom.moonPada == bride.moonPada
    // A cancellation applies whenever the Moons share a nakshatra or a rashi — unless they are the
    // exact same Moon (same nakshatra, pada and rashi), where nothing offsets the shared nadi.
    if (sameNakshatra && sameRasi && samePada) return false
    return sameNakshatra || sameRasi
}

private fun verdictFor(total: Double): GunaMilanVerdict =
    when {
        total >= 32 -> GunaMilanVerdict.EXCELLENT
        total >= 25 -> GunaMilanVerdict.GOOD
        total >= 18 -> GunaMilanVerdict.AVERAGE
        else -> GunaMilanVerdict.POOR
    }

// ---- Varna (max 1): spiritual/temperamental order by Moon sign; groom's varna should not be lower.

internal enum class Varna(
    val rank: Int,
) {
    SHUDRA(1),
    VAISHYA(2),
    KSHATRIYA(3),
    BRAHMIN(4),
}

// Moon sign -> varna: water signs Brahmin, fire Kshatriya, earth Vaishya, air Shudra (0 = Mesha).
internal val VARNA_BY_RASI =
    listOf(
        Varna.KSHATRIYA,
        Varna.VAISHYA,
        Varna.SHUDRA,
        Varna.BRAHMIN,
        Varna.KSHATRIYA,
        Varna.VAISHYA,
        Varna.SHUDRA,
        Varna.BRAHMIN,
        Varna.KSHATRIYA,
        Varna.VAISHYA,
        Varna.SHUDRA,
        Varna.BRAHMIN,
    )

private fun varnaKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val g = VARNA_BY_RASI[groom.moonRasiIndex]
    val b = VARNA_BY_RASI[bride.moonRasiIndex]
    val points = if (g.rank >= b.rank) 1.0 else 0.0
    return KootaScore(Koota.VARNA, points, "Groom ${g.name.lowercase()}, bride ${b.name.lowercase()}")
}

// ---- Vashya (max 2): mutual magnetism by sign. Standard vasya sets; 1 point per direction of
// control, 2 for the same sign. (Half-sign refinements for Dhanu/Makara are omitted — verify on-device.)

private val VASYA_SETS: List<Set<Int>> =
    listOf(
        setOf(4, 7),
        setOf(3, 6),
        setOf(5),
        setOf(7, 8),
        setOf(6),
        setOf(11, 2),
        setOf(9, 5),
        setOf(3),
        setOf(11),
        setOf(10, 0),
        setOf(0),
        setOf(9),
    )

private fun vashyaKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val g = groom.moonRasiIndex
    val b = bride.moonRasiIndex
    val points =
        when {
            g == b -> 2.0
            else -> (if (b in VASYA_SETS[g]) 1.0 else 0.0) + (if (g in VASYA_SETS[b]) 1.0 else 0.0)
        }
    return KootaScore(Koota.VASHYA, points, "Mutual attraction of the signs")
}

// ---- Tara (max 3): birth-star health. Auspicious taras {2,4,6,8,9} score in each direction (1.5 each).

private val FAVOURABLE_TARAS = setOf(2, 4, 6, 8, 9)

private fun taraOf(
    fromNak: Int,
    toNak: Int,
): Int {
    val count = ((toNak - fromNak + 27) % 27) + 1
    return ((count - 1) % 9) + 1
}

private fun taraKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val fromGroom = if (taraOf(groom.nakshatraNumber, bride.nakshatraNumber) in FAVOURABLE_TARAS) 1.5 else 0.0
    val fromBride = if (taraOf(bride.nakshatraNumber, groom.nakshatraNumber) in FAVOURABLE_TARAS) 1.5 else 0.0
    return KootaScore(Koota.TARA, fromGroom + fromBride, "Birth-star (tara) compatibility")
}

// ---- Yoni (max 4): instinctive compatibility by nakshatra animal. Same yoni = 4, sworn enemies = 0,
// otherwise neutral = 2. (The finer friendly/unfriendly gradations of the full table are approximated.)

internal enum class Yoni {
    HORSE,
    ELEPHANT,
    SHEEP,
    SERPENT,
    DOG,
    CAT,
    RAT,
    COW,
    BUFFALO,
    TIGER,
    DEER,
    MONKEY,
    MONGOOSE,
    LION,
}

internal val YONI_BY_NAKSHATRA =
    listOf(
        Yoni.HORSE,
        Yoni.ELEPHANT,
        Yoni.SHEEP,
        Yoni.SERPENT,
        Yoni.SERPENT,
        Yoni.DOG,
        Yoni.CAT,
        Yoni.SHEEP,
        Yoni.CAT,
        Yoni.RAT,
        Yoni.RAT,
        Yoni.COW,
        Yoni.BUFFALO,
        Yoni.TIGER,
        Yoni.BUFFALO,
        Yoni.TIGER,
        Yoni.DEER,
        Yoni.DEER,
        Yoni.DOG,
        Yoni.MONKEY,
        Yoni.MONGOOSE,
        Yoni.MONKEY,
        Yoni.LION,
        Yoni.HORSE,
        Yoni.LION,
        Yoni.COW,
        Yoni.ELEPHANT,
    )

private val SWORN_YONI_ENEMIES =
    setOf(
        setOf(Yoni.COW, Yoni.TIGER),
        setOf(Yoni.HORSE, Yoni.BUFFALO),
        setOf(Yoni.ELEPHANT, Yoni.LION),
        setOf(Yoni.DOG, Yoni.DEER),
        setOf(Yoni.CAT, Yoni.RAT),
        setOf(Yoni.MONKEY, Yoni.SHEEP),
        setOf(Yoni.MONGOOSE, Yoni.SERPENT),
    )

private fun yoniKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val g = YONI_BY_NAKSHATRA[groom.nakshatraNumber - 1]
    val b = YONI_BY_NAKSHATRA[bride.nakshatraNumber - 1]
    val points =
        when {
            g == b -> 4.0
            setOf(g, b) in SWORN_YONI_ENEMIES -> 0.0
            else -> 2.0
        }
    return KootaScore(Koota.YONI, points, "Yoni ${g.name.lowercase()} & ${b.name.lowercase()}")
}

// ---- Graha Maitri (max 5): friendship of the Moon-sign lords.

private enum class Relation { FRIEND, NEUTRAL, ENEMY }

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

private fun relationOf(
    of: Graha,
    to: Graha,
): Relation =
    when (to) {
        in PLANET_FRIENDS.getValue(of) -> Relation.FRIEND
        in PLANET_ENEMIES.getValue(of) -> Relation.ENEMY
        else -> Relation.NEUTRAL
    }

private fun grahaMaitriKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val lg = RASI_LORD[groom.moonRasiIndex]
    val lb = RASI_LORD[bride.moonRasiIndex]
    val points =
        if (lg == lb) {
            5.0
        } else {
            pointsForRelations(relationOf(lg, lb), relationOf(lb, lg))
        }
    return KootaScore(Koota.GRAHA_MAITRI, points, "Sign lords ${lg.displayName} & ${lb.displayName}")
}

private fun pointsForRelations(
    a: Relation,
    b: Relation,
): Double {
    val friends = listOf(a, b).count { it == Relation.FRIEND }
    val enemies = listOf(a, b).count { it == Relation.ENEMY }
    return when {
        friends == 2 -> 5.0
        friends == 1 && enemies == 0 -> 4.0
        enemies == 0 -> 3.0
        friends == 1 -> 1.0
        enemies == 1 -> 0.5
        else -> 0.0
    }
}

// ---- Gana (max 6): temperament (Deva/Manushya/Rakshasa). Standard asymmetric groom/bride table:
// same = 6; Deva & Manushya (either way) = 5; groom Manushya + bride Rakshasa = 1; every other
// Rakshasa pairing (Deva-Rakshasa both ways, groom Rakshasa + bride Manushya) = 0.

internal enum class Gana { DEVA, MANUSHYA, RAKSHASA }

internal val GANA_BY_NAKSHATRA =
    listOf(
        Gana.DEVA,
        Gana.MANUSHYA,
        Gana.RAKSHASA,
        Gana.MANUSHYA,
        Gana.DEVA,
        Gana.MANUSHYA,
        Gana.DEVA,
        Gana.DEVA,
        Gana.RAKSHASA,
        Gana.RAKSHASA,
        Gana.MANUSHYA,
        Gana.MANUSHYA,
        Gana.DEVA,
        Gana.RAKSHASA,
        Gana.DEVA,
        Gana.RAKSHASA,
        Gana.DEVA,
        Gana.RAKSHASA,
        Gana.RAKSHASA,
        Gana.MANUSHYA,
        Gana.MANUSHYA,
        Gana.DEVA,
        Gana.RAKSHASA,
        Gana.RAKSHASA,
        Gana.MANUSHYA,
        Gana.MANUSHYA,
        Gana.DEVA,
    )

private fun ganaKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val g = GANA_BY_NAKSHATRA[groom.nakshatraNumber - 1]
    val b = GANA_BY_NAKSHATRA[bride.nakshatraNumber - 1]
    val points =
        when {
            g == b -> 6.0
            setOf(g, b) == setOf(Gana.DEVA, Gana.MANUSHYA) -> 5.0
            g == Gana.MANUSHYA && b == Gana.RAKSHASA -> 1.0
            else -> 0.0 // Deva & Rakshasa (either), or groom Rakshasa + bride Manushya
        }
    return KootaScore(Koota.GANA, points, "Gana ${g.name.lowercase()} & ${b.name.lowercase()}")
}

// ---- Bhakoot (max 7): emotional/prosperity by sign distance. The 2/12, 5/9 and 6/8 axes are the
// Bhakoot dosha (0 points); every other relationship scores the full 7.

private fun bhakootKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val distance = ((bride.moonRasiIndex - groom.moonRasiIndex + 12) % 12) + 1
    val dosha = distance in setOf(2, 5, 6, 8, 9, 12)
    val note = if (dosha) "Inauspicious sign axis" else "Signs in harmony"
    return KootaScore(Koota.BHAKOOT, if (dosha) 0.0 else 7.0, note)
}

// ---- Nadi (max 8): constitutional/genetic. Same nadi is the Nadi dosha (0 points); different = 8.

internal enum class Nadi { AADI, MADHYA, ANTYA }

internal val NADI_BY_NAKSHATRA =
    listOf(
        Nadi.AADI,
        Nadi.MADHYA,
        Nadi.ANTYA,
        Nadi.ANTYA,
        Nadi.MADHYA,
        Nadi.AADI,
        Nadi.AADI,
        Nadi.MADHYA,
        Nadi.ANTYA,
        Nadi.ANTYA,
        Nadi.MADHYA,
        Nadi.AADI,
        Nadi.AADI,
        Nadi.MADHYA,
        Nadi.ANTYA,
        Nadi.ANTYA,
        Nadi.MADHYA,
        Nadi.AADI,
        Nadi.AADI,
        Nadi.MADHYA,
        Nadi.ANTYA,
        Nadi.ANTYA,
        Nadi.MADHYA,
        Nadi.AADI,
        Nadi.AADI,
        Nadi.MADHYA,
        Nadi.ANTYA,
    )

private fun nadiKoota(
    groom: GunaMilanProfile,
    bride: GunaMilanProfile,
): KootaScore {
    val g = NADI_BY_NAKSHATRA[groom.nakshatraNumber - 1]
    val b = NADI_BY_NAKSHATRA[bride.nakshatraNumber - 1]
    val points = if (g == b) 0.0 else 8.0
    val note = if (g == b) "Same nadi (${g.name.lowercase()})" else "Different nadi"
    return KootaScore(Koota.NADI, points, note)
}
