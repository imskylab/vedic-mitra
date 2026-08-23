/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.designsystem.component

import androidx.compose.ui.text.style.TextAlign

/**
 * One column of a [VedicTable].
 *
 * @property header the column heading.
 * @property weight how much of the row's width it takes, relative to the other columns.
 * @property alignment how the cell text aligns — numbers usually read better centred or trailing.
 */
data class TableColumn(
    val header: String,
    val weight: Float = 1f,
    val alignment: TextAlign = TextAlign.Start,
)
