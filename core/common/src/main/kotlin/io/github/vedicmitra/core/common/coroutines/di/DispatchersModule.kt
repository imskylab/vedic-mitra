/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
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
