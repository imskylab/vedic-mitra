# 14. Monetization via donations and commercial licensing, not in-app purchases

- **Status:** Accepted
- **Date:** 2026-08-21

## Context

Vedic Mitra has real running costs — a developer's time, chiefly — and no way for anyone to
contribute to them. The project already had the legal foundation for monetization (AGPL-3.0-or-later
with a single copyright holder and a contributor grant permitting commercial relicensing, per
`LICENSING.md` and `CONTRIBUTING.md`), but none of the rails: no `FUNDING.yml`, no sponsor button,
no donation link in the app or on the landing page, and a commercial license advertised with no
price, no terms, and no contact channel a company would actually use.

The forces at play:

- **The AGPL makes feature gating close to pointless.** Anyone can rebuild from source with the
  paywall removed and redistribute that build legally. A gate would inconvenience honest users while
  stopping nobody.
- **The app's whole positioning is "offline, no ads, no tracking, not the product."** A billing SDK,
  an ads SDK, or an entitlement server would contradict the pitch that earns the goodwill in the
  first place.
- **Play Store distribution carries real, permanent overhead** — Console account, Data Safety
  disclosures, target-SDK treadmill, listing assets, privacy-policy hosting — that is not worth
  taking on before there is evidence of demand.
- **The genuinely valuable asset is `:core:astronomy`**, not the UI: an offline panchanga/muhurta/
  kundali engine cross-checked against established almanacs. That is what a business would want to
  embed, and embedding it in a closed product is exactly what the AGPL forbids without a license.

## Decision

Monetize through **two rails only**: voluntary donations, and commercial licensing for proprietary
use. Specifically:

1. **No in-app purchases, no ads, no feature gating, no entitlement layer.** Every feature stays
   available to everyone. The in-app Support screen states this explicitly, so the ask is never
   mistaken for a transaction.
2. **Links only.** The Support screen opens the browser via `LocalUriHandler` or copies to the
   clipboard. No new dependency, no manifest change, no network call — the app stays offline and
   stays acceptable to FOSS channels such as F-Droid/IzzyOnDroid.
3. **UPI is a copyable VPA, never a `upi://` URI.** Compose's `AndroidUriHandler.openUri` rethrows
   `ActivityNotFoundException` as `IllegalArgumentException` and nothing catches it, so a `upi://`
   link would crash the app on every device with no UPI app installed — every non-Indian device and
   every emulator. Android 11+ package visibility also prevents a pre-flight `resolveActivity` check
   without a `<queries>` entry. Copying the VPA is a first-class flow in every major UPI app and
   degrades to inert text elsewhere.
4. **The commercial license becomes a real product**: published tiers and prices
   (`docs/COMMERCIAL_LICENSE.md`), a template agreement (`LICENSE-COMMERCIAL.md`), and a dedicated
   email as the contact channel rather than a public issue — companies do not negotiate licensing in
   the open.
5. **An AGPL section 7 additional permission is granted** (`LICENSE-EXCEPTIONS.md`) for linking with
   proprietary platform libraries. This is needed *today*: the app already links
   `play-services-location`. It is written broadly enough to cover future Google or OEM libraries.
6. **A privacy policy is published** (`docs/PRIVACY.md`), because the landing page's "no tracking"
   claims had nothing behind them and asking for money raises the bar on that.
7. **Support is a top-level bottom-bar destination**, not a buried Settings row — the ask is
   visible, and the screen's own copy ("nothing here unlocks anything") is what keeps it honest.
   Accepted tension: this is the most prominent placement in an app whose pitch is "your practice
   isn't the product". Moving it back to a sub-route is a small change if it ever reads as too much.
8. **Stay off the Play Store for now.** Distribution remains GitHub Releases.

## Consequences

- **Positive:** supporters have a path in three taps; businesses can self-qualify against published
  prices instead of opening an enquiry to learn there is no price list; the offline/no-tracking
  promise survives intact; F-Droid remains viable; no billing code to maintain, test, or migrate.
- **Positive:** the `SettingsScreen`/`AboutScreen` row duplication was collapsed into shared
  `SettingsComponents` on the way, keeping `SettingsContent` well under Detekt's `LongMethod`
  threshold as sections accumulate.
- **Negative:** donation conversion is low by nature. This will not fund the project on its own; the
  commercial rail is the one with real upside, and it depends on inbound that may take months.
- **Negative:** three places now hold the same links (`SupportLinks.kt`, `.github/FUNDING.yml`,
  `README.md`). Single-sourcing is not possible with `buildConfig` disabled and no codegen, so they
  must be edited together; `SupportLinksTest` pins their shape as a partial guard.
- **Deferred:** a second "Open UPI app" row guarded by a `<queries>` entry plus an explicit
  `resolveActivity` check and a raw `startActivity` — only if users ask for it.
- **Deferred:** Play Store presence, in-app purchases, and paid content packs. Revisit when donation
  and enquiry volume justifies the compliance overhead; nothing in this decision blocks that, and
  the section 7 permission above already pre-authorises Play Billing if it ever happens.
