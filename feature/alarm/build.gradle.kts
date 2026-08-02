// :feature:alarm — alarm feature.
//
// Owns the alarm screen UI and presentation. Schedules alarms and posts notifications through the
// core scheduler/notifications ports. The `vedicmitra.android.feature` convention plugin supplies
// Compose, Hilt, lifecycle/navigation, and the shared core modules.
//
// IMPORTANT: alarm behaviour itself is NOT implemented in Phase 1 — only the screen skeleton and
// its dependency wiring.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.alarm"
}

dependencies {
    implementation(projects.core.scheduler)
    implementation(projects.core.notifications)
}
