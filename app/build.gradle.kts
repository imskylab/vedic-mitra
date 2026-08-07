// :app — the installable Android application.
//
// This module wires the dependency graph together: it depends on every feature module and the
// core modules they need, hosts the Hilt application + single Activity, and owns app-level
// configuration (manifest, theming entry point, navigation host). It contains no domain logic.

plugins {
    alias(libs.plugins.vedicmitra.android.application)
    alias(libs.plugins.vedicmitra.android.compose)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra"
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

    // App-level Compose + AndroidX.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.navigation.compose)

    // Instrumented UI tests.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
