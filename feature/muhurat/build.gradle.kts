// :feature:muhurat — electional muhurta ("find best dates").
//
// Owns the flow that lets the user pick an activity (category → activity) and see the most
// auspicious upcoming days for it, ranked by the muhurta scorer in :core:astronomy. Resolves the
// location through :core:domain's ResolveLocationUseCase. The `vedicmitra.android.feature` convention
// plugin supplies Compose, Hilt, lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.muhurat"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
}
