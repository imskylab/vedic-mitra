/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.DefaultAstronomyEngine

/** Binds the [AstronomyEngine] port to its default implementation. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AstronomyModule {
    @Binds
    abstract fun bindAstronomyEngine(impl: DefaultAstronomyEngine): AstronomyEngine
}
