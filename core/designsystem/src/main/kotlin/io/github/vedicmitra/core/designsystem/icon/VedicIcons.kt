/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.designsystem.icon

import androidx.annotation.DrawableRes
import io.github.vedicmitra.core.designsystem.R

/**
 * The brand's custom cultural glyphs (duotone maroon + gold), inspired by a traditional rolled
 * Panchanga almanac. Consume via `painterResource(VedicIcons.Panchang)`.
 *
 * These cover the *signature/spiritual* features only; utilitarian icons (calendar, bell, search,
 * settings, location, share) use Material Symbols instead, so the ornate style stays special.
 *
 * Stotra/Om is intentionally **not** here: ॐ ships as a Devanagari text glyph (a bundled font),
 * not a drawable, so it renders identically on every device rather than as a hand-approximated path.
 */
object VedicIcons {
    /** A rolled almanac scroll — the Panchang. */
    @get:DrawableRes
    val panchang: Int = R.drawable.ic_glyph_panchang

    /** A clock with an auspicious star — an electional Muhurat. */
    @get:DrawableRes
    val muhurat: Int = R.drawable.ic_glyph_muhurat

    /** A marigold worship flower — Festivals. */
    @get:DrawableRes
    val festivals: Int = R.drawable.ic_glyph_festivals

    /** The North-Indian birth-chart diamond — Kundali. */
    @get:DrawableRes
    val kundali: Int = R.drawable.ic_glyph_kundali

    /** A mala (prayer-bead ring) — Japa. */
    @get:DrawableRes
    val japa: Int = R.drawable.ic_glyph_japa

    /** A figure seated in padmasana before a rising sun — Meditation. */
    @get:DrawableRes
    val meditate: Int = R.drawable.ic_glyph_meditate

    /** Two clasped hands with a mehndi mandala (wedding hastamelap) — Matchmaking. */
    @get:DrawableRes
    val matchmaking: Int = R.drawable.ic_glyph_matchmaking

    /** The twelve rashis arranged in a zodiac wheel — Rashifal (horoscope). */
    @get:DrawableRes
    val rashifal: Int = R.drawable.ic_glyph_rashifal

    /** A mandala-bordered almanac page with Om marks and a turning leaf — the Calendar. */
    @get:DrawableRes
    val calendar: Int = R.drawable.ic_glyph_calendar

    /** A woodcut almanac page with a central Om and endless-knot corners — Events. */
    @get:DrawableRes
    val events: Int = R.drawable.ic_glyph_events

    /**
     * A coin dropping into a donation box (daana-patra) — Support.
     *
     * Unlike the other bundled glyphs this one is an **alpha stencil**, not coloured artwork: the
     * lid, slot and lettering are holes, so it tints like a Material symbol and reads on either
     * theme. It is the one glyph meant for navigation chrome rather than a hub tile, and chrome has
     * to take the bar's colours.
     */
    @get:DrawableRes
    val support: Int = R.drawable.ic_glyph_support
}
