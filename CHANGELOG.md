# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Stotra — a library of hymns and shlokas.** The Stotra tile now opens a browsable library
  (`:feature:stotra`) of 20+ traditional stotras — Ganesha, Shiva, Vishnu, Devi, the Navagraha stotra,
  the peace mantras and more — each shown in Devanagari with a roman transliteration and a short note.
  A "today's stotra" is suggested by the weekday's ruling graha (e.g. Gayatri on Sunday, Hanuman on
  Tuesday, Guru on Thursday). Tapping one opens a reader with an adjustable font size for chanting. All
  text is public-domain Sanskrit. This lights up the last of the hub tiles.
- **Meditation — timed sit with a breath guide.** The Meditate tile now opens a working timer
  (`:feature:meditation`): pick a length (5–30 min), and a countdown runs with a breath-pacing circle
  that expands and contracts, plus a soft start/end bell (a generated tone — no bundled audio). Each
  finished sit is logged to a history with a daily total and a streak, stamped with the day's nakshatra.
  Panchanga hook: it surfaces today's Brahma Muhurta window with a "sit now" shortcut and an optional
  daily pre-dawn reminder (scheduled through the existing reminder pipeline and rolled forward on each
  visit).
- **Japa — mala chant counter.** The Japa tile now opens a working counter (`:feature:japa`): tap the
  ring to count beads, with a mala completing every 108 (a haptic buzz marks each round). Pick from a
  bundled catalog of mantras (Gayatri, Om Namah Shivaya, Mahamrityunjaya, and the nine Navagraha beeja
  mantras, shown in Devanagari + transliteration). Sittings are logged to a history with a daily total
  and a streak, and an in-progress mala is saved so it can be resumed. Two panchanga hooks: it suggests
  the beeja mantra of your current mahadasha lord (from your primary profile's chart), stamps each
  logged sitting with the day's nakshatra, and surfaces today's Brahma Muhurta as an auspicious time to
  sit.
- **Rashifal — daily & weekly Moon-transit outlook.** The Rashifal tile now opens a working reading
  (`:feature:rashifal`): today's verdict for a rashi plus the seven days ahead, computed from real
  astronomy rather than generic text. Each day is graded by Chandrabala (the transit Moon's house from
  the read sign); when you read your own birth Moon sign it's fully personalised with Tarabala from
  your birth star. Defaults to your primary profile's sign, and you can tap any of the twelve rashis to
  browse it (a sign-only transit reading) — the profile's own sign is starred.
- **Matchmaking — kundali matching (Guna Milan).** The Match tile now opens Ashtakoota matching: pick
  one male and one female chart-ready profile and see the 36-guna breakdown across the eight kootas
  (Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi), the total, the verdict band, and any
  Nadi / Bhakoot dosha. Scored from both Moons via the JPL-validated chart engine.
- **About — maintainer links.** The About screen now shows tappable GitHub (@imskylab) and LinkedIn
  links for the creator, and the same links were added to the README, project site, AUTHORS,
  LICENSING contact, and the issue-template contact links.
- **Muhurat — personalised to a birth chart.** The best-days results now have a profile selector
  ("General" plus each chart-ready profile, defaulting to your primary). Picking a profile re-ranks
  the days for that person by layering their Tarabala (the day's nakshatra counted from their birth
  star) and Chandrabala (the day's Moon sign counted from their birth Moon sign) onto the general
  panchanga score, with the reasons shown on each day. "General" gives the non-personalised ranking.
- **Kundali — pick which profile to view.** When you keep more than one chart-ready profile, the
  Kundali screen now shows a chip picker to switch between them (defaulting to your primary profile)
  instead of only ever showing the primary.
- **Profiles — gender.** A profile can now record a gender (Male / Female / Other), shown on the
  profile list. It's optional for a chart and groundwork for kundali matching, which pairs one male
  and one female profile.
- **Muhurat — set a reminder for a chosen day.** The muhurta day detail now has a "Set reminder for
  this day" button: it schedules a one-shot notification for that day's sunrise (named for the
  activity) and persists it, so it survives a reboot. A day already begun is declined with a message.
- **Muhurat — choose the search window.** The best-days results now have a 30 / 60 / 90-day selector,
  so you can widen or narrow how far ahead the ranking looks (default 60 days) instead of a fixed
  window.
- **Muhurat — day detail with time windows.** Tapping a ranked day in the Muhurat results now opens
  that day's detail: its weekday/tithi/nakshatra and the day's time windows split into the auspicious
  ones to prefer and the inauspicious ones to avoid (the muhurtas — Abhijit, Brahma, Rahu Kalam,
  Yamaganda, … — plus the Choghadiya), each with its start–end time.
- **Muhurat — find your best days.** The Home hub's Muhurat tile now opens a working flow
  (`:feature:muhurat`): pick a category (Bal Sanskar, Vivah, Vastu, Purchases, Medical, Business,
  Agriculture, Ceremonies), then an activity, and see the most auspicious upcoming days ranked — each
  with a star rating, a 0–100 score and the panchanga reasons that earned it. General
  (panchanga-based) guidance for now; personalisation to a birth chart comes later.
- **Muhurat: ranked best-days finder (engine).** `AstronomyEngine.bestMuhurtasFor(activity, days,
  location)` walks a date range, samples each day at its own sunrise, scores the panchanga with the
  muhurta scorer, and returns the days best-first (ties broken by the earlier date) — the data the
  upcoming "find best dates" screen will show. Still engine-only.
- **Muhurat scoring engine (foundation).** `:core:astronomy` gains an electional-muhurta layer: a
  catalog of activities grouped into eight categories (Bal Sanskar, Vivah, Vastu, Purchases, Medical,
  Business, Agriculture, Ceremonies — including Gardening), a per-activity rules table (starting with
  Griha Pravesh, Vivah, Namkaran, Vehicle and Bhoomi Poojan, the rest on a sensible default), and a
  `scoreMuhurta` that rates a day's panchanga 0–100 with reasons — nakshatra weighted most, then
  weekday and tithi, with the Rikta/Amavasya tithis, Vyatipata/Vaidhriti yogas and Vishti (Bhadra)
  karana penalised. Not yet wired to a screen; the ranked "best days" finder builds on it next.
- **About Vedic Mitra.** Settings now has an About screen with the app's version (read from the
  installed build), a short description, the author (Jayvardhan Potabatti), copyright, and the
  licensing (GNU AGPL-3.0-or-later, with a commercial license also available).
