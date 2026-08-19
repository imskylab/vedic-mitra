// :feature:stotra — a bundled library of hymns and shlokas (stotra).
//
// Owns the browsable stotra library and its reader (Devanagari + transliteration + a short significance
// note), plus a weekday-graha "today's stotra" suggestion. The content is a static in-module catalog of
// traditional (public-domain) Sanskrit, so no core data/engine modules are needed — the
// `vedicmitra.android.feature` convention plugin already supplies Compose, Hilt, navigation, and the
// shared design-system/UI modules.

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.stotra"
}
