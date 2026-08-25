# Architecture

Vedic Mitra is a **modular, feature-first Clean Architecture** Android application. This document
explains the layers, module boundaries, and the rules that keep the codebase maintainable as it
grows.

## Goals

- **Separation of concerns** — UI, presentation, domain, and data are distinct.
- **Testability** — logic depends on abstractions that are easy to fake.
- **Independent features** — features evolve without entangling each other.
- **Swappable capabilities** — astronomy, scheduling, notifications, and location sit behind ports.

## Layers

```
┌──────────────────────────────────────────────────────────────┐
│ Presentation                                                   │
│   Compose UI  →  ViewModel (MVVM, StateFlow<UiState>)          │
├──────────────────────────────────────────────────────────────┤
│ Domain                                                         │
│   UseCases (framework-free)  +  domain models                  │
├──────────────────────────────────────────────────────────────┤
│ Data                                                           │
│   Repository implementations  →  data sources / core ports     │
└──────────────────────────────────────────────────────────────┘
```

**Dependency rule:** source code dependencies point **inward**. Presentation depends on domain
abstractions; data implements them. Nothing inner knows about anything outer.

## Modules

### `:app`
Assembles the graph. Hosts the Hilt application root, the single `MainActivity`, and (in later
phases) the navigation host. Contains no business logic.

### `:core`
Shared, cross-feature building blocks.

- **`:core:common`** — framework-agnostic: `AppResult`, `DispatcherProvider`, `GeoCoordinates`. No
  UI, no other internal deps.
- **`:core:ui`** — reusable composables and preview tooling (`@ThemePreviews`).
- **`:core:designsystem`** — the Material 3 theme (`VedicMitraTheme`) and tokens.
- **`:core:datastore`** — persisted preferences and birth profiles on Jetpack DataStore.
- **`:core:domain`** — use cases needed by more than one feature, so neither has to own them.
- **`:core:astronomy` / `:core:scheduler` / `:core:alarm` / `:core:notifications` /
  `:core:location`** — **ports**: an interface defining a capability, with its implementation
  Hilt-bound in the module's own `di/`. A feature depends on the interface, never the
  implementation, which is what lets a test swap in a fake without a framework.

### `:feature`
User-facing screens. Each owns its UI + ViewModel (+ later, domain/data). A feature may depend on
`:core:*` only — **never** on another feature. Shared cross-feature behaviour is promoted into
`:core`.

## Module dependency rules

| From | May depend on | Must not depend on |
| --- | --- | --- |
| `:app` | any `:core`, any `:feature` | — |
| `:feature:*` | `:core:*` | other `:feature:*` |
| `:core:ui` | `:core:designsystem`, `:core:common` | any `:feature`, capability ports |
| `:core:designsystem` | `:core:common` (rare) | any `:feature` |
| `:core:astronomy/scheduler/alarm/notifications/location/datastore` | `:core:common` | any `:feature`, other capability ports |
| `:core:common` | — (external libs only) | everything internal |

These rules are enforced socially in review today; a Gradle module-graph assertion may be added
later.

## Build architecture

Per-module build config is **not** duplicated. Convention plugins in
[`build-logic/`](../build-logic) encapsulate it:

| Plugin id | Applies |
| --- | --- |
| `vedicmitra.android.application` | Android app + Kotlin + SDK levels + Detekt + test deps |
| `vedicmitra.android.library` | Android library + Kotlin + SDK levels + Detekt + test deps |
| `vedicmitra.android.compose` | Compose compiler + BOM + Compose bundle |
| `vedicmitra.android.hilt` | Hilt + KSP + Hilt deps |
| `vedicmitra.android.feature` | library + compose + hilt + core modules + lifecycle/nav |
| `vedicmitra.kotlin.library` | pure Kotlin/JVM library |

All versions are centralized in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml).

## Data flow (target design)

```
User action
   → Composable emits event lambda
      → ViewModel handles event, calls UseCase
         → UseCase invokes Repository
            → Repository reads/writes data source or core port
         ← returns AppResult<Domain>
      ← ViewModel maps to UiState
   ← Composable recomposes from StateFlow<UiState>
```

## Further reading

- [module-guide.md](module-guide.md) — how to add a new module.
- [getting-started.md](getting-started.md) — local setup.
- [adr/](adr) — architecture decision records.
- [../AGENTS.md](../AGENTS.md) — detailed conventions.
