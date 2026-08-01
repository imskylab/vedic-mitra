# AGENTS.md — AI Coding Instructions for Vedic Mitra

This file briefs AI coding assistants (and new human contributors) on how to work in this
repository. Follow it precisely; it encodes the architecture and conventions the project is built
on. When a request conflicts with these rules, surface the conflict rather than silently breaking
them.

> **Project state:** Phase 1 foundation. Do **not** implement business logic, astronomy
> calculations, scheduling, or alarm behaviour unless the task explicitly asks for that phase. Core
> capability modules are **contracts (interfaces) only** right now.

---

## 1. Architecture

Vedic Mitra is a **modular, feature-first Clean Architecture** Android app using **MVVM**, the
**Repository pattern**, and **Hilt** DI.

**Layering (dependencies point inward):**

```
Compose UI  →  ViewModel  →  UseCase (domain)  →  Repository  →  Data source / core Port
```

Rules:

- **UI never talks to data directly.** UI → ViewModel → domain/repository.
- **Depend on abstractions.** Features depend on `:core` **ports** (interfaces), not
  implementations.
- **No feature-to-feature dependencies.** A `:feature:*` module may depend only on `:core:*`.
- **`:core:common` depends on nothing internal** and pulls in no UI.
- **Ports live in `:core`**, implementations are provided (and Hilt-bound) in the phase that builds
  them.
- Keep the **domain layer framework-free** (no Android imports) wherever practical.

## 2. Folder & module structure

```
:app                     application shell — Hilt root, MainActivity, navigation host
:core:common             AppResult, dispatcher abstractions, shared value types (no UI)
:core:ui                 reusable composables + preview tooling
:core:designsystem       Material 3 theme (colour/type/shape/spacing)
:core:astronomy          PORT: AstronomyEngine (contracts only)
:core:scheduler          PORT: TaskScheduler (contracts only)
:core:notifications      PORT: Notifier (contracts only)
:core:location           PORT: LocationProvider (contracts only)
:feature:home            home screen (UI + ViewModel)
:feature:settings        settings screen (UI + ViewModel)
:feature:alarm           alarm screen (UI + ViewModel)
build-logic/convention   Gradle convention plugins (vedicmitra.*)
config/                  detekt + spotless configuration
docs/                    architecture, module guide, ADRs
```

Within a feature module, organise by responsibility:

```
feature/<name>/src/main/kotlin/io/github/vedicmitra/feature/<name>/
    <Name>Screen.kt        // Compose UI (stateful + stateless composables)
    <Name>ViewModel.kt     // MVVM presentation, @HiltViewModel
    <Name>UiState.kt       // (when it grows) immutable UI state
    navigation/            // navigation entry points/routes
    domain/                // use cases (framework-free)
    data/                  // repository impls, DTOs, mappers
    di/                    // Hilt modules for this feature
```

## 3. Naming conventions

| Element | Convention | Example |
| --- | --- | --- |
| Package | lowercase, `io.github.vedicmitra.<layer>.<module>` | `io.github.vedicmitra.core.astronomy` |
| Class / interface / object | PascalCase | `AstronomyEngine`, `HomeViewModel` |
| Function | camelCase | `snapshotAt()` |
| `@Composable` function | PascalCase (UI-noun) | `HomeScreen`, `PanchangaCard` |
| Constant / top-level val | UPPER_SNAKE_CASE | `MIN_SDK` |
| Backing property | leading underscore | `_uiState` / `uiState` |
| Test method | backticked sentence | ``fun `map transforms success value`()`` |
| Port interface | capability noun | `Notifier`, `LocationProvider` |
| Hilt module | `<Feature>Module` / `<Capability>Module` | `AstronomyModule` |
| Gradle convention plugin id | `vedicmitra.<area>.<role>` | `vedicmitra.android.feature` |

- **No Hungarian notation, no abbreviations** unless industry-standard (`ui`, `io`, `db`).
- Source directory is `src/main/kotlin` (not `java`).

## 4. Coding conventions (Kotlin)

- Target **Kotlin 2.1 / JDK 21**; max line length **120**; 4-space indent; **no wildcard imports**.
- Prefer **immutability**: `val`, `data class`, read-only collections, `StateFlow` over mutable
  observable state exposed publicly.
- Model fallible operations with **`AppResult`** (`:core:common`) instead of throwing across layers.
- Use **coroutines/Flow** for async; inject **`DispatcherProvider`** rather than referencing
  `Dispatchers` directly.
- Keep functions small and single-purpose; respect the Detekt complexity thresholds.
- Every **public** class/function has **KDoc** explaining intent (not restating the signature).
- All versions come from `gradle/libs.versions.toml`. Never hard-code a dependency version in a
  module build script.

