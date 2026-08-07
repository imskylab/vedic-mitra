// Vedic Mitra — Gradle settings
//
// Declares the plugin + dependency repositories and the full module graph. Feature-first
// layout: shared code lives under :core, screens under :feature, and :app wires them together.

pluginManagement {
    // The included build that provides our `vedicmitra.*` convention plugins.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Auto-provisions the JDK 21 toolchain from a well-known distribution when it isn't installed
// locally, so the build works regardless of the developer's default JDK. Must appear immediately
// after pluginManagement and before any other statement.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Modules must declare dependencies only via the version catalog; project-level repos are banned
    // so every artifact resolves from the same, auditable set of repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "vedic-mitra"

// Speeds up first-run type-safe project accessors.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// --- App ---------------------------------------------------------------------
include(":app")

// --- Core (shared, cross-feature) --------------------------------------------
include(":core:common")
include(":core:datastore")
include(":core:ui")
include(":core:designsystem")
include(":core:astronomy")
include(":core:scheduler")
include(":core:notifications")
include(":core:alarm")
include(":core:location")
include(":core:domain")

// --- Features (feature-first screens) ----------------------------------------
include(":feature:home")
include(":feature:calendar")
include(":feature:settings")
include(":feature:alarm")
include(":feature:location")
