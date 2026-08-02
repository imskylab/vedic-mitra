// :feature:alarm — muhurta reminders.
//
// Owns the reminders screen: lists the day's muhurta windows and lets the user schedule a
// notification for when one begins. Schedules through the core scheduler and reads the day's
// muhurtas from the astronomy engine at the device location. The `vedicmitra.android.feature`
// convention plugin supplies Compose, Hilt, lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.alarm"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.location)
    implementation(projects.core.datastore)
    implementation(projects.core.scheduler)
    implementation(projects.core.notifications)
}
