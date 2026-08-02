// :core:astronomy — astronomy / panchanga engine.
//
// Defines the AstronomyEngine port and its default implementation: a low-precision Meeus-based
// solar/lunar ephemeris that derives sun times, tithi, nakshatra, and vara (Phase 2 slice).

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra.core.astronomy"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
}
