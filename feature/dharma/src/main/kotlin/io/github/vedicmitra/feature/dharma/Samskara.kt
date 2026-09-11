/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.dharma

/**
 * Which samskara an entry is — its **identity**, as distinct from what it is called.
 *
 * [id] is frozen, on the `FestivalKind` model. Nothing durable may refer to one of these by [label]:
 * display copy is the most changeable thing in the app, and a lookup built on it fails *silently*
 * rather than erroring. See `docs/knowledge-standards.md`, "A claim is not its own key".
 *
 * Nothing persists a samskara yet — this slice is a reference, with no record of what has been
 * performed. While that stays true the enum can move to a shared module for free. Once anything is
 * stored against an [id], moving one becomes a migration rather than a rename.
 *
 * ## Why these five, and not sixteen
 *
 * The set is bounded by what could be **read**, not by what could be listed. Each entry below is
 * described in a grhyasutra that was opened and quoted; the rest of the sixteen follow as their
 * passages are found. Karnavedha is the instructive absence — the app already offers to find a
 * muhurta for it, and it does not appear anywhere in the four sutras of SBE 29, so it has no entry
 * here rather than a plausible-looking citation. See ADR 0024.
 *
 * @property id frozen. Anything durable refers to a samskara by this.
 * @property label the Sanskrit name, as the tradition names the rite. Not extracted to `strings.xml`:
 *   a Sanskrit domain term is a name rather than copy (ADR 0019, ADR 0021).
 * @property alias the popular form, where the app already uses one elsewhere. ADR 0019 rule 4 assigns
 *   register by role — the muhurta picker offers *Mundan*, because that is what someone choosing a
 *   date for it calls it, while a reference to the grhyasutras meets *Chudakarana*. Carrying both is
 *   what keeps the two visibly one rite.
 */
enum class Samskara(
    val id: String,
    val label: String,
    val alias: String? = null,
) {
    NAMAKARANA("namakarana", "Namakarana", alias = "Namkaran"),
    ANNAPRASANA("annaprasana", "Annaprasana"),
    CHUDAKARANA("chudakarana", "Chudakarana", alias = "Mundan"),
    UPANAYANA("upanayana", "Upanayana"),
    VIVAHA("vivaha", "Vivaha", alias = "Vivah"),
}
