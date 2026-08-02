// :core:scheduler — task scheduling port (contracts only).
//
// Abstraction over "run this at a specific time" used by the alarm feature. Phase 1 defines only
// the interface; the concrete AlarmManager/WorkManager-backed implementation is added later.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
}

android {
    namespace = "io.github.vedicmitra.core.scheduler"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
}
