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

package io.github.vedicmitra.feature.rashifal

import io.github.vedicmitra.core.astronomy.Bala
import io.github.vedicmitra.core.astronomy.OutlookBand
import io.github.vedicmitra.core.astronomy.RashiDay
import io.github.vedicmitra.core.astronomy.RashiOutlook
import io.github.vedicmitra.core.astronomy.Tara

/**
 * Turns the engine's computed outlook factors into plain interpretive text. Everything here is a pure,
 * deterministic function of the classical positions the engine computed (Chandrabala house, Tarabala
 * grade, verdict band) — no invented predictions, just the traditional meaning of the transit read
 * back in words.
 */
internal object RashifalText {
    /** A short headline for the day's overall verdict. */
    fun headline(band: OutlookBand): String =
        when (band) {
            OutlookBand.AUSPICIOUS -> "A favourable day"
            OutlookBand.MIXED -> "A mixed day"
            OutlookBand.CHALLENGING -> "A day for care"
        }

    /**
     * The traditional meaning of the Moon transiting the [position]th house from the read sign — the
     * heart of the daily reading. The framing matches the classical Chandrabala grade, so a "strong"
     * position always reads encouragingly and a "weak" one cautiously.
     */
    fun chandraNarrative(position: Int): String =
        when (position) {
            1 ->
                "The Moon rides over your own sign today — a strong Chandrabala. You feel emotionally " +
                    "attuned and sure of yourself; a fine day to begin what is close to your heart."
            2 -> "The Moon moves through your 2nd — a steady day for family, money, and well-chosen words."
            3 ->
                "The Moon is in your 3rd — a strong Chandrabala. Courage and initiative are with you, " +
                    "and effort is rewarded."
            4 ->
                "The Moon is in your 4th — a weaker Chandrabala. Guard your peace at home and avoid " +
                    "overreaching; let big moves wait."
            5 ->
                "The Moon is in your 5th — a reflective, neutral day. Think things through before acting " +
                    "on impulse."
            6 ->
                "The Moon is in your 6th — a strong Chandrabala. You get the better of obstacles and " +
                    "rivals, and your resolve is high."
            7 ->
                "The Moon is in your 7th — a strong Chandrabala. Partnership, travel, and simple comforts " +
                    "are favoured."
            8 ->
                "The Moon is in your 8th — a weaker Chandrabala. Conserve your energy and hold off on " +
                    "risky commitments."
            9 -> "The Moon is in your 9th — a neutral day of effort; patience earns more than haste today."
            10 ->
                "The Moon is in your 10th — a strong Chandrabala. Work and reputation are favoured; act " +
                    "on your ambitions."
            11 ->
                "The Moon is in your 11th — a strong Chandrabala. Gains, friendship, and fulfilled hopes " +
                    "mark the day."
            12 ->
                "The Moon is in your 12th — a weaker Chandrabala. Rest, wind down, and keep an eye on " +
                    "expenses."
            else -> ""
        }

    /** The Tarabala line, shown only for a personalised reading (the person's own sign). */
    fun taraNarrative(tara: Tara): String =
        when (tara.strength) {
            Bala.STRONG ->
                "Your Tarabala is favourable (${tara.name} tara) — the day's star supports what " +
                    "you set out to do."
            Bala.WEAK -> "Your Tarabala is weak (${tara.name} tara) — hold back on anything that truly matters."
            Bala.NEUTRAL ->
                "The day's star is your own birth star (${tara.name} tara) — neither a push nor " +
                    "a clear block."
        }

    /** A one-line summary of how the week ahead breaks down, and which day looks best. */
    fun weekSummary(outlook: RashiOutlook): String {
        val week = outlook.week
        val favourable = week.count { it.band == OutlookBand.AUSPICIOUS }
        val challenging = week.count { it.band == OutlookBand.CHALLENGING }
        val mixed = week.size - favourable - challenging
        return "Over the next ${week.size} days: $favourable favourable, $mixed mixed, $challenging to " +
            "watch."
    }

    /** Whether a day is one of the week's brightest (an auspicious band). */
    fun isBright(day: RashiDay): Boolean = day.band == OutlookBand.AUSPICIOUS
}
