/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for the single Android **application** module (:app). Configures the
 * applicationId, versioning, SDK levels, and the standard unit-test dependencies, then delegates
 * shared compile options to [configureKotlinAndroid].
 *
 * Apply via `id("vedicmitra.android.application")`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)

                defaultConfig {
                    applicationId = ProjectConfig.NAMESPACE_PREFIX
                    targetSdk = ProjectConfig.TARGET_SDK
                    versionCode = 1
                    versionName = "0.1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            configureDetekt()

            dependencies {
                add("testImplementation", libs.findBundle("unit-test").get())
            }
        }
}
