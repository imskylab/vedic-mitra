// :core:notifications — device notifications.
//
// Implements the Notifier port with NotificationManagerCompat: creates the app's notification
// channels and posts/cancels notifications. Callers are responsible for holding the runtime
// POST_NOTIFICATIONS permission (API 33+) before posting; the notifier degrades gracefully to a
// failure result when notifications are disabled.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra.core.notifications"
}

dependencies {
    api(projects.core.common)
    implementation(libs.bundles.coroutines)
    implementation(libs.androidx.core.ktx)
}