## 5. Compose conventions

- **State hoisting:** split a screen into a stateful entry composable (resolves `hiltViewModel()`,
  collects state) and a **stateless** `*Content` composable that takes state + lambdas as
  parameters. Preview the stateless one.
- Collect flows with **`collectAsStateWithLifecycle()`**.
- Every composable accepts a **`modifier: Modifier = Modifier`** as its first optional parameter and
  applies it to its root.
- Read colours/typography/shapes only from **`MaterialTheme`** / the design system — never hard-code
  hex colours or dp text sizes in feature UI.
- Wrap screens in **`VedicMitraTheme`** for previews; use the `@ThemePreviews` multipreview from
  `:core:ui` to check light + dark.
- Keep composables side-effect-free; use the proper effect APIs (`LaunchedEffect`, etc.) when
  needed. Hoist events upward as lambdas.

## 6. Dependency Injection (Hilt) conventions

- The application is the DI root (`@HiltAndroidApp` on `VedicMitraApplication`); `MainActivity` is
  `@AndroidEntryPoint`.
- ViewModels use **`@HiltViewModel`** with **`@Inject constructor(...)`**; obtain them in Compose via
  `hiltViewModel()`.
- **Bind interfaces to implementations** with `@Binds` in an `abstract class` module; use
  `@Provides` only for types you don't own.
- Choose the correct **component scope** — `SingletonComponent` for app-wide singletons,
  `ViewModelComponent` for per-ViewModel. Annotate with `@Singleton` etc. deliberately.
- Each capability/feature owns its Hilt module(s) under a `di/` package. Ports declared in `:core`
  get their bindings in the module that implements them.
- Prefer **constructor injection**; use qualifiers (`@Qualifier`) to disambiguate (e.g. dispatcher
  bindings).

## 7. Testing

- **Unit tests** live in `src/test/…`; instrumented tests in `src/androidTest/…`.
- Stack: **JUnit4**, **Truth** (assertions), **MockK** (mocking), **Turbine** (Flow),
  **kotlinx-coroutines-test**.
- Test **ViewModels, use cases, repositories, and mappers**. Do not test framework/generated code.
- Inject a **test dispatcher** via `DispatcherProvider`; never rely on real `Dispatchers` in tests.
- Name tests as behaviour sentences: ``fun `cancel removes the scheduled task`()``.
- Follow **Arrange → Act → Assert**; one logical assertion focus per test.
- New behaviour ships with tests. Bug fixes add a regression test.

## 8. Documentation

- Public APIs: **KDoc** with intent, params, return, and any threading/failure notes.
- Architectural decisions: add an **ADR** under `docs/adr/` (copy `0001`'s format).
- Keep **README**, **docs/**, and the **CHANGELOG** current when behaviour or structure changes.
- Update **this file** when a convention changes.

## 9. Commit message format

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <imperative summary ≤72 chars>

<body: what & why, wrapped ~100 cols>

<footer: BREAKING CHANGE: …, Closes #123>
```

- **types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`
- **scope:** module/area (`home`, `core-astronomy`, `build-logic`, …)
- One logical change per commit. Examples:
  - `feat(alarm): schedule exact alarms via TaskScheduler`
  - `fix(core-common): propagate cause in AppResult.map`
  - `build(deps): bump compose bom to 2025.01.00`

## 10. Definition of Done

A task is complete only when **all** hold:

- [ ] Change is in the **correct module and layer**; no illegal cross-module dependency.
- [ ] Follows naming, Kotlin, Compose, and DI conventions above.
- [ ] Public APIs have **KDoc**.
- [ ] **Tests** added/updated and passing.
- [ ] `./gradlew spotlessCheck detekt testDebugUnitTest assembleDebug` all succeed.
- [ ] No new dependency version hard-coded outside the catalog.
- [ ] UI changes verified in **light and dark**, using design-system tokens.
- [ ] Docs/CHANGELOG updated if behaviour or structure changed.
- [ ] Commits follow Conventional Commits; PR uses the template.

## 11. Guardrails for AI assistants

- **Do not** invent astronomy math, alarm delivery, or scheduling in Phase 1 — extend the **ports**
  only, and stop if implementation is required but not requested.
- **Do not** add libraries without adding them to the version catalog and justifying them.
- **Do not** bypass convention plugins by copying build config into modules.
- **Prefer editing** existing files and reusing `:core` utilities over creating parallel ones.
- If a requested change would violate a layer/module rule, **explain the conflict and propose a
  compliant alternative** instead of proceeding.
