# 17. Purnimanta month naming as a user preference

- **Status:** Accepted (refines ADR 0005)
- **Date:** 2026-09-01

## Context

ADR 0005 chose the **amanta** scheme — the lunar month runs new moon to new moon, named for the
solar rashi at the new moon that opens it. That decision was right for the engine and remains
unchanged. What it did not address is that the app then *presented* amanta as the only answer, with
nothing on screen saying which convention was being followed.

For a large share of users that is quietly wrong. Under **purnimanta**, used across much of North
India, a month ends at the full moon instead. The two schemes agree through the bright fortnight and
disagree through the dark one, where purnimanta already uses the *following* month's name. So for
roughly a fortnight in every lunar month the same day carries two different month names, and both
readings are correct.

The practical effect is on how festival dates read. A reader whose almanac says Phalguna Krishna and
whose app says Chaitra Krishna has no way, today, to tell whether the app is wrong or simply
following the other convention. That is the failure: not the calculation, but the silence around it.

## Decision

**Purnimanta is a relabelling, not a second calculation.** The engine continues to compute amanta
throughout — every tithi, window, festival date and the year boundary is untouched. Only the month
*name* changes, through `Maasa.nameIn(reckoning, paksha)`:

- **Shukla paksha** — both schemes give the same name.
- **Krishna paksha** — purnimanta gives the following month's name.

**The scheme is a persisted user preference** (`MaasaReckoning`, in `:core:common` so
`:core:datastore` need not depend on the engine), defaulting to **amanta** — an existing user's
reading must not change under them without their choosing it. An unset or unrecognised stored value
also falls back to amanta rather than to a guess.

**The active scheme is named on every reading, not only when it is the non-default one.** The
calendar's Maasa row carries "Amanta" or "Purnimanta" beside the value. A reader whose almanac
disagrees is exactly the reader who would never think to look in Settings, so the answer has to be
where the disagreement appears.

**The year boundary is unaffected.** Chaitra Shukla Pratipada opens the year in both schemes, so the
samvatsara (ADR 0005) and the era years (`EraYears`) need no adjustment. The schemes differ about
what the dark fortnight is called, not about where the year starts.

## Consequences

Two screens show month names — the calendar's day detail and Home's hero — and both now read the
same preference, so they cannot disagree. The sankalpa frame takes the reckoning for the same
reason: it sits directly below the Maasa row and would otherwise contradict it.

`:feature:calendar` and `:feature:home` gain a dependency on `:core:datastore`, which the layer
rules permit (features may depend on any `:core:*`). The preference is folded into UI state rather
than triggering a reload — switching it must relabel the day already on screen without recomputing a
month of ephemeris.

**One case is deliberately unverified: the adhika (intercalary) month.** During a leap month's dark
fortnight the rule applied mechanically drops the "Adhika" prefix, because the month following an
Adhika Jyeshtha is the nija Jyeshtha rather than Ashadha. Sources differ on how an intercalary month
is labelled in purnimanta usage and this project has no independent implementation to check it
against, so the behaviour is pinned by a test that says outright it is pinned to prevent drift
rather than because it is confirmed. `MaasaNaming.kt` names this as the case to distrust first.

Regional solar calendars — Tamil, Malayalam, Bengali, Odia — remain unaddressed. They are a
different axis from the lunar-month scheme and need their own work.
