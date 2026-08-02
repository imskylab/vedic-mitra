/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.dependencies

/**
 * Aggregate convention plugin for **feature** modules. A feature module is an Android library that
 * needs Compose, Hilt, lifecycle/navigation, and the project's core modules — so this plugin
 * composes the library + compose + hilt conventions and adds the feature-common dependencies.
 *
 * Apply via `id("vedicmitra.android.feature")`. Individual feature scripts then only declare their
 * own extra dependencies.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            with(pluginManager) {
                apply("vedicmitra.android.library")
                apply("vedicmitra.android.compose")
                apply("vedicmitra.android.hilt")
            }

            dependencies {
                // Shared core modules every feature depends on.
                addProject("implementation", ":core:common")
                addProject("implementation", ":core:ui")
                addProject("implementation", ":core:designsystem")

                add("implementation", libs.findBundle("lifecycle").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())
                add("implementation", libs.findBundle("coroutines").get())
            }
        }

    /** Small helper so feature scripts read cleanly when wiring project dependencies. */
    private fun DependencyHandler.addProject(
        configuration: String,
        path: String,
    ) {
        add(configuration, project(mapOf("path" to path)))
    }
}
