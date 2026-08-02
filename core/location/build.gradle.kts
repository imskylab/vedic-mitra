// :core:location — device location.
//
// Implements the LocationProvider port with Google Play Services fused location. Callers are
// responsible for holding the runtime location permission before requesting a location.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra.core.location"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
}
