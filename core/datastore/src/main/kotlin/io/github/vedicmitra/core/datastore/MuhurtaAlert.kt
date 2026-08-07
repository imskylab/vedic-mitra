/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

import io.github.vedicmitra.core.common.model.AlertStyle

/**
 * A user-configured alert style for one muhurta, keyed by its display [name] (e.g. "Brahma
 * Muhurta"). Only names the user has explicitly switched to an alarm are stored; a name absent from
 * [ReminderRepository.alertTypeByName] uses the default [AlertStyle.NOTIFICATION].
 *
 * @property name the muhurta's traditional name, matching [io.github.vedicmitra.core.astronomy.Muhurta.name].
 * @property alert how its reminder should alert the user.
 */
data class MuhurtaAlert(
    val name: String,
    val alert: AlertStyle,
)
