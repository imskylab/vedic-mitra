<p align="center">
  <img src="art/logo/vedic-mitra-logo.png" alt="Vedic Mitra logo" width="220" />
</p>

# Vedic Mitra

> A modern Android companion for Vedic timekeeping — panchanga, muhurta, and astronomy-aware
> reminders — built with Kotlin, Jetpack Compose, and Clean Architecture.

[![CI](https://github.com/imskylab/vedic-mitra/actions/workflows/ci.yml/badge.svg)](https://github.com/imskylab/vedic-mitra/actions/workflows/ci.yml)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Commercial license](https://img.shields.io/badge/Commercial-available-brightgreen.svg)](docs/COMMERCIAL_LICENSE.md)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

> **Status:** The roadmap below is an expanded, 12-phase vision for the project. **Phase 1
> (Foundation) is done**; **Phase 2 (Daily Timings)**, **Phase 3 (Panchang)**, **Phase 4 (Festivals
> & Vrats)**, and **Phase 7 (Location & Astronomy)** are partially built: the app already computes
> today's Panchang (tithi, nakshatra, yoga, karana, paksha, vara, ayana, ritu, maasa, samvatsara),
> Brahma/Abhijit Muhurta, Dur Muhurta, Varjyam, the inauspicious kalams (Rahu, Yamaganda, Gulika),
> the sixteen Choghadiya windows, sunrise/sunset, moonrise/moonset, Moon phase, golden-hour windows,
> and the graha rashi positions (Sun/Moon/Guru/Shukra) with their next pravesh — for any saved or
> GPS location, with offline timezone/DST detection. It derives upcoming festivals, lunar
> observances (Ekadashi, Purnima, Amavasya, Sankashti Chaturthi, Pradosh, …) and Sankrantis, and
> schedules reboot-survivable, per-event-configurable reminders for muhurta, Choghadiya, and tithi
> events. It shows all of this on a home dashboard and a browsable monthly **Panchang** calendar
> (tap any day for its full panchang; notable days are highlighted), wrapped in a golden/maroon
> brand theme drawn from the app emblem, navigated via a bottom bar. Calculations — including the
> sunrise-tithi convention by which the day is named — are cross-checked against published
> almanacs and an independent reference implementation before shipping. Phases 5–6 and 8–12 remain largely aspirational — see the [Roadmap](#roadmap) for
> the full picture and current progress.

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
          ┌─────────────────┼───────────────────┐
   ┌──────▼──────┐   ┌──────▼───────┐   ┌───────▼──────┐
   │ :feature:   │   │ :feature:    │   │ :feature:    │
   │   home      │   │  settings    │   │   alarm      │
   └──────┬──────┘   └──────┬───────┘   └──────┬───────┘
          │                 │                  │
          └───────── depend on core ports ─────┘
   ┌──────────────────────────────────────────────────────┐
   │ :core:common  :core:ui  :core:designsystem           │
   │ :core:astronomy  :core:scheduler                     │
   │ :core:notifications  :core:location                  │
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

> **Astrology arc — build order.** Profile (Phase 5) → the **chart-computation layer** in
> `:core:astronomy` (Lagna/houses, D9/D10, Vimshottari dasha, transits — Phase 6) → Kundali display →
> Rashifal → **Muhurta** (Phase 6). General (panchang) Muhurta needs none of this — it runs on today's
> engine and can ship independently as a quick win.

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
- [x] Moonrise
- [x] Moonset
- [x] Moon phase
- [x] Golden Hour
- [x] Brahma Muhurta
- [x] Abhijit Muhurta
- [x] Rahu Kalam
- [x] Yamagandam
- [x] Gulika Kalam
- [x] Dur Muhurta
- [x] Varjyam

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
- [x] Upcoming events
- [x] Current moon phase
- [ ] Next alarm

### 🟡 Phase 3 — Panchang

**Hindu Calendar**

- [x] Daily Panchang screen with calendar navigation
- [x] Monthly calendar
- [x] Grid calendar
- [ ] List calendar
- [ ] Yearly overview

**Panchang Details**

- [x] Tithi
- [x] Nakshatra
- [x] Yoga
- [x] Karana
- [x] Paksha
- [x] Maas
- [x] Ritu
- [x] Samvatsara
- [x] Ayana

**Celestial Information**

- [x] Sunrise
- [x] Sunset
- [x] Moonrise
- [x] Moonset
- [x] Moon phase
- [x] Planetary positions
- [x] Zodiac transitions

### 🟡 Phase 4 — Festivals & Vrats

**Festival Calendar**

- [x] Major Hindu festivals
- [ ] Regional festivals
- [x] Sankranti
- [x] Ekadashi
- [x] Purnima
- [x] Amavasya
- [x] Chaturthi
- [x] Pradosham
- [x] Shivaratri
- [x] Navaratri
- [x] Diwali
- [x] Holi
- [x] Janmashtami
- [x] Rama Navami
- [x] Guru Purnima

**Vrat Support**

- [ ] Fasting days
- [ ] Parana timings
- [ ] Festival descriptions
- [ ] Ritual guidance
- [x] Important observances

**Notifications**

- [ ] Festival reminders
- [x] Vrat reminders
- [ ] Panchang alerts
- [x] Upcoming observances

### 🟡 Phase 5 — Personalization

**User Profile** *(multiple profiles — yourself + family/friends, one primary "Self"; prerequisite
for the Phase 6 astrology features)*

- [x] Name
- [x] Date of Birth
- [x] Time of Birth *(exact — Lagna, houses and divisional charts collapse without it)*
- [x] Place of Birth *(free text for now; geocoding to coordinates + time zone is a later refinement)*

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

> Restructured to lead with the **chart-computation layer** — the shared foundation for Kundali,
> Rashifal and Muhurta. The engine already computes graha rashi positions (with pravesh), nakshatra,
> yoga and karana; this phase adds the natal-chart primitives it lacks.

**Chart-Computation Layer** *(build once, on `:core:astronomy`)*

- [ ] Lagna / ascendant + house cusps
- [ ] Whole-chart planetary rasi + degree
- [ ] Navamsa (D9)
- [ ] Dasamsa (D10) *(and further divisional charts as needed)*
- [ ] Vimshottari dasha state at an arbitrary date
- [ ] Transit snapshot at an arbitrary date
- [ ] Pure `natalChart(birth)` + `chartStateAt(birth, moment)` API — offline, deterministic,
  reference-checked

**Kundli** *(consumes the chart layer)*

- [ ] Birth chart
- [ ] Planetary positions
- [ ] Lagna
- [ ] Navamsa
- [ ] Dasha overview
- [ ] Birth report

**Horoscope (Rashifal)**

- [ ] Daily Rashifal *(start light: transit vs. birth Moon rashi)*
- [ ] Weekly Rashifal
- [ ] Monthly Rashifal
- [ ] Yearly Rashifal

**Muhurta (Electional)** *(picking auspicious times for events)*

- [ ] General / panchang muhurta — tithi · nakshatra · yoga · karana · Choghadiya/Hora · avoiding
  Rahu/Yamaganda/Gulika · Abhijit *(needs only today's engine — buildable now)*
- [ ] Personalized muhurta — Tarabala / Chandrabala relative to the user's birth Moon *(needs
  Profile + the chart layer's birth-Moon slice)*
- [ ] Event-type presets (marriage, housewarming, travel, purchase, …)

**Match Making**

- [ ] Kundli matching
- [ ] Compatibility score
- [ ] Guna Milan

**Reports**

- [ ] Planetary transit report
- [ ] Personalized recommendations

### 🟡 Phase 7 — Location & Astronomy

**Location Support**

- [x] GPS location
- [x] City selection
- [x] Custom latitude/longitude
- [x] Multiple saved locations

**Offline Engine**

- [x] Offline astronomical calculations (on-device Meeus ephemeris, no network)
- [x] Automatic timezone detection
- [x] DST support
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

- [x] Monthly grid
- [ ] Agenda view
- [ ] Timeline view
- [x] Festival highlights
- [x] Color-coded observances

**Dashboard**

- [x] Clean, modern UI
- [x] Material You support
- [x] Dark mode
- [x] Dynamic colors
- [ ] Home screen widgets

**Landing hub** *(new default Home — hero + categorised shortcut grid; see
[ADR 0013](docs/adr/0013-home-hub-landing-and-navigation.md))*

- [ ] Contextual "today" hero (panchang glance + auspicious-now strip) → opens the daily Panchang
- [ ] Category tabs: Daily · Astrology · Devotion
- [ ] Shortcut grid — tiles map to roadmap phases; unbuilt features show a "coming/unlock" state
- [ ] Bottom nav: Home · Panchang · Reminders · Explore · Profile *(Settings moves under Profile)*
- [ ] Panchang dashboard preserved as the Panchang destination (reachable from the hero + tab)

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

### Release builds (installable, updatable)

For a signed release that **updates** an already-installed copy in place, see
**[docs/RELEASING.md](docs/RELEASING.md)**. In short: create a release keystore once with `keytool`,
copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties` (both the
keystore and this file are gitignored), bump [`version.properties`](version.properties), then build
`:app:assembleRelease` (APK) or `:app:bundleRelease` (AAB for Play). The design is recorded in
[ADR 0010](docs/adr/0010-release-signing-and-versioning.md).

> The `v*` tag [release workflow](.github/workflows/release.yml) currently attaches a **debug-signed**
> APK for convenience. Debug-signed builds cannot update a keystore-signed install (different
> certificate), so use a locally signed release build for real distribution until CI is wired to sign
> with the release keystore.

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

## Support the project

Vedic Mitra is free and always will be — every feature, for everyone, with no ads, no tracking, and
no account. Nothing is paywalled, and nothing here unlocks anything. If it's useful to you, this is
how you can keep it going.

**Donate**

- **[GitHub Sponsors](https://github.com/sponsors/imskylab)** — one-off or monthly.
- **[Ko-fi](https://ko-fi.com/imskylab)** — a one-off tip by card or PayPal.
- **UPI** — `skylab@upi` (India).

**For businesses**

The AGPL requires anything built on Vedic Mitra — including hosted services — to share its source
under the same terms. If that doesn't work for your product, a **commercial license** lifts the
obligation: see **[pricing and tiers](docs/COMMERCIAL_LICENSE.md)**, or email `skylabs.in@gmail.com`.
The `:core:astronomy` engine is licensable on its own.

**Costs nothing**

- ⭐ Star the repo — it's the single biggest help for discovery.
- 🐛 [Report a bug](https://github.com/imskylab/vedic-mitra/issues/new?template=bug_report.md), or a
  panchanga timing that disagrees with your almanac.
- 🌐 Contribute a translation, or regional festival data — see [CONTRIBUTING.md](CONTRIBUTING.md).
- 💬 Tell someone who'd find it useful.

## Author

**Jayvardhan Potabatti** — Creator, Owner, Designer & Lead Developer.

- GitHub: [@imskylab](https://github.com/imskylab)
- LinkedIn: [linkedin.com/in/imskylab](https://www.linkedin.com/in/imskylab/)

See [AUTHORS.md](AUTHORS.md) for the full list, and [CONTRIBUTING.md](CONTRIBUTING.md) to get involved.

## License

Vedic Mitra is **dual-licensed** — see **[LICENSING.md](LICENSING.md)**:

- **[GNU AGPL-3.0-or-later](LICENSE)** for open-source use (note: the AGPL requires
  network/SaaS deployments to offer their source to users), with one
  [additional permission](LICENSE-EXCEPTIONS.md) under AGPL section 7 for linking against
  proprietary platform libraries such as Google Play services.
- A **[commercial license](docs/COMMERCIAL_LICENSE.md)** for proprietary/closed-source use, without
  the AGPL's copyleft obligations — see [LICENSE-COMMERCIAL.md](LICENSE-COMMERCIAL.md) for terms.

Vedic Mitra collects no data and has no servers; see the [Privacy Policy](docs/PRIVACY.md).

Third-party components are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

© 2026 Jayvardhan Potabatti.
