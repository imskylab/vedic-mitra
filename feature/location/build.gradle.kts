// :feature:location — location management feature.
//
// Owns the screens for choosing where the panchanga is computed: the saved-locations list (select /
// delete / use current), adding a location by city search, and adding one by custom latitude and
// longitude. Reads and writes saved locations through :core:datastore and geocodes city names
// through :core:location. The `vedicmitra.android.feature` convention plugin supplies Compose, Hilt,
// lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.location"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.location)
}
