// :core:notifications — notification port (contracts only).
//
// Abstraction over posting/cancelling notifications and managing channels. Phase 1 defines only the
// interface; the concrete NotificationManager-backed implementation is added later.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
}

android {
    namespace = "io.github.vedicmitra.core.notifications"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
}
