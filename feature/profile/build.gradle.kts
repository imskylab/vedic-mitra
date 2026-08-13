// :feature:profile — the user's birth profile.
//
// Owns the screen that captures name, date, time and place of birth — the foundation the astrology
// features (Kundali, Rashifal, Muhurta) will build on. Reads and writes the profile through
// :core:datastore. The `vedicmitra.android.feature` convention plugin supplies Compose, Hilt,
// lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.profile"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.location)
}
