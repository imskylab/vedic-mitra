/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock

import io.github.vedicmitra.feature.cosmicclock.domain.PanchangaClockModel

/**
 * What the Cosmic Clock screen shows.
 *
 * @property isLoading whether the first load is still running. Only the first: a refresh with a
 *   clock already on screen happens silently, as it does on Home.
 * @property model the clock itself, or `null` before the first successful load. A clock with no
 *   progress looks finished rather than pending, so there is no partial state between these two.
 * @property locationLabel where these values are computed for — the panchanga differs by longitude,
 *   so the screen has to say which place it means.
 * @property usingDefaultLocation whether that location is the fallback rather than the user's.
 * @property errorMessage why the load failed, if it did.
 */
data class CosmicClockUiState(
    val isLoading: Boolean = false,
    val model: PanchangaClockModel? = null,
    val locationLabel: String = "",
    val usingDefaultLocation: Boolean = false,
    val errorMessage: String? = null,
)
