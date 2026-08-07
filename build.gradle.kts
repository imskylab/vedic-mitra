// Vedic Mitra — root build script
//
// This file intentionally declares plugins with `apply false`: it registers them on the
// classpath so sub-modules (and our convention plugins) can apply them, without applying them
// to the root project itself. Cross-cutting quality tooling (Spotless, Detekt) is configured
// here so every module inherits a single, consistent configuration.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // No kotlin-android plugin: AGP 9 provides built-in Kotlin for Android modules.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

// --- Spotless: formatting gate for all Kotlin + Gradle Kotlin DSL sources ----
spotless {
    val licenseHeader = rootProject.file("config/spotless/copyright.kt")

    kotlin {
        target("**/*.kt")
        // Exclude generated build output and the license-header template itself. Excluding the
        // `build` directory itself (not just its contents) prunes the directory walk so Spotless
        // never descends into build intermediates — avoiding a race where a concurrent codegen
        // task rewrites/deletes files under build/ mid-scan ("Could not read path").
        targetExclude("**/build", "**/build/**/*.kt", "config/spotless/copyright.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    // Pin the code style so Spotless matches standalone ktlint; ktlint 1.x defaults to
                    // "ktlint_official" but Spotless still defaults to "intellij_idea".
                    "ktlint_code_style" to "ktlint_official",
                    // Compose @Composable functions are PascalCase by convention; ktlint would flag them.
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                ),
            )
        licenseHeaderFile(licenseHeader)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build", "**/build/**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(mapOf("ktlint_code_style" to "ktlint_official"))
    }
    format("misc") {
        target("**/*.md", "**/*.yml", "**/*.yaml", "**/.gitignore")
        targetExclude("**/build", "**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// --- Detekt: static analysis gate --------------------------------------------
// The `detekt` task on the root aggregates via each module applying the plugin through the
// convention plugins. Shared config + baseline live under config/detekt.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    parallel = true
}

dependencies {
    // detekt-formatting adds ktlint-backed rules to Detekt runs.
    detektPlugins(libs.detekt.formatting)
}
