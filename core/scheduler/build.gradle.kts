// :core:scheduler — time-based task scheduling.
//
// Implements the TaskScheduler port with AlarmManager: schedules an exact (or, when exact alarms
// are not permitted, best-effort) broadcast at a future instant that posts a notification through
// the Notifier port. Depends on :core:notifications because a scheduled reminder *is* a
// notification to be shown when it fires.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra.core.scheduler"
}

dependencies {
    api(projects.core.common)
    api(projects.core.notifications)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.coroutines)
}
