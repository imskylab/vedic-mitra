// :feature:calendar — the Hindu calendar feature.
//
// Owns the calendar screen: a monthly panchang grid (each day shows its tithi) with a detail view
// of the selected day's full panchanga. Reads from the astronomy engine at the device location.
// The `vedicmitra.android.feature` convention plugin supplies Compose, Hilt, lifecycle/navigation,
// and the shared core modules, so this script only declares feature-specific dependencies.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.calendar"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
    implementation(projects.core.location)
    // For the runtime location-permission request (rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)
}
