# Vedic Mitra

> A modern Android companion for Vedic timekeeping — panchanga, muhurta, and astronomy-aware
> reminders — built with Kotlin, Jetpack Compose, and Clean Architecture.

[![CI](https://github.com/vedicmitra/vedic-mitra/actions/workflows/ci.yml/badge.svg)](https://github.com/vedicmitra/vedic-mitra/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

> **Status: Phase 1 — Foundation.** This repository currently contains the architectural
> foundation only: build system, modular skeleton, quality tooling, CI, and documentation.
> Astronomy, scheduling, and alarm behaviour are intentionally **not** implemented yet — see the
> [Roadmap](#roadmap).

---

## Project Vision

Vedic Mitra ("Vedic Friend") aims to make traditional Vedic timekeeping genuinely useful on a
modern phone. Rather than static almanac tables, it computes the panchanga (tithi, nakshatra, yoga,
karana, vara) and auspicious windows (muhurta) for the user's exact location and time, and lets
them set astronomy-aware reminders and alarms around those windows.

Guiding principles:

- **Accuracy first** — astronomy is computed, not looked up, from the observer's coordinates.
- **Offline-friendly** — core calculations run on-device.
- **Respectful, uncluttered UX** — Material 3, light/dark, dynamic colour.
- **Maintainable by many** — strict modular Clean Architecture so features stay independent.

## Architecture

Vedic Mitra follows **Clean Architecture** with a **feature-first**, **multi-module** layout, using
**MVVM** for presentation, the **Repository pattern** for data, and **Hilt** for dependency
injection.

### Layer rules

```
UI (Compose)  →  ViewModel (MVVM)  →  UseCase / Domain  →  Repository  →  Data source / Port
```

- Dependencies point **inwards**: UI depends on domain abstractions, never the reverse.
- Features never depend on other features — only on `:core:*` modules.
- Cross-cutting capabilities (astronomy, scheduling, notifications, location) are exposed as
  **ports** (interfaces) in `:core`, so implementations are swappable and testable.

### Module graph

```
                       ┌─────────┐
                       │  :app   │  (assembles everything, Hilt root, single Activity)
                       └────┬────┘
          ┌─────────────────┼──────────────────┐
   ┌──────▼──────┐   ┌──────▼───────┐   ┌───────▼──────┐
   │ :feature:   │   │ :feature:    │   │ :feature:    │
   │   home      │   │  settings    │   │   alarm      │
   └──────┬──────┘   └──────┬───────┘   └──────┬───────┘
          │                 │                  │
          └───────── depend on core ports ─────┘
   ┌──────────────────────────────────────────────────────┐
   │ :core:common  :core:ui  :core:designsystem            │
   │ :core:astronomy  :core:scheduler                      │
   │ :core:notifications  :core:location                   │
   └──────────────────────────────────────────────────────┘
```

| Module | Responsibility |
| --- | --- |
| `:app` | Application shell: Hilt root, single Activity, navigation host, module assembly. |
| `:core:common` | Framework-agnostic building blocks: `AppResult`, dispatcher abstractions, value types. |
| `:core:ui` | Reusable Compose widgets and preview tooling. |
| `:core:designsystem` | Material 3 theme: colour, typography, shapes, spacing tokens. |
| `:core:astronomy` | **Port** for panchanga/astronomy calculations (contracts only in Phase 1). |
| `:core:scheduler` | **Port** for scheduling work at a time (contracts only in Phase 1). |
| `:core:notifications` | **Port** for posting notifications (contracts only in Phase 1). |
| `:core:location` | **Port** for device location (contracts only in Phase 1). |
| `:feature:home` | Home screen (panchanga overview). |
| `:feature:settings` | Settings screen (preferences, location, theme). |
| `:feature:alarm` | Alarm screen (astronomy-aware reminders). |

Build configuration is not copy-pasted between modules — it lives in **convention plugins** under
[`build-logic/`](build-logic), applied by id (e.g. `vedicmitra.android.feature`). See
[docs/architecture.md](docs/architecture.md) and [docs/module-guide.md](docs/module-guide.md).

## Tech Stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.3.10 (JDK 21) |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture, MVVM, Repository pattern |
| DI | Hilt (+ KSP) |
| Async | Kotlin Coroutines / Flow |
| Build | AGP 9.3.1 on Gradle 9.6.1 (Kotlin DSL), Version Catalog, convention plugins |
| Quality | Detekt, Spotless, Ktlint |
| Testing | JUnit4, Truth, MockK, Turbine, Coroutines-test |
| CI | GitHub Actions |

## Roadmap

- **Phase 1 — Foundation (this repo).** Build system, modules, DI/theme scaffolding, quality
  tooling, CI, docs. No business logic.
- **Phase 2 — Astronomy engine.** Implement `:core:astronomy` (ephemeris, panchanga elements).
- **Phase 3 — Location & settings.** Implement `:core:location`, wire `:feature:settings`.
- **Phase 4 — Home.** Real panchanga overview UI in `:feature:home`.
- **Phase 5 — Scheduling & alarms.** Implement `:core:scheduler` + `:core:notifications` and the
  `:feature:alarm` experience.
- **Phase 6 — Polish & release.** Widgets, localisation, Play Store release pipeline.

## Build Instructions

> **Android Studio is not required.** The project is command-line/CI-first — build it from a
> terminal, from VS Code tasks, or entirely on GitHub. Full setup (including installing the Android
> SDK command-line tools without Android Studio) is in [docs/getting-started.md](docs/getting-started.md).

### Build on GitHub (no local setup)

- Push a branch → the [CI workflow](.github/workflows/ci.yml) builds the APK and uploads it as an
  artifact on the Actions run.
- Push a `v*` tag → the [release workflow](.github/workflows/release.yml) attaches an installable
  (debug-signed) APK to a GitHub Release.

### Prerequisites (local CLI build)

- **JDK 21** — Gradle runs on it (auto-detected; a local JDK 21 is recommended).
- **Android SDK command-line tools** with platform **API 36** and `build-tools;36.0.0` — install via
  `sdkmanager`, no Android Studio needed. Set `ANDROID_HOME` or create `local.properties` with
  `sdk.dir=...`.

### Common commands

```bash
# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Formatting + static analysis (the CI quality gate)
./gradlew spotlessCheck detekt

# Auto-fix formatting
./gradlew spotlessApply

# List all modules
./gradlew projects
```

Convenience wrappers live in [`scripts/`](scripts) (`format` and `check`).

The debug APK is written to `app/build/outputs/apk/debug/`.

## Contribution Guide

Contributions are welcome! Please read **[CONTRIBUTING.md](CONTRIBUTING.md)** for the workflow,
coding standards, and the **Definition of Done**, and **[AGENTS.md](AGENTS.md)** for the detailed
conventions (also used to brief AI coding assistants). By participating you agree to the
[Code of Conduct](CODE_OF_CONDUCT.md).

In short:

1. Fork and branch (`feat/…`, `fix/…`).
2. Follow the architecture and naming conventions.
3. Keep `spotlessCheck`, `detekt`, and tests green.
4. Use [Conventional Commits](https://www.conventionalcommits.org/).
5. Open a PR using the template.

## Author

**Jayvardhan Potabatti** — Creator, Owner, Designer & Lead Developer.

See [AUTHORS.md](AUTHORS.md) for the full list, and [CONTRIBUTING.md](CONTRIBUTING.md) to get involved.

## License

Released under the [MIT License](LICENSE) © 2026 Jayvardhan Potabatti.
