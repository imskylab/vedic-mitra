# 5. Amanta Maasa naming and Chandramana Samvatsara

- **Status:** Accepted
- **Date:** 2026-08-06

## Context

Adding the lunar month (Maasa) and the sixty-year-cycle year name (Samvatsara) to the panchanga is
less about new astronomy — the Sun and Moon longitudes already exist in `Ephemeris` — and more about
committing to a *convention*, because several coexist and they disagree by a whole month or a whole
year:

- **Amanta vs purnimanta.** A lunar month can be reckoned new-moon-to-new-moon (amanta, common in
  South/West India) or full-moon-to-full-moon (purnimanta, common in the North). The two agree on
  the bright fortnight but label the dark fortnight with different month names.
- **Adhika (leap) months.** The lunar year is ~11 days shorter than the solar year, so roughly every
  32.5 months an extra lunation is inserted — a month in which the Sun enters no new rashi (no
  Sankranti). Naively counting lunations from Chaitra therefore drifts: 2026 contains an Adhika
  Jyeshtha, so a naive count places early August in Shravana when the correct amanta month is
  Ashadha.
- **Samvatsara anchoring.** The sixty-name Jovian cycle can be tied to Jupiter's actual transit
  (Barhaspatya) or, in the widely-used South-Indian Chandramana scheme, advanced at Chaitra Shukla
  Pratipada (Ugadi) and derived arithmetically from the Shaka year. These can differ.

## Decision

Follow the **amanta** scheme and the **Chandramana** samvatsara throughout, both in `MaasaCalculator`.

- **Month name.** Find the new moon that begins the current lunation, then name the month from the
  Sun's sidereal rashi at that instant: `monthIndex = (sunRashiIndex + 1) mod 12` (Sun in Meena →
  Chaitra, Mesha → Vaishakha, …). This is equivalent to naming the month after the Sankranti it
  contains, but reads only one instant.
- **New-moon search.** The Moon's elongation from the Sun grows monotonically over a lunation but has
  no closed-form inverse, so `newMoonAtOrBefore`/`newMoonAfter` seed an estimate a fraction of a
  synodic month away, then bisect a ±2-day bracket — where the signed elongation is small and
  strictly increasing, well clear of the full-moon wrap — for the ascending zero crossing. This is
  the same numerical-bisection posture as `VarjyamCalculator` and `LunarDay` (see ADR 0003).
- **Adhika detection.** A lunation whose bounding new moons fall in the same rashi contains no
  Sankranti; it is flagged `Maasa.adhika` and shares the following month's name ("Adhika Jyeshtha").
- **Samvatsara.** Walk back new moon by new moon to the Chaitra that opened the current lunar year,
  take that new year's Gregorian year, convert to the elapsed Shaka year (`gregorianYear − 78`), and
  map it: `index = (shakaYear + 11) mod 60`. Because the walk finds the actual Ugadi, the samvatsara
  changes at Ugadi rather than on 1 January or at the solar new year.

Every value was cross-checked against drikpanchang.com and the published Ugadi 2026 almanac
(Parabhava Nama Samvatsara, new year 19 March 2026, Shaka 1948), and reproduced by a standalone port
of the `Ephemeris` math before the Kotlin was written:

| Date (IST) | Maasa | Samvatsara |
| --- | --- | --- |
| 2026-08-05 | Ashadha (Krishna) | Parabhava · Shaka 1948 |
| 2026-05-25 | **Adhika** Jyeshtha | Parabhava · Shaka 1948 |
| 2026-01-15 | Pausha | Vishvavasu · Shaka 1947 *(pre-Ugadi)* |
| 2026-03-25 | Chaitra | Parabhava · Shaka 1948 *(post-Ugadi)* |

The samvatsara mapping was additionally checked to reproduce the published Ugadi names for
2019–2027 (Vikari, Sharvari, Plava, Shubhakruth, Shobhakruth, Krodhi, Vishvavasu, Parabhava,
Plavanga).

## Consequences

- **Positive:** correctness survives leap months — the Adhika-aware naming reports the same month as
  reference almanacs even in a thirteen-month year, where a lunation count would be off by one.
- **Positive:** no new ephemeris primitives were needed; the feature is pure search and arithmetic
  over the existing Sun/Moon longitudes and Lahiri ayanamsa.
- **Negative:** the amanta choice means the dark-fortnight month name differs from a purnimanta
  almanac by one month — correct for the convention, but a North-Indian user may expect the other
  label. Revisit if a user-selectable amanta/purnimanta toggle is wanted.
- **Negative:** a **Kshaya** (a lunation spanning two Sankrantis, possible only near perihelion and
  rare — years apart) is named after the first ingress and not specially flagged; add handling if a
  future date range requires it.
- **Negative:** the samvatsara walk assumes the samvatsara turns at Chaitra (Chandramana). A future
  Barhaspatya (Jupiter-transit) option would need a different derivation, not just a different label.
