/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock.domain

import io.github.vedicmitra.core.astronomy.PanchangaConcept
import kotlin.time.Instant

/**
 * The whole clock as one spoken sentence, for a screen reader.
 *
 * A `Canvas` is invisible to TalkBack, and the usual workaround — invisible boxes over each arc
 * carrying their own semantics — means maintaining a second hit-test that drifts from the drawing
 * and produces two dozen disconnected fragments to swipe through. One coherent description of the
 * whole face is both less work and better to listen to; the limb list below then provides the
 * per-limb detail as ordinary focusable rows.
 *
 * Pure, so it can be tested, and so the same string can seed a widget or a complication later.
 *
 * Deliberately says when each limb *ends* rather than how long is left. A countdown read aloud is
 * stale the moment it is spoken, and re-announcing the whole face every minute to keep it current
 * would be intolerable; the limb rows carry the countdown, where a listener asks for it one at a
 * time. That is also why this needs no "now" — an end time is a fact about the day, not about the
 * moment of asking.
 *
 * @param formatTime renders an instant as the reader would hear it. Injected because time zones and
 *   locale formatting are a platform concern and this layer has no business knowing about them.
 */
fun PanchangaClockModel.spokenSummary(formatTime: (Instant) -> String): String {
    val parts =
        rings.map { ring ->
            val ending = ring.endsAt?.let { ", ends ${formatTime(it)}" } ?: ""
            val padaSuffix =
                pada
                    ?.takeIf { ring.concept == PanchangaConcept.NAKSHATRA }
                    ?.let { ", pada ${it.index + 1}" }
                    .orEmpty()
            "${ring.label} ${ring.activeName}$padaSuffix$ending"
        }
    // A single sentence per limb, in the order they are drawn, so someone listening can map what
    // they hear onto the rings from the outside in if they later look at the face.
    return (listOf(HEADING) + parts).joinToString(separator = ". ", postfix = ".")
}

private const val HEADING = "Panchanga clock"
