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
 * The birth moments every reference test computes against.
 *
 * Shared so that a chart's inputs are written once. Two tests each carrying their own copy of an
 * epoch would eventually disagree, and the goldens either side of that would then be checked against
 * different charts without anything failing.
 *
 * Five decades, four Indian cities and London — the last so the ascendant is exercised well away from
 * Indian latitudes, where it moves through the signs at a very different rate.
 *
 * @property label how goldens refer to this chart.
 * @property epochMillis the birth instant, UTC.
 */
internal data class ReferenceBirth(
    val label: String,
    val epochMillis: Long,
    val latitude: Double,
    val longitude: Double,
)

internal val REFERENCE_BIRTHS: List<ReferenceBirth> =
    listOf(
        // 1990-05-17 09:20 IST, Hyderabad
        ReferenceBirth("Hyderabad 1990", 642916200000L, 17.3850, 78.4867),
        // 1975-11-02 14:45 IST, Delhi
        ReferenceBirth("Delhi 1975", 184151700000L, 28.6139, 77.2090),
        // 2001-03-21 06:00 IST, Chennai
        ReferenceBirth("Chennai 2001", 985134600000L, 13.0827, 80.2707),
        // 1988-12-31 23:50 IST, Mumbai
        ReferenceBirth("Mumbai 1988", 599595600000L, 19.0760, 72.8777),
        // 1980-06-15 08:30 BST, London
        ReferenceBirth("London 1980", 329902200000L, 51.5074, -0.1278),
    )

/** The computed chart for a reference birth, by [label]. */
internal fun referenceChartFor(label: String): NatalChart {
    val birth = REFERENCE_BIRTHS.first { it.label == label }
    return natalChart(birth.epochMillis, birth.latitude, birth.longitude)
}
