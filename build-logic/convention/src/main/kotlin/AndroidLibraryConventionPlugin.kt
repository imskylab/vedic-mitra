/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Android **library** modules (everything under :core and the data/domain
 * layers of :feature). Applies the Android library plugin, shared compile options, the standard
 * unit-test dependencies, and Detekt.
 *
 * AGP 9 provides built-in Kotlin, so no separate `org.jetbrains.kotlin.android` plugin is applied.
 *
 * Apply via `id("vedicmitra.android.library")`.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = ProjectConfig.COMPILE_SDK
                defaultConfig {
                    minSdk = ProjectConfig.MIN_SDK
                    // targetSdk is an application concern and is deprecated on library defaultConfig.
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
            }

            configureDetekt()

            dependencies {
                add("testImplementation", libs.findBundle("unit-test").get())
            }
        }
}
