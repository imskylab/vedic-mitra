// :feature:japa — the japa (mala chant counter).
//
// Owns the screen that counts malas: a tap counter with a bundled mantra catalog, per-sitting history
// and streaks (persisted via :core:datastore's JapaRepository), and panchanga hooks — stamping each
// sitting with the day's nakshatra/tithi, suggesting the current mahadasha lord's beeja mantra from the
// primary profile's chart, and surfacing today's Brahma Muhurta window (both via :core:astronomy,
// located through :core:domain's ResolveLocationUseCase). The `vedicmitra.android.feature` convention
// plugin supplies Compose, Hilt, lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.japa"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
}
