/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.domain

import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.SavedLocation
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.LocationRepository
import io.github.vedicmitra.core.location.LocationProvider
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject

/**
 * Resolves which location the panchanga should be computed for, in priority order:
 *
 * 1. the user's **selected saved location**, if one is chosen;
 * 2. otherwise the **device location** (requires a granted permission and an available fix);
 * 3. otherwise a built-in **default** (New Delhi), flagged so the UI can say so.
 *
 * Consolidates the fallback that Home and Calendar previously each hard-coded, and supplies the
 * per-location time zone so day boundaries land in the right local time.
 */
class ResolveLocationUseCase
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
        private val locationProvider: LocationProvider,
    ) {
        /** Returns the location to compute for, applying the resolution order above. */
        suspend operator fun invoke(): ResolvedLocation {
            selectedSavedLocation()?.let { saved ->
                return ResolvedLocation(
                    coordinates = saved.coordinates,
                    zoneId = saved.zoneId,
                    label = saved.label,
                    isDefault = false,
                )
            }

            return when (val device = locationProvider.currentLocation()) {
                is AppResult.Success ->
                    ResolvedLocation(
                        coordinates = device.data,
                        zoneId = ZoneId.systemDefault().id,
                        label = CURRENT_LOCATION_LABEL,
                        isDefault = false,
                    )

                is AppResult.Failure -> DEFAULT
            }
        }

        private suspend fun selectedSavedLocation(): SavedLocation? {
            val selectedId = locationRepository.selectedLocationId.first() ?: return null
            return locationRepository.savedLocations.first().firstOrNull { it.id == selectedId }
        }

        private companion object {
            const val CURRENT_LOCATION_LABEL = "Current location"

            // Used when no location is selected and the device location is unavailable (New Delhi).
            val DEFAULT =
                ResolvedLocation(
                    coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
                    zoneId = "Asia/Kolkata",
                    label = "New Delhi",
                    isDefault = true,
                )
        }
    }
