// :feature:settings — app settings feature.
//
// Owns the settings screen UI and presentation. Reads/writes user preferences through core ports.
// The `vedicmitra.android.feature` convention plugin supplies Compose, Hilt, lifecycle/navigation,
// and the shared core modules. No business logic in Phase 1.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.settings"
}

dependencies {
    implementation(projects.core.location)
    // Preference persistence is wired here when settings are implemented.
    implementation(libs.androidx.datastore.preferences)
}
