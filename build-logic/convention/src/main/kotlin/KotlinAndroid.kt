/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Applies the Kotlin + Android compile options shared by application and library modules:
 * SDK levels, the JDK 21 toolchain, and common Kotlin compiler flags.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK

        defaultConfig {
            minSdk = ProjectConfig.MIN_SDK
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

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