- **Kundali (birth chart).** The Kundali tile on the Home hub now opens your birth chart, computed
  from your primary profile: the lagna, the nine grahas by rashi and house (with retrograde), the
  Moon's nakshatra and pada, and your current Vimshottari mahadasha. Drawn as a **North-Indian diamond
  chart** — the square divided by its diagonals and the side-midpoint diamond into twelve fixed houses
  (house 1 at the top centre, running anticlockwise), each showing its rashi number and the grahas
  placed there (retrograde in red) — above the same details as tappable cards.
- **All nine grahas.** Planetary positions now cover the full classical set — Sun, Moon, Mars,
  Mercury, Jupiter, Venus, Saturn, Rahu and Ketu (sidereal / Lahiri) — up from four. Mars, Mercury and
  Saturn use the JPL Keplerian ephemeris; Rahu/Ketu are the mean lunar nodes. This shows on the Home
  planetary-positions list and is the first piece of the birth-chart engine.
- **Birthplace geocoding.** A profile's place of birth is now searched through the geocoder and, on
  picking a result, resolved to coordinates + an IANA time zone (reusing `:core:location`) — the exact
  location and moment a birth chart needs.
- **Birth profiles.** Settings → Profiles keeps multiple birth profiles — yourself plus family or
  friends — each with name, relation, date, exact time, and place of birth, stored on-device
  (`:feature:profile` + a `ProfileRepository` in `:core:datastore`). One is always the primary
  "Self"; the first profile you add becomes primary, and you can switch it. They're the foundation the
  upcoming astrology features (Kundali, Rashifal, personalised Muhurat) build on.
- **Home hub landing.** Home is now a hub: a tappable "today's panchang" hero, the auspicious-now
  strip, and a categorised shortcut grid (Daily · Astrology · Devotion) built with the brand's saffron
  theme and custom cultural glyphs. Daily tiles open the calendar, reminders, or the full daily
  panchang (the previous dashboard, now behind the hero and the Panchang tile); Astrology/Devotion
  tiles are shown with a "coming soon" state so the growing roadmap stays discoverable.
- **Intro splash video.** A short branded video plays full-screen (muted) when the app opens, then
  hands off to the home screen. Tapping anywhere — or the back button — skips it. The clip is bundled
  in the app (`res/raw`).
- **Tap a Home list item for its significance — and set a reminder.** Rows in the
  Auspicious/Inauspicious periods and Upcoming festivals/events lists are tappable, opening a bottom
  sheet with the item's time or date and a short, offline explanation of what it is and why it
  matters (from `PanchangaGlossary`). Muhurta and recurring-observance rows also offer a **Set
  reminder** button that schedules the reminder (via a shared `AddReminderUseCase`) so it appears and
  can be edited on the Reminders screen.
