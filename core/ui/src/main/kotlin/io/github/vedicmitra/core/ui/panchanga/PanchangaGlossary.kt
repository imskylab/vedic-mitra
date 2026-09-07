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
import io.github.vedicmitra.core.astronomy.FestivalKind
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.ui.R

/**
 * Short significance blurbs for the panchanga items the app surfaces — muhurtas, recurring
 * observances, named festivals, and Sankrantis.
 *
 * Here rather than in `:core:astronomy` for the same reason as [PanchangaPrimer]: it is copy, and the
 * engine must stay free of Android resources (ADR 0021).
 *
 * **Keyed on identity, never on the name an item is shown with.** That distinction is the whole
 * point of this object's shape. A lookup keyed on display copy does not fail loudly when the copy
 * changes — it falls through to the caller's fallback, so the app says "no significance known" about
 * something it demonstrably knows, in its own voice. A **Cite** claim silently becomes a denial of
 * knowledge, which no mode declaration or `source` field can protect against. See
 * `docs/knowledge-standards.md`, "A claim is not its own key".
 *
 * The two overloads take the two identities that exist: [MuhurtaKind] for the day's windows and
 * [FestivalKind] for everything on a calendar. Both are total — every entry of both enums has a
 * blurb, and a test iterates them, so adding a kind without writing its copy breaks the build.
 */
object PanchangaGlossary {
    /** The significance blurb for the muhurta window [kind]. */
    @StringRes
    fun significanceOf(kind: MuhurtaKind): Int =
        when (kind) {
            MuhurtaKind.BRAHMA -> R.string.glossary_brahma_muhurta
            MuhurtaKind.ABHIJIT -> R.string.glossary_abhijit_muhurta
            MuhurtaKind.RAHU_KALAM -> R.string.glossary_rahu_kalam
            MuhurtaKind.YAMAGANDA -> R.string.glossary_yamaganda
            MuhurtaKind.GULIKA_KALAM -> R.string.glossary_gulika_kalam
            MuhurtaKind.DUR_MUHURTA -> R.string.glossary_dur_muhurta
            MuhurtaKind.VARJYAM -> R.string.glossary_varjyam
        }

    /** The significance blurb for the festival, observance or Sankranti [kind]. */
    @StringRes
    fun significanceOf(kind: FestivalKind): Int =
        when (kind) {
            FestivalKind.UGADI -> R.string.glossary_ugadi_gudi_padwa
            FestivalKind.RAMA_NAVAMI -> R.string.glossary_rama_navami
            FestivalKind.AKSHAYA_TRITIYA -> R.string.glossary_akshaya_tritiya
            FestivalKind.BUDDHA_PURNIMA -> R.string.glossary_buddha_purnima
            FestivalKind.GURU_PURNIMA -> R.string.glossary_guru_purnima
            FestivalKind.RAKSHA_BANDHAN -> R.string.glossary_raksha_bandhan
            FestivalKind.KRISHNA_JANMASHTAMI -> R.string.glossary_krishna_janmashtami
            FestivalKind.GANESH_CHATURTHI -> R.string.glossary_ganesh_chaturthi
            FestivalKind.NAVARATRI -> R.string.glossary_navaratri_begins
            FestivalKind.VIJAYADASHAMI -> R.string.glossary_vijayadashami
            FestivalKind.DIWALI -> R.string.glossary_diwali
            FestivalKind.MAHA_SHIVARATRI -> R.string.glossary_maha_shivaratri
            FestivalKind.HOLI -> R.string.glossary_holi
            FestivalKind.VINAYAKA_CHATURTHI -> R.string.glossary_vinayaka_chaturthi
            FestivalKind.EKADASHI -> R.string.glossary_ekadashi
            FestivalKind.PRADOSH -> R.string.glossary_pradosh
            FestivalKind.PURNIMA -> R.string.glossary_purnima
            FestivalKind.SANKASHTI_CHATURTHI -> R.string.glossary_sankashti_chaturthi
            FestivalKind.MASIK_SHIVARATRI -> R.string.glossary_masik_shivaratri
            FestivalKind.AMAVASYA -> R.string.glossary_amavasya
            FestivalKind.SANKRANTI -> R.string.glossary_sankranti_generic
            FestivalKind.MAKARA_SANKRANTI -> R.string.glossary_sankranti_makara
        }
}
