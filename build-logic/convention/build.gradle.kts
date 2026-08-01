// build-logic:convention — defines the reusable `vedicmitra.*` Gradle plugins.
//
// Each plugin encapsulates configuration that would otherwise be copy-pasted into every module
// (Android library setup, Compose enablement, Hilt wiring, ...). Modules apply them by id, e.g.
// `id("vedicmitra.android.library")`.

plugins {
    `kotlin-dsl`
}

group = "io.github.vedicmitra.buildlogic"

// Align the Java toolchain used to compile the convention plugins with the project's JDK 21 target.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Puts the plugin implementations on the convention plugins' compile classpath so they can be
    // applied by id (e.g. pluginManager.apply("com.google.dagger.hilt.android")) and their DSLs
    // (Android, Kotlin, Compose, KSP, Detekt) are type-safe.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

// Register each convention plugin with a stable id that modules reference.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "vedicmitra.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "vedicmitra.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "vedicmitra.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "vedicmitra.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "vedicmitra.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "vedicmitra.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
    }
}
