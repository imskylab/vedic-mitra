/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.matchmaking

import io.github.vedicmitra.core.astronomy.Koota

/**
 * Plain-language significance for each of the eight Ashtakoota kootas — what the koota measures and
 * what a strong or weak score means. Shown when a guna row is tapped. These are general classical
 * descriptions of the koota itself, independent of any particular pairing.
 */
internal object KootaSignificance {
    fun of(koota: Koota): String =
        when (koota) {
            Koota.VARNA ->
                "Varna (1 guna) compares the spiritual and mental temperament of the couple, grouping the " +
                    "Moon signs into four varnas — Brahmin (water signs), Kshatriya (fire), Vaishya (earth) " +
                    "and Shudra (air). The point is earned when the groom's varna is equal to or higher than " +
                    "the bride's, reflecting a natural harmony of ego and disposition. A weak Varna is not a " +
                    "serious obstacle on its own; it simply asks both partners to be mindful of pride and to " +
                    "meet each other with mutual respect."
            Koota.VASHYA ->
                "Vashya (2 gunas) measures the magnetic pull and mutual influence between the partners — how " +
                    "naturally each yields to and cares for the other — from the Vashya group of each Moon " +
                    "sign. A strong Vashya points to easy give-and-take and lasting attraction; a weak Vashya " +
                    "suggests the couple may need to work consciously at balancing who leads and who follows, " +
                    "so that affection never tips into control."
            Koota.TARA ->
                "Tara (3 gunas) weighs the birth-star compatibility that governs the couple's shared health, " +
                    "fortune and longevity. Each partner's nakshatra is counted from the other's and graded by " +
                    "the nine taras. A favourable count promises well-being and a destiny that supports the " +
                    "marriage; a weak Tara advises extra care over health and over the timing of important " +
                    "family decisions."
            Koota.YONI ->
                "Yoni (4 gunas) reflects physical and sexual compatibility, pairing each nakshatra with an " +
                    "animal 'yoni'. Matching or friendly yonis indicate natural intimacy and biological " +
                    "harmony; opposing (enemy) yonis point to differences in physical temperament and desire " +
                    "that the couple can still bridge with patience, tenderness and open communication."
            Koota.GRAHA_MAITRI ->
                "Graha Maitri (5 gunas) assesses the mental and intellectual affinity of the couple through " +
                    "the friendship of their two Moon-sign lords. Friendly lords bring shared values, easy " +
                    "conversation and psychological understanding; unfriendly lords suggest the partners think " +
                    "differently and will grow closest by cultivating empathy and curiosity about each other's " +
                    "way of seeing the world."
            Koota.GANA ->
                "Gana (6 gunas) compares temperament, classing each nakshatra as Deva (divine), Manushya " +
                    "(human) or Rakshasa (fierce) in nature. Like ganas, or the gentler pairings, promise a " +
                    "harmony of disposition; a Deva–Rakshasa mismatch is the classic sign of clashing natures " +
                    "— though a strong Bhakoot or a friendship between the Moon lords can soften it a great deal."
            Koota.BHAKOOT ->
                "Bhakoot (7 gunas), also called Rashi koota, judges the emotional bond and the prosperity, " +
                    "health and progeny of the household from the distance between the two Moon signs. The 6-8, " +
                    "2-12 and 5-9 distances create a Bhakoot dosha that can strain finances or wellbeing — but " +
                    "classical rules cancel it when the sign lords are the same or friendly, in which case the " +
                    "zero points carry no real blemish."
            Koota.NADI ->
                "Nadi (8 gunas) carries the greatest weight and concerns health and progeny — the " +
                    "constitutional compatibility of the couple. Each nakshatra belongs to one of three nadis " +
                    "(Aadi, Madhya, Antya), and partners should ideally have different nadis. A same-nadi (Nadi " +
                    "dosha) is the most serious flaw, linked to health and childbearing difficulties, yet it too " +
                    "is cancelled under classical exceptions such as the same nakshatra with different padas, or " +
                    "the same sign with different stars."
        }
}
