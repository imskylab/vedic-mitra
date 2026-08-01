/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
