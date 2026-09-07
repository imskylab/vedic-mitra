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
 * Which festival, observance or Sankranti an entry is — its **identity**, as distinct from what it
 * is called.
 *
 * [id] is frozen. Anything durable that refers to one of these — a glossary entry, a stored
 * preference, a reminder — refers to it by [id], never by [label]. Display copy is the most
 * changeable thing in the app: it gets reworded, corrected, and eventually translated, and a lookup
 * built on it fails *silently*, falling through to a fallback rather than erroring. See
 * `docs/knowledge-standards.md`, "A claim is not its own key", where this enum's absence was the
 * fourth recorded instance.
 *
 * [label] is the English display name and is free to change.
 *
 * [monthlyTithis] is the other half of the fix. The tithis a recurring observance falls on used to
 * live in two `when` blocks — one keyed by tithi, one keyed by *name* — which had to be kept in step
 * by hand and gave the name a second job it should never have had. They are one property here, and
 * both directions are derived from it.
 *
 * The tithi numbers below are the domain rather than constants waiting to be named, so this file
 * carries the module's usual `MagicNumber` suppression.
 *
 * @property monthlyTithis the global tithis (1..30) this observance recurs on, or `null` if it is not
 *   a recurring observance. Krishna tithis are 15 + their number in the fortnight.
 */
enum class FestivalKind(
    val id: String,
    val label: String,
    val monthlyTithis: Set<Int>? = null,
) {
    // Named festivals, in calendar order from Chaitra.
    UGADI("ugadi", "Ugadi / Gudi Padwa"),
    RAMA_NAVAMI("rama-navami", "Rama Navami"),
    AKSHAYA_TRITIYA("akshaya-tritiya", "Akshaya Tritiya"),
    BUDDHA_PURNIMA("buddha-purnima", "Buddha Purnima"),
    GURU_PURNIMA("guru-purnima", "Guru Purnima"),
    RAKSHA_BANDHAN("raksha-bandhan", "Raksha Bandhan"),
    KRISHNA_JANMASHTAMI("krishna-janmashtami", "Krishna Janmashtami"),
    GANESH_CHATURTHI("ganesh-chaturthi", "Ganesh Chaturthi"),
    NAVARATRI("navaratri", "Navaratri begins"),
    VIJAYADASHAMI("vijayadashami", "Vijayadashami"),
    DIWALI("diwali", "Diwali"),
    MAHA_SHIVARATRI("maha-shivaratri", "Maha Shivaratri"),
    HOLI("holi", "Holi"),

    // Recurring lunar observances, with the tithis they fire on.
    VINAYAKA_CHATURTHI("vinayaka-chaturthi", "Vinayaka Chaturthi", setOf(4)),
    EKADASHI("ekadashi", "Ekadashi", setOf(11, 26)),
    PRADOSH("pradosh", "Pradosh", setOf(13, 28)),
    PURNIMA("purnima", "Purnima", setOf(15)),
    SANKASHTI_CHATURTHI("sankashti-chaturthi", "Sankashti Chaturthi", setOf(19)),
    MASIK_SHIVARATRI("masik-shivaratri", "Masik Shivaratri", setOf(29)),
    AMAVASYA("amavasya", "Amavasya", setOf(30)),

    /**
     * The Sun's entry into a new rashi. One kind for eleven of the twelve: they share a blurb, and
     * the rashi that distinguishes them lives in the [Festival.name] rather than here.
     */
    SANKRANTI("sankranti", "Sankranti"),

    /** Makara Sankranti, which opens Uttarayana and is called out separately from the rest. */
    MAKARA_SANKRANTI("makara-sankranti", "Makara Sankranti"),
    ;

    /** Whether this is a recurring monthly observance rather than a one-off in the year. */
    val isRecurring: Boolean get() = monthlyTithis != null

    companion object {
        /** The observance falling on the global [tithi] (1..30), or `null` for an ordinary tithi. */
        fun observanceAt(tithi: Int): FestivalKind? = entries.firstOrNull { tithi in it.monthlyTithis.orEmpty() }
    }
}
