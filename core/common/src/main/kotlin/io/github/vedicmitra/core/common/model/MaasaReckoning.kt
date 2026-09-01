/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.common.model

/**
 * Which lunar-month scheme a month name follows.
 *
 * The two agree on when a month *is* — the tithis, the festivals and the year boundary are the same
 * — and disagree only on what the dark fortnight is **called**. Lives here rather than in
 * `:core:astronomy` because it is a persisted user preference, and `:core:datastore` may not depend
 * on the engine.
 */
enum class MaasaReckoning {
    /** New moon to new moon. The scheme this app computes in, and its default. */
    AMANTA,

    /** Full moon to full moon, used across much of North India. */
    PURNIMANTA,
}
