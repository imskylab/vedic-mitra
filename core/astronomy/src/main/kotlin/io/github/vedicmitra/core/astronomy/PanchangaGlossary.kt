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
 * Short, offline significance blurbs for the panchanga items the app surfaces — muhurtas, recurring
 * observances, named festivals, and Sankrantis — keyed by the exact display name each is shown with.
 *
 * The text is intentionally brief (a sentence or two), factual, and neutral in tone; it explains what
 * an item is and why it matters, not how to observe it. Sankrantis share one blurb resolved by the
 * "<Rashi> Sankranti" naming, with Makara Sankranti called out specially.
 */
object PanchangaGlossary {
    private const val SANKRANTI_SUFFIX = " Sankranti"

    /** The significance blurb for the item shown as [name], or `null` if none is known. */
    fun significanceOf(name: String): String? =
        ENTRIES[name] ?: ENTRIES[name.withoutOrdinalSuffix()] ?: sankrantiSignificanceOf(name)

    /**
     * `"Dur Muhurta 2"` → `"Dur Muhurta"`.
     *
     * A window that occurs twice in a day is displayed numbered, so the display name stops matching
     * the entry. Saturday is the only weekday with two Dur Muhurtas, and on Saturdays both rows were
     * falling through to the caller's "no significance known" fallback.
     */
    private fun String.withoutOrdinalSuffix(): String = ORDINAL_SUFFIX.replace(this, "")

    private val ORDINAL_SUFFIX = Regex(" \\d+$")

    private fun sankrantiSignificanceOf(name: String): String? =
        when {
            !name.endsWith(SANKRANTI_SUFFIX) -> null
            name.startsWith("Makara") -> MAKARA_SANKRANTI
            else -> GENERIC_SANKRANTI
        }

    private const val GENERIC_SANKRANTI =
        "The Sun's entry into a new rashi (zodiac sign). Each Sankranti is considered auspicious for " +
            "bathing in sacred rivers, charity, and worship."

    private const val MAKARA_SANKRANTI =
        "The Sun's entry into Makara (Capricorn), marking the start of Uttarayana — its northward " +
            "journey. Celebrated across India as a harvest festival (Makar Sankranti, Pongal, Lohri)."

    private val ENTRIES: Map<String, String> =
        mapOf(
            // Muhurtas — daily auspicious/inauspicious windows.
            "Brahma Muhurta" to
                "The roughly 48 minutes before sunrise — traditionally the most auspicious time for " +
                "meditation, japa, and study, when the mind is calm and sattvic.",
            "Abhijit Muhurta" to
                "A short 'victory' window around solar noon, auspicious for beginning important tasks; " +
                "it is said to remove obstacles (but is skipped on Wednesdays).",
            "Rahu Kalam" to
                "An inauspicious period of about 90 minutes ruled by Rahu, traditionally avoided for " +
                "starting new or important undertakings.",
            "Yamaganda" to
                "An inauspicious daytime period associated with Yama; new ventures and travel are " +
                "traditionally avoided during it.",
            "Gulika Kalam" to
                "A period ruled by Gulika (Mandi), a son of Saturn. It is avoided for auspicious " +
                "beginnings, though acts started in it are said to recur.",
            "Dur Muhurta" to
                "A short inauspicious segment of the day; important or auspicious activities are " +
                "traditionally postponed until it passes.",
            "Varjyam" to
                "A 'to-be-avoided' window tied to the current nakshatra; new or auspicious activities " +
                "are traditionally deferred until it ends.",
            // Recurring lunar observances.
            "Ekadashi" to
                "The eleventh lunar day of each fortnight, sacred to Vishnu. Many keep a fast (vrat) " +
                "for health, discipline, and spiritual merit.",
            "Purnima" to
                "The full-moon day, considered auspicious for worship, charity, and vrats; several " +
                "major festivals fall on a Purnima.",
            "Amavasya" to
                "The new-moon day, traditionally set aside for honouring ancestors (tarpana) and for " +
                "quiet reflection.",
            "Sankashti Chaturthi" to
                "The fourth lunar day of the waning fortnight, dedicated to Ganesha. Devotees fast " +
                "until moonrise to seek the removal of obstacles.",
            "Vinayaka Chaturthi" to
                "The fourth lunar day of the waxing fortnight, dedicated to Ganesha and observed with " +
                "worship for wisdom and success.",
            "Pradosh" to
                "The twilight window of the thirteenth lunar day, sacred to Shiva; worship at dusk is " +
                "held to be especially fruitful.",
            "Masik Shivaratri" to
                "The monthly night of Shiva, on the fourteenth day of the waning fortnight, observed " +
                "with worship and a night vigil.",
            // Named festivals.
            "Ugadi / Gudi Padwa" to
                "The lunar new year of the Deccan and Maharashtra (Chaitra Shukla Pratipada), " +
                "welcoming the new samvatsara with festivity and fresh beginnings.",
            "Rama Navami" to
                "The birth of Lord Rama on Chaitra Shukla Navami, marked by worship, readings of the " +
                "Ramayana, and processions.",
            "Akshaya Tritiya" to
                "An 'imperishable' day (Vaishakha Shukla Tritiya) believed to bring lasting merit and " +
                "prosperity; favoured for new ventures and purchases.",
            "Buddha Purnima" to
                "The Vaishakha full moon marking the birth — and by tradition the enlightenment and " +
                "passing — of Gautama Buddha.",
            "Guru Purnima" to
                "The Ashadha full moon honouring one's guru and the sage Vyasa, observed with " +
                "gratitude and learning.",
            "Raksha Bandhan" to
                "The Shravana full moon when sisters tie a rakhi on their brothers' wrists, a symbol " +
                "of protection and affection.",
            "Krishna Janmashtami" to
                "The birth of Lord Krishna on Shravana Krishna Ashtami, celebrated with fasting, " +
                "midnight worship, and festivity.",
            "Ganesh Chaturthi" to
                "The birthday of Ganesha (Bhadrapada Shukla Chaturthi), welcomed with clay idols and " +
                "worship, and closed with a ceremonial immersion.",
            "Navaratri begins" to
                "The first of nine nights honouring the Divine Mother (Durga), from Ashwina Shukla " +
                "Pratipada, with fasting, dance, and worship.",
            "Vijayadashami" to
                "Dussehra — the tenth day celebrating the victory of good over evil (Rama over Ravana, " +
                "Durga over Mahishasura).",
            "Diwali" to
                "The festival of lights on Kartika Amavasya, honouring Lakshmi with lamps, a cleaned " +
                "home, and the celebration of prosperity.",
            "Maha Shivaratri" to
                "The 'great night of Shiva' in Magha, observed with fasting, a night vigil, and " +
                "worship of the Shiva linga.",
            "Holi" to
                "The spring festival of colours on the Phalguna full moon, preceded by Holika Dahan, " +
                "celebrating joy and the triumph of devotion.",
        )
}
