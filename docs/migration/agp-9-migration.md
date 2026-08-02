# Migration: AGP 9 / Gradle 9 / Kotlin 2.3

## Status — executed (toolchain done; androidx deferred)

The toolchain lift is **complete and CI-green**: Gradle 9.6.1, AGP 9.3.1, Kotlin 2.3.10 + KSP 2.3.10
(via AGP's **built-in Kotlin** — the standalone `kotlin-android` plugin is removed), Hilt 2.60.1,
compileSdk 36. Key changes that were required:

- AGP 9's `CommonExtension` is non-generic and no longer exposes `defaultConfig`/`compileOptions` —
  SDK config moved onto the concrete `ApplicationExtension`/`LibraryExtension`.
- The standalone Kotlin/Hilt Gradle plugins used the removed `BaseExtension`; adopting AGP built-in
  Kotlin + Hilt 2.60.1 fixed this.
- `kotlinx.datetime.Instant` → stdlib `kotlin.time.Instant`.

**Deferred:** the latest androidx/Compose line (core 1.19, lifecycle 2.11, hilt-navigation 1.4,
Compose BOM 2026.06) requires compiling against **compileSdk 37**, which currently exists only as a
preview (`android-CANARY`) — not a stable platform. Those bumps stay held until API 37 ships stable
(or opt into the preview via `compileSdkPreview`). The Dependabot `androidx.*` hold remains in place.

## Why

The current 2026 androidx, Compose, and Dagger-Hilt releases **require Android Gradle Plugin 9.1+**
(verified from CI: `hilt-android-gradle-plugin` refuses AGP < 9.0; `androidx.core:1.19.0`,
`lifecycle:2.11.0`, `hilt-navigation-compose:1.4.0` fail AAR-metadata checks demanding "AGP 9.1.0 or
higher"). The project is intentionally pinned to the AGP 8.x baseline, and Dependabot holds those
bumps (see `.github/dependabot.yml`). This migration lifts the whole toolchain together so modern
libraries can be adopted.

**Do this on a branch** (`chore/agp9-migration`), one stage per commit, letting CI go green before
the next stage. Nothing here changes app behavior — it's a toolchain lift.

## Baseline → target

| Component | Current | Target |
| --- | --- | --- |
| Gradle wrapper | 8.14 | 9.x (match AGP 9.1's minimum) |
| AGP | 8.13.2 | **9.1+** (≥ 9.1 is required by androidx; take latest 9.x) |
| Kotlin (+ Compose compiler plugin, same version) | 2.1.0 | 2.2.x |
| KSP | 2.1.0-1.0.29 | matching Kotlin, e.g. `2.2.x-x.x.x` |
| androidx-core | 1.15.0 | 1.19.0 |
| androidx-activity | 1.9.3 | 1.13.0 |
| androidx-lifecycle | 2.8.7 | 2.11.0 |
| compose BOM | 2024.12.01 | 2026.06.01 |
| hilt-navigation-compose | 1.2.0 | 1.4.0 |
| Dagger Hilt | 2.54 | 2.60.1+ |
| androidx-test ext / espresso | 1.2.1 / 3.6.1 | 1.3.0 / 3.7.0 |
| kotlinx-serialization | 1.7.3 | 1.11.0 |
| kotlinx-datetime | 0.6.1 | 0.8.0 **(breaking — see below)** |

All version edits are in one file: `gradle/libs.versions.toml`. That's the payoff of the catalog +
convention-plugin architecture — no per-module churn.

## Known breaking changes to handle

1. **kotlinx-datetime `Instant`.** In 0.7+, `kotlinx.datetime.Instant`/`Clock` moved to the stdlib
   (`kotlin.time.Instant`/`Clock`, stable since Kotlin 2.1.20). Two files use it:
   - `core/astronomy/src/main/kotlin/io/github/vedicmitra/core/astronomy/AstronomyEngine.kt`
   - `core/scheduler/src/main/kotlin/io/github/vedicmitra/core/scheduler/TaskScheduler.kt`

   Change `import kotlinx.datetime.Instant` → `import kotlin.time.Instant`. Keep the
   `kotlinx-datetime` dependency (still needed for `LocalDate`/`TimeZone` in the astronomy phase).

2. **Gradle 9 deprecations.** Surface them *before* bumping the wrapper:
   `./gradlew help --warning-mode all` and `./gradlew assembleDebug --warning-mode all`. Fix each
   flagged usage (common: `project.buildDir` → `layout.buildDirectory`). We already saw a
   "Deprecated Gradle features … incompatible with Gradle 9.0" warning, so budget for a few.

3. **AGP 9 DSL removals.** Do the manual equivalent of the AGP Upgrade Assistant using the
   [AGP 9 release notes](https://developer.android.com/build/releases/gradle-plugin). Review our
   convention plugins (`build-logic/convention/…`) for any removed `CommonExtension`/variant API.

4. **`android.suppressUnsupportedCompileSdk=36`** in `gradle.properties` — remove it once AGP 9
   officially lists compileSdk 36 as supported (it should).

5. **Detekt vs Kotlin 2.2.** detekt 1.23.8 embeds an older Kotlin for analysis; it runs without type
   resolution here so it should be fine, but if it errors, bump to the latest detekt 1.23.x or the
   detekt 2.x line and re-tune `config/detekt/detekt.yml`. ktlint 1.5.0 / Spotless 7.0.2 are fine
   with Kotlin 2.2 source.

6. **Config cache** becomes default in Gradle 9 (we already enable it). Confirm the convention
   plugins are config-cache compatible (no `Project` access at execution time).

## Staged plan (verify CI green after each)

1. **Deprecations first, still on 8.x** — run with `--warning-mode all`, fix Gradle-9-incompatible
   usages. Commit.
2. **Gradle 9** — `./gradlew wrapper --gradle-version <9.x>`; fix fallout. Commit.
3. **AGP 9.1+** — bump `agp` in the catalog; resolve DSL/API breakages in `build-logic`. Commit.
4. **Kotlin 2.2 + KSP + Compose compiler** — bump `kotlin` and `ksp` refs together. Commit.
5. **kotlinx-datetime 0.8 + Instant migration** — bump `kotlinxDatetime`, fix the two imports above,
   bump `kotlinxSerialization`. Commit.
6. **androidx + Hilt + test libs** — bump core/activity/lifecycle/compose-BOM/hilt-nav/hilt/test.
   Commit.
7. **Cleanup** — remove `suppressUnsupportedCompileSdk`; refresh version comments in the catalog;
   bump detekt if needed. Commit.
8. **Re-enable Dependabot** — remove the AGP-9-era `ignore` rules from `.github/dependabot.yml`
   (androidx/dagger minor+major, AGP/Gradle major, Kotlin major+minor, kotlinx-datetime major) so
   updates flow again now that the ecosystem is reachable. Commit.
9. **Docs** — update the tech-stack table in `README.md`, `AGENTS.md`, and `CHANGELOG.md`
   (`[Unreleased]`).

## Verify (each stage + final)

```bash
./gradlew spotlessCheck detekt testDebugUnitTest assembleDebug
```

CI runs the same gates on every push, so the branch's green check is the source of truth. If working
without a local Android SDK, push the branch and read CI.

## Rollback

Each stage is its own commit on a branch; revert to the last green commit if a stage misbehaves. The
migration is not merged to `main` until the full chain is green, so `main` stays releasable
throughout.
