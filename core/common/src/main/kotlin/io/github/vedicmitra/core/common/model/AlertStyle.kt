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

/** How a scheduled reminder alerts the user when it fires. */
enum class AlertStyle {
    /** A heads-up notification with sound — the quiet default. */
    NOTIFICATION,

    /** A full-screen ringing alarm that plays over the lock screen until dismissed. */
    ALARM,
}
