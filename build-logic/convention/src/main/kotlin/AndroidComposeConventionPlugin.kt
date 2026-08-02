/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin that enables Jetpack Compose for a module. Applies the Kotlin Compose compiler
 * plugin (Kotlin 2.x), turns on the `compose` build feature, and wires the Compose BOM plus the
 * common Compose dependency bundle.
 *
 * Apply **after** an Android application/library plugin, via `id("vedicmitra.android.compose")`.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            // The android extension is registered under the name "android" for both application and
            // library modules; both implement CommonExtension, so this cast is safe and avoids relying
            // on getByType resolving a supertype.
            val extension = extensions.getByName("android") as CommonExtension
            extension.buildFeatures.compose = true

            dependencies {
                val bom = libs.findLibrary("androidx-compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                add("implementation", libs.findBundle("compose").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())

                add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
}
