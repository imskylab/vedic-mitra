// :core:ui — shared Compose UI infrastructure.
//
// Reusable composables, preview tooling, and UI helpers built on top of :core:designsystem. This
// is where cross-feature widgets (buttons, loading/empty states, previews) will live. It depends on
// the design system for tokens but never on any feature.
//
// It also holds the panchanga explanatory copy, which moved here from :core:astronomy (ADR 0021):
// the engine is pure Kotlin and must stay that way for F5, so text needing translation cannot live
// in it. If that copy grows much past the primer and the glossary, split it into a :core:content
// module rather than letting this one drift into being the app's text repository.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.compose)
}

android {
    namespace = "io.github.vedicmitra.core.ui"
}

dependencies {
    // `api`, not `implementation`: PanchangaConcept is a parameter of PanchangaPrimer.of, so every
    // caller needs the type. The engine stays unaware of this module.
    api(projects.core.astronomy)
    implementation(projects.core.designsystem)
    implementation(libs.bundles.lifecycle)
}
