<p align="center">
  <img src="art/logo/vedic-mitra-logo.png" alt="Vedic Mitra logo" width="220" />
</p>

# Vedic Mitra

> A modern Android companion for Vedic timekeeping — panchanga, muhurta, and astronomy-aware
> reminders — built with Kotlin, Jetpack Compose, and Clean Architecture.

[![CI](https://github.com/vedicmitra/vedic-mitra/actions/workflows/ci.yml/badge.svg)](https://github.com/vedicmitra/vedic-mitra/actions/workflows/ci.yml)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Commercial license](https://img.shields.io/badge/Commercial-available-brightgreen.svg)](LICENSING.md)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

> **Status: Phases 1–5 complete.** The app computes today's panchanga (tithi, nakshatra, yoga,
> karana, vara) and muhurta windows for the device's location, persists theme preferences, and
> schedules on-device notification reminders for muhurta windows. Next up is **Phase 6 — polish &
> release** (home widgets, localisation, Play Store pipeline) — see the [Roadmap](#roadmap).

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
| `:core:astronomy` | Panchanga/astronomy engine: Meeus ephemeris, Lahiri ayanamsa, muhurta windows. |
| `:core:scheduler` | `AlarmManager`-backed exact scheduling of reminder notifications. |
| `:core:notifications` | `NotificationManagerCompat`-backed channels and notification posting. |
| `:core:location` | Device location via Play Services fused provider. |
| `:core:datastore` | Persisted preferences (theme, enabled reminders) on Jetpack DataStore. |
| `:feature:home` | Home screen: today's panchanga for the device location. |
| `:feature:settings` | Settings screen: theme preferences. |
| `:feature:alarm` | Reminders screen: schedule notifications for the day's muhurta windows. |

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

- ✅ **Phase 1 — Foundation.** Build system, modules, DI/theme scaffolding, quality tooling, CI,
  docs.
- ✅ **Phase 2 — Astronomy engine.** `:core:astronomy` (Meeus ephemeris, Lahiri ayanamsa, panchanga
  elements, muhurta windows).
- ✅ **Phase 3 — Location & settings.** `:core:location` (fused provider) and `:feature:settings`
  (theme, on DataStore).
- ✅ **Phase 4 — Home.** Real panchanga overview for the device location in `:feature:home`.
- ✅ **Phase 5 — Scheduling & alarms.** `:core:notifications` + `:core:scheduler` (exact alarms) and
  the `:feature:alarm` reminders experience.
- ⬜ **Phase 6 — Polish & release.** Home widgets, localisation, Play Store release pipeline.

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

Vedic Mitra is **dual-licensed** — see **[LICENSING.md](LICENSING.md)**:

- **[GNU AGPL-3.0-or-later](LICENSE)** for open-source use (note: the AGPL requires
  network/SaaS deployments to offer their source to users).
- A **commercial license** for proprietary/closed-source use, without the AGPL's
  copyleft obligations.

Third-party components are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

© 2026 Jayvardhan Potabatti.
