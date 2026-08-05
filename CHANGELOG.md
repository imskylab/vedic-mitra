# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Moonrise and moonset.** `LunarDay` computes the Moon's topocentric altitude (extending
  `Ephemeris` with the Moon's ecliptic latitude, distance, equatorial conversion, and Greenwich
  Mean Sidereal Time — none of which existed before) and finds rise/set crossings via a coarse
  scan plus bisection, since the Moon moves too fast and irregularly for the Sun's closed-form
  hour-angle formula. Cross-checked against drikpanchang.com for Delhi across 7 consecutive days;
  every reference matched within ~5 minutes. Because the lunar day (~24h50m) is longer than the
  civil day, a civil day can have two moonrises/moonsets or none of one kind roughly once a month —
  `MoonTimes` follows the same nullable convention as `SunTimes` and reports the first occurrence
  within the civil day.
- **Dur Muhurta and Varjyam.** `MuhurtaCalculator` now adds Dur Muhurta (one or two of the day's 15
  equal parts, by a verified per-weekday table cross-checked against drikpanchang.com) and Varjyam
  (Nakshatra Thyajyam — a 4-ghati inauspicious window positioned within the current nakshatra by a
  verified 27-nakshatra ghati table, requiring a new backward search — `VarjyamCalculator` — for the
  exact instant the current nakshatra began, since ghatis are counted from nakshatra-start, not
  sunrise or midnight). Fixed a related correctness bug this surfaced: Abhijit Muhurta is now
  correctly suppressed on Wednesdays, when it would otherwise coincide with that day's Dur Muhurta.
- **Ayana and Ritu.** `:core:astronomy` now derives Ayana (Uttarayana/Dakshinayana) and Ritu (the
  six Indian seasons) from the Sun's sidereal longitude — the Drik (observed-position) convention.
  Both, along with the existing tithi/nakshatra/yoga/karana/vara/sunrise/Rahu Kalam/Brahma Muhurta
  calculations, were cross-checked against [datepanchang.com](https://datepanchang.com) and
  [drikpanchang.com](https://www.drikpanchang.com) for Mumbai/Delhi, 5 August 2026, and matched
  within a minute or so throughout.
- **Moon phase and golden hour.** `:core:astronomy` now derives the Moon's phase (one of the eight
  traditional divisions, from its elongation) and computes the day's golden-hour windows (Sun
  elevation between -4° and +6°, bracketing sunrise and sunset) via the same NOAA solar-position
  equations already used for sunrise/sunset. Both are shown on the home screen.
- **Muhurta reminders.** Reboot-survivable, exact-alarm notification reminders for the day's
  muhurta windows (Brahma Muhurta, Abhijit Muhurta, Rahu Kalam, Yamaganda, Gulika Kalam), each with
  its own independently configurable lead time (0–30 minutes before the window starts). Changing a
  window's lead time immediately re-schedules its alarm if already enabled, and the notification
  text reflects how far ahead it fires (e.g. "Brahma Muhurta starts in 30 minutes"). See
  [docs/adr/0002-per-event-muhurta-reminder-offsets.md](docs/adr/0002-per-event-muhurta-reminder-offsets.md).

### Changed
- **License changed from MIT to a dual license** — GNU AGPL-3.0-or-later for open-source use, plus a
  commercial license for proprietary use. See [LICENSING.md](LICENSING.md) and
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
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
