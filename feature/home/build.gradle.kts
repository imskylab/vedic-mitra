// :feature:home — the home screen feature.
//
// Feature-first module: owns the home screen's UI (Compose), presentation (ViewModel), and its
// wiring to the core ports it needs. The `vedicmitra.android.feature` convention plugin brings in
// Compose, Hilt, lifecycle/navigation, and the shared core modules, so this script only declares
// feature-specific dependencies.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.home"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
    implementation(projects.core.location)
    // For the runtime location-permission request (rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)
}
