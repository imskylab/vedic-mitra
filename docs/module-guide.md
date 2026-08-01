# Module Guide

How to add and structure modules in Vedic Mitra. Read [architecture.md](architecture.md) first for
the layering and dependency rules.

## Choosing a module type

| You are adding… | Module type | Convention plugin |
| --- | --- | --- |
| A user-facing screen | `:feature:<name>` | `vedicmitra.android.feature` |
| A shared UI widget/tooling | code in `:core:ui` | (already `library` + `compose`) |
| A design token / theme change | code in `:core:designsystem` | — |
| A new cross-cutting capability | `:core:<capability>` (port) | `vedicmitra.android.library` |
| Framework-free utility | `:core:common` or new `vedicmitra.kotlin.library` | — |

Prefer extending an existing module over creating a new one.

## Adding a new feature module

1. **Create the directory + build script** `feature/<name>/build.gradle.kts`:

   ```kotlin
   plugins {
       alias(libs.plugins.vedicmitra.android.feature)
   }

   android {
       namespace = "io.github.vedicmitra.feature.<name>"
   }

   dependencies {
       // only the extra core ports this feature needs
   }
   ```

2. **Add a bare manifest** `feature/<name>/src/main/AndroidManifest.xml`:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest />
   ```

3. **Register the module** in [`settings.gradle.kts`](../settings.gradle.kts):

   ```kotlin
   include(":feature:<name>")
   ```

4. **Add sources** under `src/main/kotlin/io/github/vedicmitra/feature/<name>/`:
   `<Name>Screen.kt` (stateful + stateless composables), `<Name>ViewModel.kt` (`@HiltViewModel`),
   and `navigation/`, `domain/`, `data/`, `di/` as the feature grows.

5. **Wire it into `:app`** by adding `implementation(projects.feature.<name>)` and hooking it into
   the navigation graph.

## Adding a new core capability (port)

1. Create `core/<capability>/build.gradle.kts` with `vedicmitra.android.library` and
   `api(projects.core.common)`.
2. Define the **interface** (the port) plus any value types — **no implementation** until the phase
   that builds it.
3. Register in `settings.gradle.kts`.
4. When implementing later, add the concrete class and a Hilt module (`@Binds`) under `di/`.

## Conventions recap

- Namespace = `io.github.vedicmitra.<layer>.<module>`; sources in `src/main/kotlin`.
- Depend on the **version catalog**; never hard-code versions.
- Features depend on `:core:*` only — never other features.
- Reuse convention plugins; don't copy build config.

See [../AGENTS.md](../AGENTS.md) for the full conventions.