- **Flexible reminder lead time.** A reminder's "remind before" time is now any whole value in
  minutes, hours, or days (up to 30 days), entered via a number field and a unit picker — replacing
  the fixed 0/5/10/15/30-minute chips. It is still stored as total minutes, and the fired
  notification phrases the lead naturally (e.g. "starts in 2 hours").
- **Rename reminders.** Each added reminder can be given a custom display name (pencil icon → rename
  dialog); it shows as the reminder's label, survives the daily renew, and reverts to the derived
  name when cleared. Persisted alongside the reminder (older saved reminders decode unchanged).
- **Planetary positions.** The home screen shows the rashi of the Sun, Moon, Guru (Jupiter), and
  Shukra (Venus), each with the date it next changes rashi (pravesh), in a collapsible list. Sun and
  Moon reuse the existing Meeus ephemeris; Guru and Shukra are computed geocentrically from JPL
  "Approximate Positions" Keplerian elements (heliocentric solve → Earth subtraction → sidereal via
  Lahiri), with the ingress found by a daily scan plus bisection. Venus's separation from the Sun
  stays within its ~47° maximum elongation as a physical sanity check.
- **Festivals, observances, and a redesigned home dashboard.** `FestivalCalculator` derives upcoming
  named festivals (Ugadi, Rama Navami, Ganesh Chaturthi, Navaratri, Diwali, Holi, Janmashtami, Maha
  Shivaratri, …), recurring lunar observances (Ekadashi, Purnima, Amavasya, Sankashti Chaturthi,
  Pradosh, Masik Shivaratri, Vinayaka Chaturthi), and Sankrantis — each judged by that day's
  **sunrise** panchanga and cross-checked against published 2026 dates. The home screen was reworked
  around panchanga-limb and season/ayana strips, an auspicious-now band, and collapsible
  Auspicious/Inauspicious/Festivals/Events sections. See
  [docs/adr/0008-festivals-and-home-landing.md](docs/adr/0008-festivals-and-home-landing.md).
- **Choghadiya.** The sixteen Choghadiya windows (eight day, eight night, weekday-sequenced) are now
  computed and available for reminders. See [docs/adr/0011-choghadiya.md](docs/adr/0011-choghadiya.md).
- **Reminders redesign with tithi reminders.** The reminders screen now works like a clock app —
  add and remove reminders from a unified list whose sources are the day's muhurta and Choghadiya
  windows plus **custom tithi** targets (built from Maasa · Paksha · Tithi, or presets like Ekadashi,
  Purnima, Amavasya). Each is resolved to its next occurrence, fires at sunrise minus a lead, and is
  renewed on load.
- **Panchang calendar highlighting.** The monthly grid highlights festival, observance, and
  Sankranti days, and each day's detail card names its notable entry.
- **Selectable and saved locations with offline timezone detection.** GPS, city search, and manual
  latitude/longitude, with multiple saved locations, resolved to a time zone (and DST) entirely
  offline. See [docs/adr/0006-selectable-and-saved-locations.md](docs/adr/0006-selectable-and-saved-locations.md)
  and [docs/adr/0007-coordinate-timezone-resolution.md](docs/adr/0007-coordinate-timezone-resolution.md).
- **Maasa and Samvatsara.** `:core:astronomy` now derives the amanta lunar month (Maasa) and the
  sixty-year-cycle year name (Samvatsara), shown on the home dashboard and each calendar day's
  detail. The month is named from the Sun's rashi at the new moon that begins it (found by a
  synodic-seeded bisection over the Moon's elongation), and a lunation with no Sankranti is flagged
  as an **Adhika** (leap) month — correctly reporting Adhika Jyeshtha in 2026, so early August
  reads Ashadha rather than Shravana. Samvatsara follows the South-Indian Chandramana convention,
  advancing at Chaitra Shukla Pratipada (Ugadi) from the elapsed Shaka year. Cross-checked against
  drikpanchang.com and the published Ugadi 2026 almanac (Parabhava Nama Samvatsara, Shaka 1948).
  See [docs/adr/0005-maasa-and-samvatsara.md](docs/adr/0005-maasa-and-samvatsara.md).
- **Hindu calendar screen.** A new `:feature:calendar` module with a monthly panchang grid — each
  day shows its tithi (Shukla/Krishna + number); tap a day for the full panchanga of that date, and
  page between months. Backed by a new lightweight `AstronomyEngine.daySummaryAt` so a whole month
  of days is computed cheaply (no per-cell sunrise/moonrise search). Reached via a new bottom
  navigation bar (Home · Calendar · Reminders · Settings).
