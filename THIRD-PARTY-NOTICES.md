# Third-Party Notices

Vedic Mitra (dual-licensed — see [LICENSING.md](LICENSING.md)) is built with
third-party open-source components. Each remains under its own license; their
inclusion here does not change those licenses. Notices are provided as required
by the respective licenses.

## Apache License 2.0

Licensed under the Apache License, Version 2.0
(<https://www.apache.org/licenses/LICENSE-2.0>):

- **AndroidX / Jetpack** (`androidx.*`, including Jetpack Compose, Lifecycle,
  Navigation, Activity, DataStore) — © The Android Open Source Project
- **Dagger & Hilt** (`com.google.dagger.*`) — © Google LLC
- **KSP** (`com.google.devtools.ksp`) — © Google LLC
- **Kotlin & kotlinx** (`org.jetbrains.kotlin*`, `org.jetbrains.kotlinx:*`) —
  © JetBrains s.r.o. and contributors
- **Google Truth** (`com.google.truth`) — © Google LLC (test only)
- **Turbine** (`app.cash.turbine`) — © Cash App / Block, Inc. (test only)
- **MockK** (`io.mockk`) — © MockK contributors (test only)
- Build tooling: **Detekt**, **ktlint**, **Spotless** — respective authors

## Google APIs Terms of Service

- **Google Play Services — Location** (`com.google.android.gms:play-services-location`)
  — © Google LLC, used under the Google APIs Terms of Service
  (<https://developers.google.com/terms>).

---

Full license texts are available from each project. This list covers the direct
dependencies; a complete, generated dependency-license report can be produced
from the build in a later pass.
