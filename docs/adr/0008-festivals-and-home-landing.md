# 8. Festivals and the home landing screen

- **Status:** Accepted
- **Date:** 2026-08-07

## Context

Two problems, one screen.

- **Home duplicated the calendar.** Home rendered the same full panchanga table as the calendar's
  selected-day detail card (ADR 0004 already flagged this). Opening the app landed the user on what
  was effectively "today's calendar cell" — no distinct value, and every field shown twice.
- **No festival awareness.** The app computed the panchanga but never told the user what's *coming* —
  the next Ekadashi, the next Diwali. That data is derivable from the panchanga we already compute.

## Decision

Rebuild Home as a **glanceable landing view**, and add a panchanga-derived **festival calculator**
to feed it.

### Home landing

Home stops repeating the panchanga table and instead shows: today's identity (vara, tithi, maasa,
samvatsara, moon phase); the **auspicious window in effect now** (an auspicious muhurta in gold, or
an inauspicious one like Rahu Kalam as a caution, else the next auspicious window); a sun/moon strip;
the **next festival**; and quick actions to the calendar, reminders, and location. The exhaustive
rows (nakshatra, yoga, karana, ayana, ritu, every muhurta, golden hours) now live **only** on the
calendar's day detail, reached from Home via "See full panchang". The auspicious-now widget reads
the muhurta `quality` flag that the snapshot always carried but Home never displayed.

### Festival calculator (`:core:astronomy`)

`upcomingFestivals` walks forward day by day and judges each day by the panchanga **at its sunrise**:

- **Observances**, computed directly from the tithi: Ekadashi (tithi 11/26), Purnima (15), Amavasya
  (30), and a Sankranti whenever the Sun has entered a new rashi since the previous sunrise.
- **Named festivals**, matched against a table of **amanta** maasa + tithi rules (the app is amanta
  throughout — ADR 0005): Ugadi, Rama Navami, Akshaya Tritiya, Buddha/Guru Purnima, Raksha Bandhan,
  Krishna Janmashtami, Ganesh Chaturthi, Navaratri, Vijayadashami, Diwali, Maha Shivaratri, Holi.

A named festival on a day overrides the generic observance for that tithi (e.g. Guru Purnima, not
"Purnima"); an Adhika month hosts none of its festivals; each name is emitted once (its next
occurrence). Following the codebase pattern (`maasaOf` takes longitude lambdas), the rule engine
takes an injected `FestivalPanchangaSource`, so it is unit-tested deterministically with synthetic
panchanga; `DefaultAstronomyEngine` wires the real ephemeris-backed source.

**Festival-day convention.** The day a festival "falls on" is the date whose **sunrise tithi** matches
the rule. This matches how most published calendars pick the day and is exact for the tithi-defined
observances and daytime festivals. Cross-checked against published 2026 dates (Ugadi 19 Mar, Rama
Navami 27 Mar, Ganesh Chaturthi 14 Sep, Vijayadashami 20 Oct, Diwali 8 Nov, Maha Shivaratri 15 Feb,
Holi/Phalguna Purnima 3 Mar).

## Consequences

- **Positive:** Home is a distinct landing screen; the panchanga table has a single home (the
  calendar); the app now surfaces upcoming festivals from the same offline ephemeris, no dataset.
- **Positive:** the festival rule engine is pure and synthetic-testable; adding or amending a
  festival is one line in the rule table.
- **Negative — timing-sensitive festivals.** A few festivals are traditionally timed to night rather
  than sunrise — Janmashtami (nishita/midnight), Maha Shivaratri (nishita), Diwali (pradosh/amavasya
  at dusk). They use the same sunrise-tithi rule here and so **may differ by a day** from almanacs
  that time them to night. This is the same Drik-vs-tradition simplification noted for Ayana/Ritu
  (ADR 0005); a per-festival timing rule can refine it later.
- **Negative — regional variation.** Festival names, dates, and which are observed vary by region
  (amanta vs purnimanta labelling, local customs). The rule set is a mainstream amanta baseline, not
  a regional authority; a future "regional panchang" option (the remaining Phase 7 item) would layer
  on top.
- **Performance:** the day-by-day scan computes the (search-based) maasa only on days whose tithi
  could match a festival rule, so a ~200-day lookahead stays cheap; it runs on the default dispatcher.
