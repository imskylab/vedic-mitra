# Commercial License — Vedic Mitra

Vedic Mitra is free software under the **GNU AGPL-3.0-or-later**. If the AGPL works for you,
you owe nothing and need nothing from this page — use it, fork it, ship it.

This page is for everyone the AGPL does *not* work for: teams who want to build on Vedic Mitra
inside a **proprietary, closed-source product** and cannot publish their own source in return.

---

## Do you need a commercial license?

**No, if you are:**

- using the Vedic Mitra app, for anything, including professionally;
- forking or modifying it and releasing your version under the AGPL;
- contributing patches upstream;
- building an internal tool your organisation uses privately and does not distribute or host for
  outside users.

**Yes, if you are:**

- shipping an app or SDK that includes Vedic Mitra code (in whole or in part, modified or not)
  **without** releasing your own source under the AGPL;
- embedding `:core:astronomy` — the panchanga, muhurta, kundali and rashifal engine — in a
  closed-source product;
- running a **hosted or SaaS service** built on Vedic Mitra. The AGPL's §13 network clause means
  your users must be offered your complete corresponding source, even though you never "distribute"
  a binary. A commercial license removes that obligation;
- white-labelling the app for a temple, publisher, astrologer, or brand;
- required by your own customers or legal team to avoid copyleft dependencies.

If you are unsure, ask. Answering the question costs nothing.

---

## What you are actually buying

The engine, not the wrapper. `:core:astronomy` is roughly 6,700 lines of computed astronomy:
Meeus ephemeris and Lahiri ayanamsa, the full panchanga with sunrise-tithi day naming, muhurta and
kalam derivation, natal charts with seventeen divisional charts, three dasha systems to three
levels, ashtakavarga, graha drishti, and Ashtakoota matching with Mangal dosha and the four
additional porutham.

Behind it sit 40 test files and about 6,000 lines of tests, cross-checked against published almanacs
and an independent reference implementation before each release — the rules are derived from
reference data and pinned as inline goldens rather than transcribed from memory, so a regression
fails CI rather than reaching a chart. It has no network dependency and no ephemeris data files to
license.

A commercial license grants you the right to use that work in a product you keep closed.

---

## Pricing

Prices are per shipped application, one-time, and perpetual for the major version licensed.
All prices are exclusive of GST and any applicable local taxes.

| Tier | Who it's for | Price |
|---|---|---|
| **Indie** | Solo developers and companies under ~₹50 lakh (~US$60k) annual revenue. One application. | **₹24,999** (~US$299) |
| **Business** | Companies above that threshold. One application. Includes 12 months of updates and email support. | **₹99,999** (~US$1,199) |
| **OEM / Embedded** | Unlimited applications, SaaS and hosted use, white-labelling, and sublicensing to your own customers. | **from ₹4,99,000/year** (~US$5,999), scoped per deal |

**Add-ons**

- **Engine-only license** — `:core:astronomy` alone, without the UI layer, at Indie pricing.
- **Integration support** — ₹25,000/month retainer for direct help wiring the engine into your app.
- **Custom work** — regional panchanga conventions, additional ayanamsas, alternative muhurta
  rulesets: quoted per scope.

**What every tier includes:** perpetual right to use the licensed version in the licensed
product(s), no attribution requirement in your UI, no copyleft obligation on your own code, and no
runtime dependency on this project's infrastructure (there is none — the engine is offline).

**What no tier includes:** a warranty of astrological or astronomical correctness, an SLA unless
separately agreed, or exclusivity. Vedic Mitra remains AGPL for everyone else.

---

## How to buy

Email **skylabs.in@gmail.com** with:

1. Your company name, country, and website.
2. The application(s) you intend to ship, and roughly what they do.
3. Which parts you plan to use — the whole app, or specific modules such as `:core:astronomy`.
4. Whether it is distributed, hosted/SaaS, or both.
5. The tier you believe applies.

You will get a quote and the agreement — see [LICENSE-COMMERCIAL.md](../LICENSE-COMMERCIAL.md) for
the template terms — usually within a few business days. Payment by bank transfer or UPI; an
invoice with GST details is issued on request.

---

## FAQ

**Can I evaluate before buying?**
Yes. Evaluate under the AGPL for as long as you like. The license is needed when you ship.

**We already shipped without a license. Now what?**
Email and say so. This gets resolved with a license backdated to first distribution, not with
lawyers. Acting late is much cheaper than being found.

**Does a license cover future versions?**
It covers the major version you license, perpetually. Business tier includes 12 months of updates;
after that you may keep using what you have or renew for the next major version.

**Do you offer discounts?**
Temples, non-profits, educational institutions and genuinely open-source-but-AGPL-incompatible
projects: ask. There is usually a workable answer.

**Can we get the code under a different open-source license instead?**
Sometimes — for an AGPL-incompatible open-source project, a targeted exception may be simpler than
a commercial license for both of us. Ask.

**Who owns the copyright?**
Jayvardhan Potabatti, solely. Contributors grant commercial-relicensing rights under
[CONTRIBUTING.md](../CONTRIBUTING.md), so the rights offered here are clean and unencumbered.

---

*This page is a plain-language summary, not legal advice and not an offer capable of acceptance.
The signed agreement governs.*
