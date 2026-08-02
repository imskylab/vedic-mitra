# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Toolchain lifted to AGP 9 / Gradle 9 / Kotlin 2.3.** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.3.10 +
  KSP 2.3.10 (now using AGP's built-in Kotlin — the standalone `kotlin-android` plugin was removed),
  and Hilt 2.60.1. Migrated the astronomy/scheduler ports from `kotlinx.datetime.Instant` to the
  stdlib `kotlin.time.Instant`.

### Notes
- Held the latest androidx/Compose line: it requires compiling against **compileSdk 37**, which is
  currently only a preview (`android-CANARY`). The project stays on stable compileSdk 36 and adopts
  those libraries once API 37 ships as a stable platform. See
  [docs/migration/agp-9-migration.md](docs/migration/agp-9-migration.md).

## [0.1.0] - 2026-08-01

### Added
- **Phase 1 — repository foundation.**
  - Gradle (Kotlin DSL) build with a centralized version catalog and `build-logic/` convention
    plugins (`vedicmitra.android.application/library/compose/hilt/feature`, `vedicmitra.kotlin.library`).
  - Modular, feature-first Clean Architecture skeleton: `:app`; `:core:common`, `:core:ui`,
    `:core:designsystem`, `:core:astronomy`, `:core:scheduler`, `:core:notifications`,
    `:core:location`; `:feature:home`, `:feature:settings`, `:feature:alarm`.
  - Hilt DI scaffolding (application root, `@HiltViewModel` skeletons) and a Material 3 design
    system (`VedicMitraTheme`) with light/dark and dynamic colour.
  - Contract-only ports for astronomy, scheduler, notifications, and location (no implementations).
  - Quality tooling: Detekt, Spotless, Ktlint, and a shared `.editorconfig`.
  - GitHub Actions CI (Android SDK setup → Spotless → Detekt → unit tests → assemble APK, APK +
    test-report artifacts), a tag-triggered Release workflow that publishes an installable APK,
    Dependabot, and issue/PR templates.
  - IDE-optional workflow: VS Code settings, recommended extensions, and build tasks
    (`.vscode/tasks.json`) so the app builds without Android Studio.
  - Documentation: README, CONTRIBUTING, CODE_OF_CONDUCT, AGENTS.md, and `docs/` (architecture,
    getting started incl. Android-Studio-free setup, module guide, ADRs).
  - Gradle 8.14 wrapper.

### Notes
- Business logic, astronomy calculations, and alarm behaviour are intentionally **not** included in
  this release — see the roadmap in the README.

[Unreleased]: https://github.com/vedicmitra/vedic-mitra/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/vedicmitra/vedic-mitra/releases/tag/v0.1.0
