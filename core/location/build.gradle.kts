// :core:location — location port (contracts only).
//
// Abstraction over obtaining the device's location, needed by astronomy (observer position) and
// settings. Phase 1 defines only the interface; the concrete fused-location implementation is added
// later.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
}

android {
    namespace = "io.github.vedicmitra.core.location"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
}
