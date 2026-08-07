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
- **Esri Geometry API** (`com.esri.geometry:esri-geometry-api`) — © Esri
  (transitive dependency of Time Zone Map)
- **Apache Commons Compress** (`org.apache.commons:commons-compress`) —
  © The Apache Software Foundation (transitive dependency of Time Zone Map)
- Build tooling: **Detekt**, **ktlint**, **Spotless** — respective authors

## Google APIs Terms of Service

- **Google Play Services — Location** (`com.google.android.gms:play-services-location`)
  — © Google LLC, used under the Google APIs Terms of Service
  (<https://developers.google.com/terms>).

## MIT License

Licensed under the MIT License:

- **Time Zone Map** (`us.dustinj.timezonemap:timezonemap`) — © Dustin Johnson.
  Offline coordinate-to-time-zone resolution (library code only; its bundled
  boundary data is covered under the ODbL section below).

## BSD 2-Clause License

- **Zstd-jni** (`com.github.luben:zstd-jni`) — © Luben Karavelov; wraps the
  Zstandard library © Meta Platforms, Inc. Native compression used by Time Zone
  Map.

## Open Data Commons Open Database License (ODbL) v1.0

Licensed under the ODbL (<https://opendatacommons.org/licenses/odbl/1-0/>):

- **Time-zone boundary data** (`us.dustinj.timezonemap:timezonemap-data`) —
  derived from [timezone-boundary-builder](https://github.com/evansiroky/timezone-boundary-builder),
  which is built from **OpenStreetMap** data. © OpenStreetMap contributors.

---

Full license texts are available from each project. This list covers the direct
dependencies; a complete, generated dependency-license report can be produced
from the build in a later pass.
