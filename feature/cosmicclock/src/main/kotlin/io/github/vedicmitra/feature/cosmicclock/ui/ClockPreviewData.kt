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

package io.github.vedicmitra.feature.cosmicclock.ui

import io.github.vedicmitra.core.astronomy.LimbWindow
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.feature.cosmicclock.domain.ClockRing
import io.github.vedicmitra.feature.cosmicclock.domain.PadaMarker
import io.github.vedicmitra.feature.cosmicclock.domain.PanchangaClockModel
import kotlin.time.Instant

/**
 * Fixtures for the previews.
 *
 * There is no UI test infrastructure in this repo, so previews in Android Studio are the only way
 * anyone sees these states before a device build. Each fixture is a case that would otherwise only
 * turn up in the wild: a limb on the point of rolling over, and a latitude where the Sun does not
 * rise. Building them by hand rather than from a snapshot keeps them exact.
 */
internal object ClockPreviewData {
    /** A middling moment: every ring part-way through its division. */
    fun typical(): PanchangaClockModel = model()

    /** Every ring nearly full — the frame before several limbs turn over at once. */
    fun aboutToRollOver(): PanchangaClockModel =
        model(karana = 0.99, tithi = 0.98, nakshatra = 0.97, yoga = 0.99, vara = 0.99)

    /**
     * A latitude where the Sun does not rise, so the vedic day has no boundary.
     *
     * The vara ring keeps its place and fills whole rather than vanishing: the weekday is still
     * known, only how far through it we are is not. Dropping the ring would reshuffle every other
     * ring's radius as a reader crossed a latitude.
     */
    fun polar(): PanchangaClockModel = model(varaWindow = null)

    private fun model(
        karana: Double = 0.8,
        tithi: Double = 0.25,
        nakshatra: Double = 0.6,
        yoga: Double = 0.4,
        vara: Double = 0.5,
        varaWindow: LimbWindow? = window(vara),
    ) = PanchangaClockModel(
        at = AT,
        rings =
            listOf(
                ring(PanchangaConcept.KARANA, "Karana", 60, 26, "Bava", window(karana)),
                ring(PanchangaConcept.TITHI, "Tithi", 30, 13, "Chaturdashi", window(tithi)),
                ring(PanchangaConcept.NAKSHATRA, "Nakshatra", 27, 3, "Rohini", window(nakshatra)),
                ring(PanchangaConcept.YOGA, "Yoga", 27, 11, "Dhriti", window(yoga)),
                ring(PanchangaConcept.VARA, "Vara", 7, 5, "Shukravara", varaWindow),
            ),
        pada = PadaMarker(index = 2, window = window(0.4)),
    )

    @Suppress("LongParameterList")
    private fun ring(
        concept: PanchangaConcept,
        label: String,
        segments: Int,
        active: Int,
        name: String,
        window: LimbWindow?,
    ) = ClockRing(
        concept = concept,
        label = label,
        segmentCount = segments,
        activeIndex = active,
        activeName = name,
        window = window,
    )

    private fun window(fraction: Double) =
        LimbWindow(
            start = AT,
            end = Instant.fromEpochMilliseconds(AT.toEpochMilliseconds() + 86_400_000L),
            angularFraction = fraction,
        )

    private val AT = Instant.fromEpochMilliseconds(1_787_000_000_000L)
}

/** Each ring's fill, taken straight from the model — what the screen passes when nothing is animating. */
internal fun PanchangaClockModel.staticProgress(): List<Float> = rings.map { (it.fraction ?: 1.0).toFloat() }
