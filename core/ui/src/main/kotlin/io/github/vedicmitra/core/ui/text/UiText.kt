/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.ui.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text a **non-composable** layer wants to show — a ViewModel's validation message, an error in UI
 * state — named rather than resolved.
 *
 * `stringResource` needs a composition, so a ViewModel cannot call it, and giving a ViewModel a
 * `Context` to call `getString` with is worse: it ties the class to Android, makes it need a context
 * in tests, and resolves the string against whatever locale was configured when the ViewModel ran
 * rather than when the text is drawn. A [UiText] says *which* string; the composable that draws it
 * says what that means in the current locale.
 *
 * A ViewModel test then asserts on a resource id, which is a stronger assertion than a string: it
 * cannot pass by accident because two different messages happen to read the same, and it does not
 * break when the copy is reworded.
 */
sealed interface UiText {
    /**
     * A string from `strings.xml`, with any format arguments.
     *
     * [args] are substituted positionally, so the translation must use `%1$s`-style indexed
     * placeholders — a translator will reorder them, and unindexed `%s` cannot be reordered.
     */
    data class Res(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    /**
     * Text that is already data rather than copy — a place name from the geocoder, a profile's own
     * name, an IANA zone id. There is nothing to translate, so there is nothing to look up.
     *
     * Not an escape hatch for copy that has not been extracted yet. If a translator would need to
     * see it, it is a [Res].
     */
    data class Raw(
        val value: String,
    ) : UiText
}

/**
 * Resolves [this] against the current composition's locale and configuration.
 *
 * The spread is suppressed rather than avoided: `stringResource` takes a vararg and there is no
 * overload that accepts a list, so the copy is unavoidable. It is a handful of elements, made once
 * per recomposition of a single string.
 */
@Suppress("SpreadOperator")
@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Raw -> value
        is UiText.Res -> if (args.isEmpty()) stringResource(id) else stringResource(id, *args.toTypedArray())
    }
