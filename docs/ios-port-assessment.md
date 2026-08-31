# iOS: what it would actually take

> An assessment, not a commitment. Written 2026-08-31 against `main` at 17 commits past `v0.8.0`.

## The question

*Is an iPhone build a simple compile or a rewrite?*

Neither, and the reason is worth stating precisely: **the app splits cleanly into a part that is
already portable and a part that cannot be ported at all.** The line between them is not where you
would guess, and one feature does not survive the crossing on any technical route.

## What the code actually is

24,898 lines of Kotlin in the main source sets:

| | Lines | Share | Portability |
|---|---:|---:|---|
| Engine and domain | 8,051 | 32% | **Already portable** |
| UI | 13,911 | 56% | Compose Multiplatform, or rewritten in SwiftUI |
| Platform services | 2,936 | 12% | Needs an iOS implementation, or has none |

### The engine is genuinely ready

`core:astronomy` is the largest module in the repo at 7,580 lines, and:

- it imports **nothing** from `android.*` or `androidx.*`
- it uses `java.time` in **zero of its 55 files** — all 22 date-touching files already use
  `kotlin.time`, which is Kotlin/Native-ready
- its only non-portable imports are Dagger and `javax.inject` annotations, in **6 files of 55**
- it is built as `com.android.library` **only because that is the only library convention plugin the
  build offers** — a `vedicmitra.kotlin.library` plugin already exists and is applied by zero
  modules, which is the natural hook for a multiplatform sibling

That is not luck; it is the Clean Architecture boundary doing its job. It also means the part that
**must not** be rewritten does not have to be. The engine carries 280 test methods across 6,806
lines, many of them goldens validated against an independent implementation — the whole-sign Kala
Sarpa rule, the 64 ashtakavarga tables, the sidereal dasha year. Re-deriving that in Swift would
throw away every finding that came from testing rather than assuming, and would reintroduce the exact
bugs that testing caught.

`java.time` is concentrated where you would expect: **47 imports across 25 files**, in 12 of the 13
feature modules plus `core:datastore`, `core:location` and one file in `core:domain`. Almost all of
it is two mechanical patterns — about 20 `DateTimeFormatter.ofPattern(...)` declarations and about 20
`ZoneId.systemDefault()` call sites bridging a `kotlin.time.Instant` to wall-clock local time.

The codebase already treats `kotlin.time` as canonical and `java.time` as a display adapter; some
files import `kotlin.time.Instant` at the top and fully-qualify `java.time.Instant` only at the
formatting boundary. **`kotlinx-datetime` is not currently a declared dependency, and adding it is
the single highest-leverage prerequisite** — it mechanically retires roughly 40 of the 47 usages, and
it is worth doing on Android alone.

## The three blockers, in increasing order of seriousness

### 1. Hilt — mechanical, and smaller than it looks

Android-only, and applied by **21 of 24 modules**. But the surface that actually has to be rewritten
is small: **6 `@Module` objects, all in a `di/` package, all `@InstallIn(SingletonComponent)`, about
24 bindings in total**. The rest is 36 `@Inject` constructors and 16 `@HiltViewModel` classes, which
are annotations on otherwise ordinary code.

The shape of the work is specific: **every one of the 5 `@Provides` methods takes
`@ApplicationContext Context`**, so each is a platform binding that needs an `expect`/`actual` or a
per-platform module. Koin or manual factories both work. Tedious, no design risk.

Two pieces of luck here. `core:alarm`, `core:designsystem` and `core:ui` use no Hilt at all. And KSP
is used for **exactly one thing** — the Hilt compiler. There is no Room and no other annotation
processing anywhere, so removing Hilt removes KSP with it.

### 2. Offline timezone resolution — a real gap

`core:location` resolves coordinates to an IANA zone offline using `timezonemap`, which is a JVM
library backed by a native `zstd-jni` `.so`. Neither builds for iOS.

**And the data is enormous.** The APK ships `timezonemap-4.5-2020d.tar.zstd` at **25.6 MB — about
78% of the 33 MB release APK.** That reframes the problem: it is not only that the library has no
iOS target, it is that offline coordinate→timezone costs 25 MB of boundary polygons however it is
done. Whatever replaces it on iOS inherits that bill, and it is worth asking whether the feature is
worth a quarter of the download on either platform.

