// :core:alarm — the in-app ringing alarm.
//
// A muhurta reminder set to "alarm" mode fires as a full-screen ringing alarm rather than a quiet
// notification: AlarmAlert posts a full-screen-intent notification that launches AlarmActivity over
// the lock screen, which plays the system alarm ringtone (looping) and vibrates until dismissed.
// The scheduler's receiver calls into this module; the module never depends on any feature.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.compose)
}

android {
    namespace = "io.github.vedicmitra.core.alarm"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.notifications)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
