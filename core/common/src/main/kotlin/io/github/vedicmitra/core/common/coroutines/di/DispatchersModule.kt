/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.common.coroutines.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.common.coroutines.DefaultDispatcherProvider
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider

/** Binds the [DispatcherProvider] abstraction to its production implementation. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DispatchersModule {
    @Binds
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
