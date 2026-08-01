// :feature:home — the home screen feature.
//
// Feature-first module: owns the home screen's UI (Compose), presentation (ViewModel), and its
// wiring to the core ports it needs. The `vedicmitra.android.feature` convention plugin brings in
// Compose, Hilt, lifecycle/navigation, and the shared core modules, so this script only declares
// feature-specific dependencies. No business logic in Phase 1.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.home"
}

dependencies {
    // Home will read panchanga/astronomy data through this port once implemented.
    implementation(projects.core.astronomy)
    implementation(projects.core.location)
}
