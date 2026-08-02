// :core:datastore — user preferences persistence.
//
// Owns the app's user-preferences store (theme, dynamic colour) behind a repository, backed by
// Jetpack DataStore. Shared by :app (to apply the theme) and :feature:settings (to change it).

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra.core.datastore"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
    implementation(libs.androidx.datastore.preferences)
}
