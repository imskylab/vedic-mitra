/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.common.result

/**
 * A discriminated result type for operations that can fail, used across layer boundaries instead of
 * throwing. Repositories and use cases return [AppResult] so callers handle success and failure
 * explicitly.
 *
 * This is foundation only — the domain-specific error hierarchy is introduced alongside the code
 * that produces it in later phases.
 *
 * @param T the type of a successful value.
 */
sealed interface AppResult<out T> {
    /** A successful outcome carrying [data]. */
    data class Success<out T>(
        val data: T,
    ) : AppResult<T>

    /** A failed outcome carrying the [cause] of the failure. */
    data class Failure(
        val cause: Throwable,
    ) : AppResult<Nothing>
}

/** Returns the contained value on success, or `null` on failure. */
fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

/** Maps the success value with [transform], propagating failures unchanged. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }
