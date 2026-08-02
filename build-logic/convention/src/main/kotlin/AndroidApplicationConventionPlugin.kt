/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for the single Android **application** module (:app). Configures the
 * applicationId, versioning, and SDK levels, plus the standard unit-test dependencies and Detekt.
 *
 * AGP 9 provides built-in Kotlin, so no separate `org.jetbrains.kotlin.android` plugin is applied;
 * Kotlin's `jvmTarget` follows `compileOptions.targetCompatibility`.
 *
 * Apply via `id("vedicmitra.android.application")`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = ProjectConfig.COMPILE_SDK
                defaultConfig {
                    applicationId = ProjectConfig.NAMESPACE_PREFIX
                    minSdk = ProjectConfig.MIN_SDK
                    targetSdk = ProjectConfig.TARGET_SDK
                    versionCode = 1
                    versionName = "0.1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
                testOptions {
                    // Robolectric needs the merged manifest/resources on the unit-test classpath.
                    unitTests.isIncludeAndroidResources = true
                }
            }

            configureDetekt()

            dependencies {
                add("testImplementation", libs.findBundle("unit-test").get())
                add("testImplementation", libs.findLibrary("robolectric").get())
                add("testImplementation", libs.findLibrary("androidx-test-core").get())
            }
        }
}
