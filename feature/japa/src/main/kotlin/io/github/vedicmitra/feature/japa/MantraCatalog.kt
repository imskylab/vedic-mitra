/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.japa

import io.github.vedicmitra.core.astronomy.Graha

/**
 * A mantra offered for japa.
 *
 * @property id a stable identifier, stored with each logged sitting.
 * @property name the common English name.
 * @property devanagari the mantra in Devanagari.
 * @property transliteration a roman transliteration.
 * @property graha the graha this is the beeja (seed) mantra of, or `null` for a general mantra.
 */
data class Mantra(
    val id: String,
    val name: String,
    val devanagari: String,
    val transliteration: String,
    val graha: Graha? = null,
)

/**
 * The bundled catalog of mantras for japa: a few general mantras plus the nine Navagraha beeja mantras.
 * All are traditional (public-domain) Sanskrit. The beeja mantras carry their [Graha] so the counter
 * can suggest the one for a person's current mahadasha lord.
 */
object MantraCatalog {
    /** Every mantra, general ones first, then the nine grahas in order. */
    val all: List<Mantra> =
        listOf(
            Mantra(
                id = "gayatri",
                name = "Gayatri Mantra",
                devanagari = "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि धियो यो नः प्रचोदयात्",
                transliteration =
                    "Om Bhur Bhuva Swaha Tat Savitur Varenyam Bhargo Devasya Dhimahi Dhiyo Yo Nah Prachodayat",
            ),
            Mantra(
                id = "om_namah_shivaya",
                name = "Om Namah Shivaya",
                devanagari = "ॐ नमः शिवाय",
                transliteration = "Om Namah Shivaya",
            ),
            Mantra(
                id = "mahamrityunjaya",
                name = "Mahamrityunjaya Mantra",
                devanagari =
                    "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम् उर्वारुकमिव बन्धनान् मृत्योर्मुक्षीय माऽमृतात्",
                transliteration =
                    "Om Tryambakam Yajamahe Sugandhim Pushtivardhanam " +
                        "Urvarukamiva Bandhanan Mrityor Mukshiya Maamritat",
            ),
            Mantra(
                id = "surya_beeja",
                name = "Surya Beeja (Sun)",
                devanagari = "ॐ ह्रां ह्रीं ह्रौं सः सूर्याय नमः",
                transliteration = "Om Hraam Hreem Hraum Sah Suryaya Namah",
                graha = Graha.SUN,
            ),
            Mantra(
                id = "chandra_beeja",
                name = "Chandra Beeja (Moon)",
                devanagari = "ॐ श्रां श्रीं श्रौं सः चन्द्राय नमः",
                transliteration = "Om Shraam Shreem Shraum Sah Chandraya Namah",
                graha = Graha.MOON,
            ),
            Mantra(
                id = "mangala_beeja",
                name = "Mangala Beeja (Mars)",
                devanagari = "ॐ क्रां क्रीं क्रौं सः भौमाय नमः",
                transliteration = "Om Kraam Kreem Kraum Sah Bhaumaya Namah",
                graha = Graha.MANGALA,
            ),
            Mantra(
                id = "budha_beeja",
                name = "Budha Beeja (Mercury)",
                devanagari = "ॐ ब्रां ब्रीं ब्रौं सः बुधाय नमः",
                transliteration = "Om Braam Breem Braum Sah Budhaya Namah",
                graha = Graha.BUDHA,
            ),
            Mantra(
                id = "guru_beeja",
                name = "Guru Beeja (Jupiter)",
                devanagari = "ॐ ग्रां ग्रीं ग्रौं सः गुरवे नमः",
                transliteration = "Om Graam Greem Graum Sah Gurave Namah",
                graha = Graha.GURU,
            ),
            Mantra(
                id = "shukra_beeja",
                name = "Shukra Beeja (Venus)",
                devanagari = "ॐ द्रां द्रीं द्रौं सः शुक्राय नमः",
                transliteration = "Om Draam Dreem Draum Sah Shukraya Namah",
                graha = Graha.SHUKRA,
            ),
            Mantra(
                id = "shani_beeja",
                name = "Shani Beeja (Saturn)",
                devanagari = "ॐ प्रां प्रीं प्रौं सः शनैश्चराय नमः",
                transliteration = "Om Praam Preem Praum Sah Shanaischaraya Namah",
                graha = Graha.SHANI,
            ),
            Mantra(
                id = "rahu_beeja",
                name = "Rahu Beeja",
                devanagari = "ॐ भ्रां भ्रीं भ्रौं सः राहवे नमः",
                transliteration = "Om Bhraam Bhreem Bhraum Sah Rahave Namah",
                graha = Graha.RAHU,
            ),
            Mantra(
                id = "ketu_beeja",
                name = "Ketu Beeja",
                devanagari = "ॐ स्रां स्रीं स्रौं सः केतवे नमः",
                transliteration = "Om Sraam Sreem Sraum Sah Ketave Namah",
                graha = Graha.KETU,
            ),
        )

    /** The default mantra when none has been chosen. */
    val default: Mantra = all.first()

    private val byId: Map<String, Mantra> = all.associateBy { it.id }
    private val byGraha: Map<Graha, Mantra> = all.mapNotNull { m -> m.graha?.let { it to m } }.toMap()

    /** The mantra with [id], or `null` if it isn't in the catalog. */
    fun byId(id: String): Mantra? = byId[id]

    /** The beeja mantra for [graha], or `null` if none is catalogued. */
    fun forGraha(graha: Graha): Mantra? = byGraha[graha]
}
