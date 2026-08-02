/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Applies the shared Kotlin/JDK toolchain for Android modules. SDK levels are set per-extension in
 * the application/library convention plugins (AGP 9's non-generic `CommonExtension` no longer
 * exposes `defaultConfig`/`compileOptions`, and the JDK 21 toolchain already drives the Java target,
 * so a separate `compileOptions` block is unnecessary).
 */
internal fun Project.configureKotlinAndroid() {
    // Pin the Kotlin/Java toolchain to JDK 21. Combined with the Foojay resolver in settings, Gradle
    // downloads a matching JDK if one isn't installed, so builds are reproducible across machines.
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(ProjectConfig.JVM_TARGET.toInt())
    }

    configureKotlinCompiler()
}

/** Shared Kotlin compiler flags applied to both Android and pure-JVM modules. */
internal fun Project.configureKotlinCompiler() {
    tasks.withType(KotlinCompile::class.java).configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}
