// :feature:dharma — the samskaras, as a cited reference.
//
// **Mode: Cite + Teach.** It reports what the grhyasutras hold about the life-cycle rites and explains
// each in plain language; it never tells a reader to perform one. Every entry carries a `ContentSource`
// naming the sutra and the passage, and the copy is held to the primer test shape — see
// `docs/knowledge-standards.md` and ADR 0024.
//
// Content is static, so there is no ViewModel and no core data/engine module is needed beyond the
// astronomy types the catalog links to. The `vedicmitra.android.feature` convention plugin already
// supplies Compose, Hilt, navigation, `:core:common` (for `ContentSource`), `:core:ui` and
// `:core:designsystem`.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.dharma"
}
