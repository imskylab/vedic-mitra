/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Android **library** modules (everything under :core and the data/domain
 * layers of :feature). Applies the Android library + Kotlin plugins, shared compile options, the
 * standard unit-test dependencies, and Detekt.
 *
 * Apply via `id("vedicmitra.android.library")`.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // Library modules only need compileSdk + minSdk (set in configureKotlinAndroid).
            // targetSdk is an application concern and is deprecated on library defaultConfig.
        }

        configureDetekt()

        dependencies {
            add("testImplementation", libs.findBundle("unit-test").get())
        }
    }
}