`kotlinx-datetime` carries timezone *rules* (and `kotlinx-datetime-zoneinfo` bundles an up-to-date
TZ database) but not geographic lookup — that is a different problem and has no standard
multiplatform answer. The options are to reimplement the lookup over the same ODbL
timezone-boundary-builder data, or to use CoreLocation reverse geocoding on iOS, which needs the
network and so breaks the offline-first promise that is one of this app's actual selling points.

### 3. The ringing alarm cannot exist on iOS

This is a **product** constraint, not an engineering one, and no amount of effort moves it.

[ADR 0009](adr/0009-ringing-alarm-reminders.md) deliberately chose to build a real alarm with the
full-screen-intent pattern, precisely because a notification "doesn't ring continuously and, by
default, doesn't pierce Do Not Disturb". On iOS the only mechanism that pierces Focus modes is the
Critical Alerts entitlement, which is manually reviewed by Apple, reserved for "urgent information
about personal health and public safety", and which the guidance says outright is **not suitable for
alarm apps**.

So on iOS, muhurta reminders are notifications. Brahma Muhurta will not wake anybody. **Feature
parity with Android is off the table before a single line is written**, which is worth knowing before
choosing a route, because it removes the main argument for sharing the UI.

## The routes

### A — Shared engine, native SwiftUI

Extract `core:astronomy`, `core:common` and `core:domain` into a Kotlin Multiplatform module. The
Android app keeps consuming it exactly as now. iOS gets a SwiftUI app on top of the same engine.

- Shares the 8,051 lines that matter, and the maths has one implementation forever
- The Android app is **not touched**, so nothing shipped can regress
- Native VoiceOver, native navigation, an iOS app that feels like one
- Costs a second UI, written from scratch and maintained in parallel

### B — Compose Multiplatform

Move the UI into `commonMain` too and ship one codebase.

- Shares far more, perhaps 70–85%
- But it requires migrating 13,911 lines of **working, shipped** UI into multiplatform source sets,
  swapping Hilt for Koin, replacing `java.time` across 12 feature modules, and replacing
  `navigation-compose` and DataStore — all against a codebase with **no UI test infrastructure at
  all** to catch the regressions
- Compose for iOS has been stable since 1.8.0 and is used in production by Netflix and Cash App, but
  iOS **accessibility is the acknowledged production gap as of early 2026** — and this app has real
  accessibility commitments, including per-row spoken descriptions added deliberately
- Material 3 on iOS is Skia-drawn and does not follow Apple's Human Interface Guidelines, so it looks
  like an Android app on an iPhone

### C — Full Swift rewrite

Rejected. It throws away 280 validated tests and guarantees the two platforms' astronomy will drift.

## Recommendation

**Route A, scoped deliberately smaller than the Android app.**

Two things point the same way. The absence of UI tests makes migrating shipped UI (route B) the
riskiest possible move — there is nothing to catch what breaks. And since the alarm cannot exist on
iOS anyway, feature parity is already impossible, which removes the strongest reason to share a UI.

So: ship an iOS app that does what the engine is good at, and does it natively.

**In scope for a first iOS release**

- Today's panchanga
- The calendar with the day detail, including the cycle rows
- Kundali — chart, grahas, dashas, yogas, doshas
- Muhurta windows as information
- Reminders as **notifications only**, with the limitation stated in the UI rather than hidden

**Out of scope**

- Ringing alarms — impossible
- Anything needing offline coordinate→timezone until blocker 2 is solved; the first release can ship
  with manual location selection from the existing saved-cities list, which sidesteps it entirely

## Phasing

1. **Extract the shared module.** Convert `core:astronomy` to a KMP module with `jvm`/`android` and
   `ios` targets, no behaviour change. Replace its Dagger annotations with plain constructors or a
   Koin module — one `@Module`, one `@Binds`, one `@Inject`. The Android app must keep building and
   every existing test must keep passing; that is the entire acceptance criterion, and it is a strong
   one. Note one trap in the shared build: `bundles.coroutines` includes
   **`kotlinx-coroutines-android`**, which 20 modules pull in — the common source set needs
   `coroutines-core` alone.
