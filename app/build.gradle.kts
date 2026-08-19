// :app — the installable Android application.
//
// This module wires the dependency graph together: it depends on every feature module and the
// core modules they need, hosts the Hilt application + single Activity, and owns app-level
// configuration (manifest, theming entry point, navigation host). It contains no domain logic.

import java.util.Properties

plugins {
    alias(libs.plugins.vedicmitra.android.application)
    alias(libs.plugins.vedicmitra.android.compose)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra"

    defaultConfig {
        // AndroidX/Material bundle dozens of translations; this app's UI is English-only, so ship only
        // English resources and drop the rest. Revisit when adding UI localisation.
        resourceConfigurations += "en"
    }

    // Release signing is driven by a gitignored keystore.properties (see keystore.properties.example
    // and docs/RELEASING.md). It is read only when present, so CI and contributors without the
    // keystore can still build/test/assembleDebug — release builds are simply left unsigned there.
    // Signing every release with the SAME keystore is what lets a new build update an installed one.
    // Read via providers.fileContents so the file is a tracked configuration-cache input.
    val keystorePropsText =
        providers
            .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
            .asText
            .orNull
    if (keystorePropsText != null) {
        val keystoreProps = Properties().apply { load(keystorePropsText.reader()) }
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    // Core modules.
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)

    // Feature modules — the app is the only place these are assembled together.
    implementation(projects.feature.home)
    implementation(projects.feature.calendar)
    implementation(projects.feature.settings)
    implementation(projects.feature.alarm)
    implementation(projects.feature.location)
    implementation(projects.feature.profile)
    implementation(projects.feature.kundali)
    implementation(projects.feature.muhurat)
    implementation(projects.feature.matchmaking)
    implementation(projects.feature.rashifal)
    implementation(projects.feature.japa)
    implementation(projects.feature.meditation)

    // App-level Compose + AndroidX.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.navigation.compose)

    // Instrumented UI tests.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
