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
 * The top-level groupings a user browses when choosing an electional muhurta. Ordering here is the
 * order shown in the UI.
 *
 * @property displayName the human-readable category name.
 */
enum class MuhurtaCategory(
    val displayName: String,
) {
    BAL_SANSKAR("Child samskaras"),
    VIVAH("Vivah"),
    VASTU("Vastu"),
    PURCHASES("Purchases"),
    MEDICAL("Medical"),
    BUSINESS("Business"),
    AGRICULTURE("Agriculture"),
    CEREMONIES("Ceremonies & Milestones"),
}

/**
 * A specific activity the user wants an auspicious time for, grouped under a [MuhurtaCategory]. Each
 * activity maps to a set of electional rules (see `muhurtaRulesFor`) used to rank candidate days.
 *
 * @property category the group this activity belongs to.
 * @property displayName the human-readable activity name.
 */
enum class MuhurtaActivity(
    val category: MuhurtaCategory,
    val displayName: String,
) {
    // Child samskaras — the rites of early life, in life order.
    JANANA_SHANTI(MuhurtaCategory.BAL_SANSKAR, "Janana Shanti"),
    NAMKARAN(MuhurtaCategory.BAL_SANSKAR, "Namkaran"),
    ANNAPRASHANA(MuhurtaCategory.BAL_SANSKAR, "Annaprashana"),
    KARNAVEDHA(MuhurtaCategory.BAL_SANSKAR, "Karnavedha"),
    MUNDAN(MuhurtaCategory.BAL_SANSKAR, "Mundan"),
    UPANAYANA(MuhurtaCategory.BAL_SANSKAR, "Upanayana"),

    // Vivah — betrothal and marriage.
    VAAGDAAN(MuhurtaCategory.VIVAH, "Vaagdaan"),
    VIVAH(MuhurtaCategory.VIVAH, "Vivah"),

    // Vastu — build and occupy.
    BHOOMI_POOJAN(MuhurtaCategory.VASTU, "Bhoomi Poojan"),
    VASTU_SHANTI(MuhurtaCategory.VASTU, "Vastu Shanti"),
    GRIHA_PRAVESH(MuhurtaCategory.VASTU, "Griha Pravesh"),

    // Purchases.
    PROPERTY_PURCHASE(MuhurtaCategory.PURCHASES, "Property"),
    VEHICLE_PURCHASE(MuhurtaCategory.PURCHASES, "Vehicle"),
    JEWELRY_PURCHASE(MuhurtaCategory.PURCHASES, "Jewelry"),
    MACHINERY_PURCHASE(MuhurtaCategory.PURCHASES, "Machinery"),
    LIVESTOCK_PURCHASE(MuhurtaCategory.PURCHASES, "Livestock"),

    // Medical — elective only; guidance never overrides professional medical advice.
    AUSHADHI_SEVAN(MuhurtaCategory.MEDICAL, "Aushadhi Sevan"),
    SHASTRAKRIYA(MuhurtaCategory.MEDICAL, "Shastrakriya"),
    CESAREAN_DELIVERY(MuhurtaCategory.MEDICAL, "Cesarean delivery"),

    // Business.
    BUSINESS_INAUGURATION(MuhurtaCategory.BUSINESS, "Business inauguration"),
    SHOP_OPENING(MuhurtaCategory.BUSINESS, "Shop opening"),
    FACTORY_INAUGURATION(MuhurtaCategory.BUSINESS, "Factory inauguration"),
    MACHINERY_COMMISSIONING(MuhurtaCategory.BUSINESS, "Machinery commissioning"),

    // Agriculture.
    FARMLAND_PURCHASE(MuhurtaCategory.AGRICULTURE, "Farmland purchase"),
    SOWING(MuhurtaCategory.AGRICULTURE, "Sowing"),
    GARDENING(MuhurtaCategory.AGRICULTURE, "Gardening"),
    HARVESTING(MuhurtaCategory.AGRICULTURE, "Harvesting"),
    WELL_CONSTRUCTION(MuhurtaCategory.AGRICULTURE, "Well construction"),
    PRODUCT_SALE(MuhurtaCategory.AGRICULTURE, "Product sale"),

    // Ceremonies & milestones.
    DOHALE_JEVAN(MuhurtaCategory.CEREMONIES, "Dohale Jevan"),
    FIFTIETH_YEAR_SHANTI(MuhurtaCategory.CEREMONIES, "50th-year Shanti"),
    SHASHTI_POORTI(MuhurtaCategory.CEREMONIES, "Shashti Poorti"),
    ;

    companion object {
        /** The activities in [category], in declaration order — for the category → activity picker. */
        fun inCategory(category: MuhurtaCategory): List<MuhurtaActivity> = entries.filter { it.category == category }
    }
}
