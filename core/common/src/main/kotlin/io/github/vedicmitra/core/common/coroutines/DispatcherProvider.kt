/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Abstraction over the coroutine dispatchers used by the app. Injecting this instead of referencing
 * [kotlinx.coroutines.Dispatchers] directly keeps code testable — tests can supply a deterministic
 * test dispatcher.
 *
 * A production implementation and its Hilt binding are added in a later phase.
 */
interface DispatcherProvider {
    /** For CPU-bound work. */
    val default: CoroutineDispatcher

    /** For disk and network I/O. */
    val io: CoroutineDispatcher

    /** For interacting with the UI / main thread. */
    val main: CoroutineDispatcher
}
