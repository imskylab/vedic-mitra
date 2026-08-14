// :feature:kundali — the birth chart (kundali).
//
// Owns the screen that renders a profile's natal chart: the lagna, the grahas by rashi/house, the
// Moon's nakshatra, and the current Vimshottari dasha. Reads the primary profile from
// :core:datastore and computes the chart through :core:astronomy's `natalChartAt`. The
// `vedicmitra.android.feature` convention plugin supplies Compose, Hilt, lifecycle/navigation, and
// the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.kundali"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.astronomy)
}
