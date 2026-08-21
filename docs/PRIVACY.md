# Privacy Policy — Vedic Mitra

**Effective date:** 21 August 2026
**Applies to:** the Vedic Mitra Android application, all versions distributed from
[github.com/imskylab/vedic-mitra](https://github.com/imskylab/vedic-mitra).

---

## The short version

Vedic Mitra collects nothing, sends nothing, and has no servers. There is no account, no
analytics, no crash reporting, no advertising, and no tracking of any kind. Everything you enter —
your birth details, saved locations, reminders, japa and meditation history — stays in the app's
private storage on your device, and is deleted when you uninstall the app.

This is verifiable rather than promised: the source is public, and the app builds with no network
client library of any kind.

---

## What the app stores, and where

All of it lives in the app's private on-device storage (Android `DataStore`). None of it is
transmitted anywhere.

| Data | Why it exists |
|---|---|
| Birth profiles (name, date, time, place) | Computing kundali, dasha, matchmaking, and rashifal |
| Saved locations and coordinates | Computing panchanga for the right place |
| Reminders and alert preferences | Scheduling muhurta, Choghadiya, and tithi notifications |
| Japa and meditation session history | Streaks and progress |
| Theme and dynamic-colour preferences | Remembering how you like the app to look |

There is no backup to any cloud service operated by this project. If you have Android's own
backup enabled, Android may include app data in your Google account backup under Google's terms —
that is a platform behaviour, outside this project's control.

## Permissions, and exactly what each is for

- **Location** (`ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`) — optional. Used only to work out
  your coordinates so sunrise, tithi, and muhurta timings are right for where you are. Coordinates
  are used in on-device calculations and stored locally. They are never transmitted. You can skip
  this entirely and enter locations manually.
- **Internet** (`INTERNET`) — used for exactly one thing: Android's own `Geocoder` service, when you
  search for a city by name while adding a location. That lookup is performed by the operating
  system, not by this app, and is subject to your device vendor's privacy policy. If it fails, the
  app carries on and you can enter coordinates directly. **No other feature uses the network.**
  Time-zone resolution, ephemeris calculation, and the entire panchanga engine run offline.
- **Notifications** (`POST_NOTIFICATIONS`) — to show the reminders you create.
- **Exact alarms** (`SCHEDULE_EXACT_ALARM`), **full-screen intent**, **vibrate**, **foreground
  service** — to make muhurta reminders fire at the right minute and ring when you ask them to.
- **Boot completed** (`RECEIVE_BOOT_COMPLETED`) — to restore your scheduled reminders after a
  restart, so they aren't silently lost.

## What the app does not do

- No user accounts, sign-in, or identifiers.
- No analytics, telemetry, or usage statistics.
- No crash or performance reporting.
- No advertising SDKs, and no ad identifiers.
- No sale or sharing of personal information — there is nothing to sell, and no recipient.
- No profiling, and no automated decisions about you.

## Links that leave the app

The About and Support screens contain links to GitHub, LinkedIn, GitHub Sponsors, and Ko-fi. Tapping
one opens your browser and hands you to that service, where **their** privacy policy applies and
this one stops. Vedic Mitra learns nothing about whether you followed a link, or what you did next —
including whether you donated.

The Support screen's UPI option copies a payment address to your clipboard. It initiates no payment
and contacts no payment provider; anything after the copy happens in your own UPI app.

## Children

Vedic Mitra is not directed at children and collects no data from anyone, including children.

## Your rights

Because nothing leaves your device, there is no data of yours to access, export, correct, or erase
on any server. To delete everything the app holds, clear the app's storage or uninstall it.

## Third-party components

The app is built on open-source libraries and, in Google-services builds, the proprietary Play
services Location library used for the optional location feature. These are listed in
[THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md). None of them is configured to report data to
this project.

## Changes to this policy

Material changes will be recorded in [CHANGELOG.md](../CHANGELOG.md) and reflected in the effective
date above. Because the policy is version-controlled, you can read its full history in git.

## Contact

Questions about this policy: open an issue at
[github.com/imskylab/vedic-mitra/issues](https://github.com/imskylab/vedic-mitra/issues), or contact
Jayvardhan Potabatti via [GitHub @imskylab](https://github.com/imskylab).