2. **Prove it on Native.** The 280 tests are written against JUnit4 and Truth, which are JVM-only.
   Keep the full suite running on the JVM target exactly as today, and port a representative subset —
   the goldens, the angular bucketing, the boundary cases — to `kotlin.test` to run on the iOS target.
   The point is not to duplicate coverage but to prove Kotlin/Native produces the same numbers,
   particularly around `Double` arithmetic and the bisection solvers.
3. **A read-only iOS app.** Today's panchanga and the calendar, SwiftUI, no persistence beyond
   selected location. This is the slice that proves the whole approach with the least surface.
4. **Kundali**, which needs no new platform capability — it is the engine plus tables.
5. **Notifications**, with `UNUserNotificationCenter`, and honest copy about what iOS will and will
   not do.
6. **Offline timezone**, if and only if it earns its place. Manual location selection may be enough —
   and given the 25.6 MB data cost, this deserves a product decision before an engineering one.

Steps 1 and 2 are the ones that de-risk everything. They are also independently valuable: a KMP
`core:astronomy` with no Dagger annotations is a cleaner module than the one there today, whether or
not an iOS app is ever built.

## Build facts, for whoever does this

Gradle 9.7.1, AGP 9.3.2, Kotlin 2.4.10, KSP 2.3.11, JDK 21 toolchain, `compileSdk` 36, `minSdk` 26.
Configuration cache is on and the build uses typesafe project accessors.

Five of the six convention plugins hard-code AGP extensions (`ApplicationExtension`,
`LibraryExtension`, `CommonExtension`) and Android-only configuration names (`debugImplementation`,
`androidTestImplementation`, `ksp`), so they do not survive a multiplatform restructure unchanged.
`Detekt.kt`, `Tests.kt` and the unused `KotlinLibraryConventionPlugin` do.

One convenience: all 13 feature modules get their entire build config from a single
`vedicmitra.android.feature` alias, so changing how features are built is a one-plugin change rather
than thirteen.

Three dead entries turned up while surveying and are worth removing whether or not iOS happens:
`kotlinx-serialization-json` (in the catalog, referenced by no module and imported by no file),
`mockk-android`, and the `vedicmitra.kotlin.library` plugin — though that last one should be kept if
this port proceeds, since it is exactly the hook a shared module needs.

## Two things found while surveying, unrelated to iOS

**A shipped bug.** `ReminderRescheduler.rescheduleEnabled` rebuilds every reminder without its
`alert` style or the alarm channel, so it defaults to `NOTIFICATION`. `BootReceiver` fires on both
`ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED`, which means **every reboot and every app
update silently downgrades alarm-style reminders to plain notifications** — a Brahma Muhurta alarm
set to ring stops ringing after a restart, with nothing to indicate it. `AlarmViewModel` reads
`alertTypeByName` correctly at all five of its scheduling sites; the rescheduler is the one path that
does not. The data needed to fix it is already persisted.

**Two more native libraries.** Besides `zstd-jni`, the APK carries
`libdatastore_shared_counter.so` and `libandroidx.graphics.path.so`, both transitive from androidx
rather than declared. Neither is a porting obstacle, but the assumption that `zstd-jni` is the only
native dependency was wrong.

## What this assessment does not cover

- Effort estimates in time. The line counts are real; how long 14,000 lines of SwiftUI takes depends
  entirely on who writes it.
- App Store, signing, and CI for a second platform. The current CI does `spotlessCheck`, `detekt`,
  `testDebugUnitTest` and `assembleDebug` on `ubuntu-latest`; iOS builds need macOS runners, which is
  a cost and a config change.
- Whether an iOS release is commercially worth it, which is not a question the code can answer.

## Sources

- [Compose Multiplatform 1.8.0 — iOS stable](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)
- [Is Compose Multiplatform production-ready in 2026](https://medium.com/@thanhnh98/is-compose-multiplatform-production-ready-in-2026-a-practical-field-guide-b4863748dbe7)
- [Apple — Critical Alerts entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.usernotifications.critical-alerts)
- [Using Critical Alerts on iOS](https://blog.kulman.sk/using-critical-alerts-on-ios/)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)
