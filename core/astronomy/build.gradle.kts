// :core:astronomy — astronomy / panchanga port (contracts only).
//
// Defines the abstraction other layers depend on to obtain astronomical data (sunrise, tithi,
// nakshatra, etc.). Phase 1 provides ONLY the interface and value types — no ephemeris or
// calculation code. The implementation is added in a dedicated later phase.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
}

android {
    namespace = "io.github.vedicmitra.core.astronomy"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
    implementation(libs.kotlinx.datetime)
}
