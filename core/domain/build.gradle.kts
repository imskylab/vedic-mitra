// :core:domain — cross-feature use cases (the Domain layer).
//
// Holds framework-light use cases that orchestrate several core ports/repositories, so no single
// capability module has to depend on another (the dependency rules forbid that). The location
// resolver lives here because it combines the device location (:core:location) with the user's
// saved-location choice (:core:datastore).

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.hilt)
}

android {
    namespace = "io.github.vedicmitra.core.domain"
}

dependencies {
    api(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.location)
    implementation(libs.bundles.coroutines)
}
