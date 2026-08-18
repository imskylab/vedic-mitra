// :feature:matchmaking — Ashtakoota kundali matching (Guna Milan).
//
// Picks one male and one female chart-ready profile, casts each Moon position through
// :core:astronomy's natal-chart engine, and scores the 36-guna match with `gunaMilan`. Profiles come
// from :core:datastore. The `vedicmitra.android.feature` convention plugin supplies Compose, Hilt,
// lifecycle/navigation, and the shared core modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.matchmaking"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.datastore)
}
