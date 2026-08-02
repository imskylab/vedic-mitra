// :feature:settings — app settings feature.
//
// Owns the settings screen UI and presentation, reading and writing user preferences through the
// :core:datastore repository. The `vedicmitra.android.feature` convention plugin supplies Compose,
// Hilt, lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.settings"
}

dependencies {
    implementation(projects.core.datastore)
}
