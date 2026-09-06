/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.ui.panchanga

import androidx.annotation.StringRes
import io.github.vedicmitra.core.ui.R

/**
 * Short significance blurbs for the panchanga items the app surfaces -- muhurtas, recurring
 * observances, named festivals, and Sankrantis.
 *
 * Here rather than in `:core:astronomy` for the same reason as [PanchangaPrimer]: it is copy, and
 * the engine must stay free of Android resources (ADR 0021).
 *
 * ## Keyed by display name, which is a known fault
 *
 * Every key below is the exact string an item is *shown with*. That is the same mistake as keying a
 * reminder on its label, fixed for muhurtas in #211: translate a display name and the lookup stops
 * matching, so every row silently falls back to "no significance known" instead of showing the
 * blurb -- the app asserting, in its own voice, that it does not know something it does. Extracting
 * the values here does not fix it: the keys are the problem, and giving festivals and observances
 * stable identities is its own change, filed as #225. Do not add entries keyed on anything new until
 * it lands.
 *
 * The general rule this breaks is in `docs/knowledge-standards.md`, under "A claim is not its own
 * key" -- this is the fourth instance, and the last one blocking a translated build.
 *
 * Sankrantis share one blurb resolved by the "&lt;Rashi&gt; Sankranti" naming, with Makara
 * Sankranti called out specially.
 */
object PanchangaGlossary {
    private const val SANKRANTI_SUFFIX = " Sankranti"

    /** The significance blurb for the item shown as [name], or `null` if none is known. */
    @StringRes
    fun significanceOf(name: String): Int? =
        ENTRIES[name] ?: ENTRIES[name.withoutOrdinalSuffix()] ?: sankrantiSignificanceOf(name)

    /**
     * `"Dur Muhurta 2"` -> `"Dur Muhurta"`.
     *
     * A window that occurs twice in a day is displayed numbered, so the display name stops matching
     * the entry. Saturday is the only weekday with two Dur Muhurtas, and on Saturdays both rows were
     * falling through to the caller's "no significance known" fallback.
     */
    private fun String.withoutOrdinalSuffix(): String = ORDINAL_SUFFIX.replace(this, "")

    private val ORDINAL_SUFFIX = Regex(" \\d+$")

    @StringRes
    private fun sankrantiSignificanceOf(name: String): Int? =
        when {
            !name.endsWith(SANKRANTI_SUFFIX) -> null
            name.startsWith("Makara") -> R.string.glossary_sankranti_makara
            else -> R.string.glossary_sankranti_generic
        }

    private val ENTRIES: Map<String, Int> =
        mapOf(
            // Muhurtas -- daily auspicious/inauspicious windows.
            "Brahma Muhurta" to R.string.glossary_brahma_muhurta,
            "Abhijit Muhurta" to R.string.glossary_abhijit_muhurta,
            "Rahu Kalam" to R.string.glossary_rahu_kalam,
            "Yamaganda" to R.string.glossary_yamaganda,
            "Gulika Kalam" to R.string.glossary_gulika_kalam,
            "Dur Muhurta" to R.string.glossary_dur_muhurta,
            "Varjyam" to R.string.glossary_varjyam,
            // Recurring lunar observances.
            "Ekadashi" to R.string.glossary_ekadashi,
            "Purnima" to R.string.glossary_purnima,
            "Amavasya" to R.string.glossary_amavasya,
            "Sankashti Chaturthi" to R.string.glossary_sankashti_chaturthi,
            "Vinayaka Chaturthi" to R.string.glossary_vinayaka_chaturthi,
            "Pradosh" to R.string.glossary_pradosh,
            "Masik Shivaratri" to R.string.glossary_masik_shivaratri,
            // Named festivals.
            "Ugadi / Gudi Padwa" to R.string.glossary_ugadi_gudi_padwa,
            "Rama Navami" to R.string.glossary_rama_navami,
            "Akshaya Tritiya" to R.string.glossary_akshaya_tritiya,
            "Buddha Purnima" to R.string.glossary_buddha_purnima,
            "Guru Purnima" to R.string.glossary_guru_purnima,
            "Raksha Bandhan" to R.string.glossary_raksha_bandhan,
            "Krishna Janmashtami" to R.string.glossary_krishna_janmashtami,
            "Ganesh Chaturthi" to R.string.glossary_ganesh_chaturthi,
            "Navaratri begins" to R.string.glossary_navaratri_begins,
            "Vijayadashami" to R.string.glossary_vijayadashami,
            "Diwali" to R.string.glossary_diwali,
            "Maha Shivaratri" to R.string.glossary_maha_shivaratri,
            "Holi" to R.string.glossary_holi,
        )
}