- **Golden brand theme.** The Material 3 theme was reworked into a golden / maroon / bronze palette
  (full tonal scheme, light and dark, plus softly-rounded shapes) drawn from the app emblem,
  replacing the placeholder indigo/saffron/teal. Dynamic (wallpaper) colour now defaults **off** so
  the brand palette is the out-of-the-box look; users can still opt into dynamic colour in Settings.
  See [docs/adr/0004-golden-theme-and-calendar.md](docs/adr/0004-golden-theme-and-calendar.md).
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
- **Smaller release builds.** Release builds now run R8 code shrinking/obfuscation and resource
  shrinking (`isMinifyEnabled` + `isShrinkResources`, with an app `proguard-rules.pro`), and ship only
  English resources (`resourceConfigurations`), dropping the dozens of translations AndroidX/Material
  bundle. Debug builds are unchanged, so the CI gate (which builds `assembleDebug`) is unaffected — the
  size win applies to `assembleRelease`/`bundleRelease`. Publish the App Bundle (`bundleRelease`) so
  Play delivers per-device (ABI/density/language) splits and users download only their slice.
- **Matchmaking — more accurate Gana koota and dosha cancellations.** The Gana koota now uses the
  standard asymmetric groom/bride table (same 6; Deva–Manushya 5; groom-Manushya + bride-Rakshasa 1;
  other Rakshasa pairings 0) instead of a symmetric one. Nadi and Bhakoot doshas now apply their
  classical cancellation (parihara) rules — Bhakoot cancels under a shared or friendly sign lord;
  Nadi cancels for same-nakshatra-different-pada, same-rashi-different-nakshatra, or
  same-nakshatra-different-rashi — so a cancelled clash keeps its lost points but drops the warning.
  (Yoni and Vashya keep their documented simplified tables; their full classical matrices vary by
  lineage and are only published as images.)
- **New hub tile artwork and larger tile icons.** The Today's Panchang tile now shows a lotus
  zodiac-wheel mandala, Muhurat a mangal kalash (sacred pot with mango leaves), Rashifal the twelve
  rashis in a zodiac wheel, Meditate a seated figure with the chakras, Calendar a mandala-bordered
  almanac page with Om marks and a turning leaf, Events a woodcut almanac page with a central Om and
  endless-knot corners, and the Match tile a clasped-hands (hastamelap) motif with a mehndi mandala —
  replacing the marigold panchang glyph, the kalash-outline muhurat glyph, the simple meditate figure,
  the plain date-range calendar icon, the plain event icon, and the placeholder Star/heart icons. All
  hub-tile icons are also enlarged to fill their chip.
- **Bottom navigation is now Home · Settings · Profile.** Panchang and Reminders left the bottom bar
  (they're already tiles on the Home hub) and Profile joined it as a first-class tab. The Home hub's
  Daily / Astrology / Devotion tabs were removed too — every shortcut now sits on one screen, each
  tile still tinted by its category. Panchang, Reminders, Kundali and the location/profile-edit
  screens open as pushed sub-routes with a back arrow in the top bar.
- **Festivals and Events are now their own full, tappable lists.** The Home hub's Festivals tile opens
  the complete list of upcoming festivals and Sankrantis, and a new Events tile opens the full list of
  lunar observances (Amavasya, Purnima, Ekadashi) — each row tappable for its significance, with a
  Set-reminder button on recurring observances. The upcoming-festivals peek moved onto the hub landing,
  and both lists were removed from the Today's Panchang detail screen (which keeps the day's limbs,
  sun/moon, planetary positions and auspicious/inauspicious periods). The Panchang shortcut is renamed
  "Today's Panchang", and Muhurat is now marked coming soon until it's implemented.
- **Reminder lead time reads as a label until you edit it.** Each reminder card now shows its lead
  time as compact text (e.g. "Remind 2 hours before" / "Remind at start") and expands to the
  number-and-unit editor only when tapped — lighter to scan and less prone to accidental edits.
- **The Calendar screen is now "Panchang."** The tab and screen were renamed to reflect what they
  show; the bottom navigation reads Home · Panchang · Reminders · Settings.
