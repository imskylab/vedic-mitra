/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The first test in `:app`, which until now had no test source set at all.
 *
 * It covers the app bar's title, which is the one piece of navigation logic that is plain data and a
 * pure function — so it can be tested without rendering anything. The rest of the graph cannot be:
 * `AppNavHost` composes `HomeScreen` immediately, and that (like sixteen other screens reachable
 * from the graph) calls `hiltViewModel()`, which needs Hilt test infrastructure, a test-only
 * `@AndroidEntryPoint` host activity, and real bindings for the whole object graph down to
 * `core:location`'s native timezone library. Back handling, the bottom-tab behaviour and rotation
 * are therefore still only covered by a manual pass on a device.
 *
 * Creating this source set is worth something beyond the assertions below: the application
 * convention plugin enables `unitTests.isIncludeAndroidResources` for any module that has a
 * `src/test` directory, so `:app` can now host a Robolectric test without a build-file change.
 */
class NavigationTitleTest {
    @Test
    fun `every bottom-bar tab has a title`() {
        // A tab with no entry would show the app name alone, with a blank line beneath it.
        TopDestination.entries.forEach { destination ->
            assertWithMessage("${destination.name} (${destination.route})")
                .that(subtitleOf(destination.route))
                .isEqualTo(destination.label)
        }
    }

    @Test
    fun `every declared route has a title`() {
        // The drift this guards against: adding a route and forgetting its label, which shows a
        // blank subtitle on that screen and nothing else -- no crash, no warning.
        //
        // The routes are read by reflection rather than listed here on purpose. A hand-written list
        // would have to be updated by the same person who forgot the label, so it would pass exactly
        // when it needed to fail.
        val routes = declaredRoutes()
        assertWithMessage("no route constants found -- has MainActivity.kt been renamed?")
            .that(routes)
            .isNotEmpty()

        routes.forEach { (name, route) ->
            assertWithMessage("$name = \"$route\" has no DESTINATION_LABELS entry")
                .that(subtitleOf(route))
                .isNotNull()
        }
    }

    @Test
    fun `a route's arguments are trimmed before the title is looked up`() {
        // Routes reach subtitleOf as their declared pattern, not as the navigated path, so the
        // argument part has to come off first. Both forms the graph actually registers are covered.
        assertThat(subtitleOf("$PROFILE_EDIT_ROUTE?$PROFILE_ID_ARG={$PROFILE_ID_ARG}"))
            .isEqualTo("Birth profile")
        assertThat(subtitleOf("$MUHURAT_DAY_ROUTE/{activity}/{day}")).isEqualTo("Muhurat")
        assertThat(subtitleOf("$MUHURAT_RESULTS_ROUTE/{activity}")).isEqualTo("Muhurat")
    }

    @Test
    fun `an unknown or absent route has no title`() {
        // Null is the "show the app name alone" case, and it must not throw on a route that has been
        // removed from the label table.
        assertThat(subtitleOf(null)).isNull()
        assertThat(subtitleOf("not-a-route")).isNull()
        assertThat(subtitleOf("")).isNull()
    }

    @Test
    fun `the Home sub-views are titled as destinations`() {
        // These three were state inside HomeScreen until they became routes. Their titles used to be
        // reported up out of the screen through a special case in subtitleOf; now they come from the
        // same table as everything else, and each screen draws no title of its own.
        assertThat(subtitleOf(PANCHANG_ROUTE)).isEqualTo("Today's Panchang")
        assertThat(subtitleOf(FESTIVALS_ROUTE)).isEqualTo("Festivals")
        assertThat(subtitleOf(EVENTS_ROUTE)).isEqualTo("Events")
    }

    @Test
    fun `no title is blank and no route is declared twice`() {
        assertThat(DESTINATION_LABELS.values.filter { it.isBlank() }).isEmpty()

        val routes = declaredRoutes().map { it.second }
        assertWithMessage("two constants share a route string")
            .that(routes)
            .containsNoDuplicates()
    }

    /**
     * Every `*_ROUTE` constant declared in `MainActivity.kt`, as name-to-value pairs.
     *
     * Top-level constants compile to static fields on the file's facade class, so reflecting over it
     * enumerates them without this test having to know what they are. `*_ARG` constants are excluded
     * — they name navigation arguments, not destinations.
     */
    private fun declaredRoutes(): List<Pair<String, String>> =
        Class
            .forName("io.github.vedicmitra.MainActivityKt")
            .declaredFields
            .filter { it.name.endsWith("_ROUTE") && it.type == String::class.java }
            .mapNotNull { field ->
                field.isAccessible = true
                (field.get(null) as? String)?.let { value -> field.name to value }
            }
}
