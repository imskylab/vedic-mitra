// :feature:meditation — a timed meditation with a breath-pacing guide.
//
// Owns the screen that runs a countdown sit with an animated breath guide and a generated (ToneGenerator)
// start/end bell, logs each sit to :core:datastore's MeditationRepository (history + streaks, stamped
// with the day's nakshatra/tithi), and offers a Brahma Muhurta hook — surfacing today's window (via
// :core:astronomy, located through :core:domain's ResolveLocationUseCase) and scheduling a pre-dawn
// reminder through :core:scheduler + :core:notifications (persisted so it re-arms). The
// `vedicmitra.android.feature` convention plugin supplies Compose, Hilt, lifecycle/navigation, and the
// shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.meditation"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
    implementation(projects.core.scheduler)
    implementation(projects.core.notifications)
}
