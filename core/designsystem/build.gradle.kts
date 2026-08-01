// :core:designsystem — the Vedic Mitra Material 3 design system.
//
// Owns the app's theme: colour schemes (light/dark + dynamic colour), typography, shapes, and
// spacing tokens. UI in features and :core:ui composes on top of these tokens rather than
// hard-coding colours or dimensions. Contains no feature/business logic.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
    alias(libs.plugins.vedicmitra.android.compose)
}

android {
    namespace = "io.github.vedicmitra.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
}
