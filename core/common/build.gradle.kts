// :core:common — cross-cutting foundation shared by every module.
//
// Holds framework-agnostic building blocks: result wrappers, coroutine dispatcher abstractions,
// and shared value types. It has no dependency on Compose or any feature. Keep this module small
// and dependency-light so anything may depend on it without pulling in UI.

plugins {
    alias(libs.plugins.vedicmitra.android.library)
}

android {
    namespace = "io.github.vedicmitra.core.common"
}

dependencies {
    implementation(libs.bundles.coroutines)
}
