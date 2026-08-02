/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
