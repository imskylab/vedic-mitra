// :feature:rashifal — the daily/weekly rashifal (Moon-transit horoscope).
//
// Owns the screen that shows a rashi's transit outlook: today's verdict and the week ahead, graded by
// Chandrabala (and Tarabala when the read sign is the selected profile's own birth Moon sign). Reads
// chart-ready profiles from :core:datastore, resolves the location through :core:domain's
// ResolveLocationUseCase, and computes the outlook through :core:astronomy's `rashiOutlook`. The
// `vedicmitra.android.feature` convention plugin supplies Compose, Hilt, lifecycle/navigation, and the
// shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.rashifal"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
}
