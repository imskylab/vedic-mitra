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
 * A panchanga idea a reader may want explained, as distinct from a named item like "Rahu Kalam".
 *
 * The **key stays in the engine while the words do not**. `LimbCycle` names one of these per row, so
 * the engine has to be able to say *which* idea a row is about; it has no business holding the
 * English sentences that explain it. Those live in `:core:ui`'s `PanchangaPrimer` — see ADR 0021 and
 * #215, which moved them so `:core:astronomy` keeps no Android dependency.
 *
 * Closed on purpose: the primer covers every entry, and a test over `entries` fails the build if a
 * new concept is added without copy.
 */
enum class PanchangaConcept {
    PANCHANGA,
    TITHI,
    PAKSHA,
    VARA,
    NAKSHATRA,
    PADA,
    YOGA,
    KARANA,
    LUNAR_MONTH,
    SUNRISE_DAY,
    MOON_PHASE,
    RASHI,
    RITU,
    SANKALPA,
}
