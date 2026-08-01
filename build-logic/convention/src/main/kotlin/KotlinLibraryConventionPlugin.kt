/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Convention plugin for **pure Kotlin/JVM** modules — no Android dependencies. Suitable for
 * platform-agnostic domain/utility code. Configures the JDK 21 toolchain, shared Kotlin compiler
 * flags, Detekt, and the standard unit-test bundle.
 *
 * Apply via `id("vedicmitra.kotlin.library")`.
 */
class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(ProjectConfig.JVM_TARGET.toInt())
        }

        configureKotlinCompiler()
        configureDetekt()

        dependencies {
            add("testImplementation", libs.findBundle("unit-test").get())
        }
    }
}
