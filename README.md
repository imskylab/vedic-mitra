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

> **Status:** The roadmap below is an expanded, 12-phase vision for the project. **Phase 1
> (Foundation) is done**, and **Phase 2 (Daily Timings)** and **Phase 3 (Panchang)** are partially
> built: the app already computes today's Panchang (tithi, nakshatra, yoga, karana, paksha, vara),
> Brahma/Abhijit Muhurta and the inauspicious kalams (Rahu, Yamaganda, Gulika), sunrise/sunset,
> Moon phase, and golden-hour windows for the device's location, persists theme preferences, and
> schedules reboot-survivable, per-event-configurable notification reminders for muhurta windows.
> Moonrise/moonset, Dur Muhurta/Varjyam, and all of Phases 4–12 remain aspirational — see the
> [Roadmap](#roadmap) for the full picture and current progress.

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

A longer-range, 12-phase roadmap that balances a solid MVP with progressively richer features.
Status marks reflect what's actually implemented today, verified against the code — not aspiration.

### ✅ Phase 1 — Foundation

**Engineering**

- [x] Clean Architecture
- [x] Modular project structure
- [x] Jetpack Compose
- [x] Material 3
- [x] Hilt
- [ ] Room
- [ ] WorkManager
- [x] Offline-first architecture
- [x] GitHub Actions CI/CD
- [x] Unit testing framework
- [x] Documentation
- [x] AI development guidelines (AGENTS.md)

> **Note:** the shipped app doesn't use Room or WorkManager. Persistence is on Jetpack DataStore
> (`:core:datastore`), and scheduling is on `AlarmManager` — exact alarms plus a reboot-survivable
> `BootReceiver` (`:core:scheduler`, `:feature:alarm`) — not deferred/constrained background work.
> Revisit this pairing only if a future phase genuinely needs a relational store or WorkManager's
> constraint-based scheduling.

### 🟡 Phase 2 — Daily Timings (MVP)

**Astronomical Calculations**

- [x] Sunrise
- [x] Sunset
- [ ] Moonrise
- [ ] Moonset
- [x] Moon phase
- [x] Golden Hour
- [x] Brahma Muhurta
- [x] Abhijit Muhurta
- [x] Rahu Kalam
- [x] Yamagandam
- [x] Gulika Kalam
- [ ] Dur Muhurta
- [ ] Varjyam

**Smart Alarms**

- [x] Brahma Muhurta alarm (as one of the scheduled muhurta reminders)
- [ ] Sunrise reminder
- [ ] Sunset reminder
- [x] Custom reminder offsets (configurable lead time)
- [x] Exact alarms
- [x] Daily automatic rescheduling (reboot-survivable via `BootReceiver`)

**Home Dashboard**

- [x] Current time
- [x] Today's Panchang summary
- [ ] Upcoming events
- [x] Current moon phase
- [ ] Next alarm

### 🟡 Phase 3 — Panchang

**Hindu Calendar**

- [ ] Daily Panchang screen with calendar navigation
- [ ] Monthly calendar
- [ ] Grid calendar
- [ ] List calendar
- [ ] Yearly overview

**Panchang Details**

- [x] Tithi
- [x] Nakshatra
- [x] Yoga
- [x] Karana
- [x] Paksha
- [ ] Maas
- [ ] Ritu
- [ ] Samvatsara
- [ ] Ayana

**Celestial Information**

- [x] Sunrise
- [x] Sunset
- [ ] Moonrise
- [ ] Moonset
- [x] Moon phase
- [ ] Planetary positions
- [ ] Zodiac transitions

### ⬜ Phase 4 — Festivals & Vrats

**Festival Calendar**

- [ ] Major Hindu festivals
- [ ] Regional festivals
- [ ] Sankranti
- [ ] Ekadashi
- [ ] Purnima
- [ ] Amavasya
- [ ] Chaturthi
- [ ] Pradosham
- [ ] Shivaratri
- [ ] Navaratri
- [ ] Diwali
- [ ] Holi
- [ ] Janmashtami
- [ ] Rama Navami
- [ ] Guru Purnima

**Vrat Support**

- [ ] Fasting days
- [ ] Parana timings
- [ ] Festival descriptions
- [ ] Ritual guidance
- [ ] Important observances

**Notifications**

- [ ] Festival reminders
- [ ] Vrat reminders
- [ ] Panchang alerts
- [ ] Upcoming observances

### ⬜ Phase 5 — Personalization

**User Profile**

- [ ] Name
- [ ] Date of Birth
- [ ] Time of Birth
- [ ] Place of Birth

**Saved Information**

- [ ] Personal tithis
- [ ] Family birthdays
- [ ] Spiritual milestones
- [ ] Favorite festivals
- [ ] Frequently observed vrats

**Custom Tracking**

- [ ] Daily sadhana
- [ ] Meditation streak
- [ ] Japa counter
- [ ] Reading tracker
- [ ] Temple visits

### ⬜ Phase 6 — Astrology

**Horoscope**

- [ ] Daily Rashifal
- [ ] Weekly Rashifal
- [ ] Monthly Rashifal
- [ ] Yearly Rashifal

**Kundli**

- [ ] Birth chart
- [ ] Planetary positions
- [ ] Lagna
- [ ] Navamsa
- [ ] Dasha overview

**Match Making**

- [ ] Kundli matching
- [ ] Compatibility score
- [ ] Guna Milan

**Reports**

- [ ] Birth report
- [ ] Planetary transit report
- [ ] Personalized recommendations

### 🟡 Phase 7 — Location & Astronomy

**Location Support**

- [x] GPS location
- [ ] City selection
- [ ] Custom latitude/longitude
- [ ] Multiple saved locations

**Offline Engine**

- [x] Offline astronomical calculations (on-device Meeus ephemeris, no network)
- [ ] Automatic timezone detection
- [ ] DST support
- [ ] Regional Panchang support

### 🟡 Phase 8 — Reminders & Automation

**Daily Notifications**

- [x] Sunrise *(covered as a muhurta reminder, not a dedicated toggle)*
- [x] Sunset *(covered as a muhurta reminder, not a dedicated toggle)*
- [x] Brahma Muhurta
- [ ] Sandhyavandanam
- [ ] Festival reminders
- [ ] Vrat reminders
- [ ] Meditation reminders

**Smart Scheduling**

- [x] Dynamic daily alarms
- [ ] Snooze options
- [ ] Repeat schedules
- [ ] Wear OS notifications (future)

### 🟡 Phase 9 — UI & User Experience

**Calendar Views**

- [ ] Monthly grid
- [ ] Agenda view
- [ ] Timeline view
- [ ] Festival highlights
- [ ] Color-coded observances

**Dashboard**

- [x] Clean, modern UI
- [x] Material You support
- [x] Dark mode
- [x] Dynamic colors
- [ ] Home screen widgets

**Accessibility**

- [ ] Large text mode
- [ ] High contrast
- [ ] Screen reader support
- [ ] Multiple themes

### ⬜ Phase 10 — Languages

**Localization**

- [ ] English
- [ ] Hindi
- [ ] Sanskrit
- [ ] Telugu
- [ ] Tamil
- [ ] Kannada
- [ ] Malayalam
- [ ] Marathi
- [ ] Gujarati
- [ ] Bengali
- [ ] Odia

Future community contributions can expand this list.

### ⬜ Phase 11 — Knowledge & Devotion

**Learning**

- [ ] Daily shloka
- [ ] Daily quote
- [ ] Festival significance
- [ ] Panchang explanations
- [ ] Beginner guides

**Devotional Tools**

- [ ] Stotra library
- [ ] Chant counter
- [ ] Meditation timer
- [ ] Audio support
- [ ] Offline content

### ⬜ Phase 12 — Ecosystem

**Integrations**

- [ ] Wear OS
- [ ] Android Auto (notifications)
- [ ] Calendar export (.ics)
- [ ] Backup & restore
- [ ] Cloud sync (optional and privacy-focused)

**Open Platform**

- [ ] Plugin architecture
- [ ] Public calculation library
- [ ] REST API (future)
- [ ] Desktop companion (future)

### Long-Term Vision (V2+)

Transform Vedic Mitra into a complete Digital Vedic Companion by combining:

- 🕉 Accurate Panchang
- 🌞 Offline astronomical calculations
- 📅 Hindu calendar
- 🙏 Festival and vrat guidance
- 🔔 Intelligent reminders
- 🌙 Celestial information
- ⭐ Astrology tools
- 📖 Spiritual learning
- 👤 Personalized experience
- 🌍 Multi-language support
- 🔒 Privacy-first, offline-first design
- 🌐 Open-source community contributions

### Suggested feature tiers

To keep the project focused as it grows, features are classified into three tiers:

- **Core (Offline):** Panchang, astronomy, reminders, calendar, location, personalization. These
  should work entirely offline after installation.
- **Enhanced (Optional Online):** Horoscope, planetary transit reports, and rich content updates.
  These can use online services but should degrade gracefully if offline.
- **Community Extensions:** Stotra packs, regional festival data, additional languages, and
  plugins. This keeps the core app lightweight while allowing the ecosystem to grow.

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