- **License changed from MIT to a dual license** — GNU AGPL-3.0-or-later for open-source use, plus a
  commercial license for proprietary use. See [LICENSING.md](LICENSING.md) and
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
- **Toolchain lifted to AGP 9 / Gradle 9 / Kotlin 2.3.** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.3.10 +
  KSP 2.3.10 (now using AGP's built-in Kotlin — the standalone `kotlin-android` plugin was removed),
  and Hilt 2.60.1. Migrated the astronomy/scheduler ports from `kotlinx.datetime.Instant` to the
  stdlib `kotlin.time.Instant`.

### Fixed
- **System back now returns to the hub from the Festivals, Events and Panchang views.** These open
  inside the Home tab (not as pushed routes), so the app-level back handler couldn't act on them and
  the device back button exited the app. Back from any of them now returns to the Home hub.
- **System back now navigates within the app instead of exiting.** Pressing the device back button
  from any screen other than the Home landing could close the app straight to the Android home screen
  (the platform's predictive-back path bypassing the in-app navigation). Back is now handled
  explicitly: it retraces the in-app journey and only leaves the app when you're already on the Home
  landing.
- **The Reminders permission banners no longer stick around after they're granted.** The "Allow exact
  alarms" and "Allow full-screen alarms" prompts were read once (the exact-alarm grant when the screen
  loaded, the full-screen grant during composition), so after the user granted a permission — or came
  back to the screen once an alarm had fired — the banner could still be showing a stale "not granted"
  state. Both are now re-checked on every screen resume, so a banner disappears the moment its
  permission is granted and never lingers.
- **The bottom-nav Home tab works after opening a shortcut.** After tapping a Home-hub tile (Panchang,
  Reminders, Kundali, …) and then switching tabs, tapping Home did nothing: because the shortcut
  screens are pushed at the graph root, Home's saved back stack included the shortcut, and the tab
  jump *restored* it instead of showing the Home landing. Tab navigation no longer saves/restores that
  state — it pops to the start destination — so every tab always lands on its own root.
- **The full-screen alarm no longer lingers after the ring stops.** When an alarm-mode reminder was
  stopped through the notification's Dismiss action or the two-minute auto-stop, the ringing service
  stopped but the lock-screen `AlarmActivity` was never told to close, so its full-screen page (with
  the Dismiss button) could stay on top of the app until its own timeout. Stopping the alarm now
  routes through the shared `AlarmRinger`, which fires a one-shot callback the activity uses to finish
  itself — so the alarm screen closes on every dismiss path, not just its own Dismiss button.
- **Returning Home from a hub tile no longer gets stuck.** Opening Panchang or Reminders from a Home
  tile and then tapping Home could freeze on a blank screen: the tiles pushed onto destinations that
  were also bottom tabs, so the save/restore state of the two navigation paths collided. Those
  screens are now plain pushed sub-routes and every tab jump goes through a single
  `navigateToTab` helper, so back and tab switching behave consistently.
- **Reminders now survive an app update, and alarm-mode reminders always surface.** Reinstalling the
  APK (like a reboot) clears pending alarms, but `BootReceiver` only re-armed on `BOOT_COMPLETED`, so
  after an update a reminder silently never fired until the Reminders screen was reopened. It now also
  re-arms on `MY_PACKAGE_REPLACED`. And the alarm receiver posts the alarm notification before starting
  the ringing foreground service (and guards the start), so the alarm is visible even if the platform
  refuses the background foreground-service start.
- **The day's tithi is now named by its sunrise, not local noon.** On days where the tithi rolls
  over between sunrise and midday, sampling at noon read one tithi ahead of published panchangas —
  e.g. 9 August 2026 showed Krishna Dwadashi where Drik/Date Panchang show Krishna Ekadashi (the
  tithi prevailing at sunrise, by which the day is named). Home and Panchang now resolve the day's
  sunrise (`AstronomyEngine.sunriseAt`) and sample the panchanga identity there, so they agree with
  each other and with reference panchangas. (Tithi is Moon−Sun elongation and so ayanamsa-independent;
  this was a sampling-instant convention, not a computation error.) See
  [docs/adr/0004-golden-theme-and-calendar.md](docs/adr/0004-golden-theme-and-calendar.md).
- **Muhurta reminders could not be set once a window had passed.** The reminders screen only ever
  computed *today's* windows, so by later in the day every row showed "already passed" with a
  disabled toggle. It now resolves each muhurta's **next upcoming** occurrence — today's if still
  ahead, otherwise tomorrow's (labelled "Tomorrow") — so a reminder can always be toggled on, and
  each time the screen opens it renews already-enabled reminders onto their next occurrence.

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

[Unreleased]: https://github.com/imskylab/vedic-mitra/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/imskylab/vedic-mitra/releases/tag/v0.1.0
