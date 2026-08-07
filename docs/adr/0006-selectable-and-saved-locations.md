# 6. Selectable and saved locations with per-location time zones

- **Status:** Accepted
- **Date:** 2026-08-07

## Context

Until now the panchanga was always computed for the device's GPS position, or — when that was
unavailable — a single hard-coded New Delhi fallback duplicated in `HomeViewModel` and
`CalendarViewModel`. Phase 7 asks for the user to be able to choose *where* the panchanga is
computed: pick a city, enter coordinates directly, and keep several saved locations to switch
between (README Phase 7, "Location Support").

Two design forces shaped this:

- **Module boundaries.** The chooser must combine the *device* location (`:core:location`) with the
  user's *saved* choice (`:core:datastore`), but the dependency rules (docs/architecture.md) forbid
  one capability/data module depending on another — both may depend only on `:core:common`.
- **Time zones.** A panchanga day is bounded by local events (sunrise, "today"). Computing a distant
  city's day while using the device's `ZoneId.systemDefault()` — as `CalendarViewModel.noonOf` did
  — mis-places every day boundary by the zone difference. A chosen location therefore needs to carry
  its *own* zone. A precise coordinate-to-zone mapping, however, would mean bundling multi-megabyte
  timezone-boundary data, which is at odds with the app's light, offline-first footprint.

## Decision

Introduce a **selected-location** concept resolved through a new **Domain** module, and give every
saved location its **own IANA time-zone id**.

- **Model.** `SavedLocation(id, label, coordinates, zoneId, source)` in `:core:common`
  (`source ∈ {DEVICE, CITY, MANUAL}`). Persisted by a new `LocationRepository` in `:core:datastore`
  following the existing Preferences-DataStore + codec pattern (`SavedLocationCodec` mirrors
  `ReminderCodec`; the saved set is a `stringSet` key, the selected id a `string` key).
- **Resolution.** A new `:core:domain` module hosts `ResolveLocationUseCase`, which may depend on
  both `:core:location` and `:core:datastore`. It resolves in order: **selected saved location →
  device location → built-in default (New Delhi)**, returning a `ResolvedLocation(coordinates,
  zoneId, label, isDefault)`. This removes the duplicated fallback from the two ViewModels, which now
  depend on the use case and thread its `zoneId` into their day-boundary math.
- **City search** uses the platform `Geocoder` behind a `GeocodingClient` port in `:core:location`
  (the blocking overload, run on the I/O dispatcher, since the callback overload is API 33+ and
  `minSdk` is 26). It needs `INTERNET`, declared in the module manifest.
- **Time zone.** Rather than bundle boundary data, each new location gets a best-guess fixed-offset
  zone from longitude alone (`TimeZoneEstimator`, 15°/hour), which the add-coordinates screen
  pre-fills and lets the user replace with a proper named IANA id (validated with `ZoneId.of`). City
  results are saved with the estimate. The day-boundary math always uses the location's stored
  `zoneId`.
- **UI.** A new `:feature:location` module holds the saved-locations list (select / delete / use
  current) plus add-by-city and add-by-coordinates screens, reached from a new "Location" row in
  Settings via non-tab routes in the app's `NavHost`.

## Consequences

- **Positive:** the panchanga can be computed for anywhere, and day boundaries land in that place's
  local time; the New-Delhi fallback now lives in exactly one place.
- **Positive:** the resolver gives `:core:domain` a first concrete inhabitant, matching the Domain
  layer the architecture already describes, without bending the module-dependency rules.
- **Negative:** the auto-guessed zone is a fixed UTC offset with **no DST**. It is correct for
  fixed-offset regions (e.g. India) and a sensible starting point elsewhere, but a user in a
  DST-observing zone must set the named IANA id to get correct summer-time boundaries. A precise
  coordinate→zone lookup and a searchable zone picker (including for the city flow) are deferred; the
  README's "Automatic timezone detection / DST" items remain open.
- **Negative:** city search depends on the platform geocoder, which needs network and whose result
  quality varies by device; failures surface as an inline message rather than a crash.
