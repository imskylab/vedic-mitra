// :core:ui — shared Compose UI infrastructure.
//
// Reusable composables, preview tooling, and UI helpers built on top of :core:designsystem. This
// is where cross-feature widgets (buttons, loading/empty states, previews) will live. It depends on
// the design system for tokens but never on any feature.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.compose)
}

android {
    namespace = "io.github.vedicmitra.core.ui"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(libs.bundles.lifecycle)
}
