// build-logic — settings for the convention-plugins included build.
//
// This is a standalone Gradle build wired into the main build via `includeBuild("build-logic")`
// in the root settings file. It shares the root's version catalog so plugin versions stay in one
// place.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Same JDK 21 auto-provisioning as the main build.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
