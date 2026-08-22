# Monetization setup — what's live, what's left

The funding and licensing rails are wired. Ko-fi, UPI, and the licensing address are real; one
donation rail is still dead, and a few commercial decisions are still open.

## 1. Rails and their status

| Rail | Destination | Status |
|---|---|---|
| **Ko-fi** | [ko-fi.com/imskylab](https://ko-fi.com/imskylab) | Live. Accepts card and PayPal, so it doubles as the PayPal route — there is deliberately no separate PayPal row. |
| **UPI** | `skylab@upi` | Live. Shown as a copyable string; **never** a `upi://` link (see ADR 0014). |
| **Commercial licensing** | `skylabs.in@gmail.com` + [pricing page](COMMERCIAL_LICENSE.md) | Live. |
| **GitHub Sponsors** | [github.com/sponsors/imskylab](https://github.com/sponsors/imskylab) | ⚠️ **Not live — 404s today.** Enable Sponsors on `@imskylab` (Settings → Sponsors; needs Stripe/bank onboarding, approval can take days). |

**The Sponsors link is the one release blocker.** Either finish onboarding before the next release,
or strip the rail: remove `GITHUB_SPONSORS` from `SupportLinks.kt`, its row in `SupportScreen.kt`,
the `github:` line in `.github/FUNDING.yml`, and the bullets in `README.md` and `docs/index.html`.
An absent row is fine; a 404 in a donation list is not.

## 2. Verify before asking anyone for money

These cannot be checked from CI, and one of them silently misroutes strangers' money if wrong:

- [ ] **Paste `skylab@upi` into a UPI app** and confirm it resolves to the payee you expect. A typo
      here sends other people's donations to a stranger.
- [ ] **Click [ko-fi.com/imskylab](https://ko-fi.com/imskylab)** — Ko-fi returns HTTP 403 to
      automated checks, so no tool has confirmed this page exists.
- [ ] **Mail `skylabs.in@gmail.com`** from the app's licensing row and confirm it arrives.
- [ ] Note that this address is now published in the app, `README.md`, `LICENSING.md` and the
      pricing page. Expect scraping; a dedicated alias is still the cleaner long-term answer.

## 3. Decisions still open

- **Pricing.** [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md) carries a first proposal (₹24,999
  Indie / ₹99,999 Business / from ₹4,99,000 per year OEM). These are starting points, not researched
  market rates — set them where you're comfortable before publicising, and remember that raising a
  published price later is harder than lowering one.
- **The agreement.** [LICENSE-COMMERCIAL.md](../LICENSE-COMMERCIAL.md) is a template and has not been
  lawyer-reviewed. Have it looked at before the first signature, and fill in `[CITY]` for
  jurisdiction.
- **GST.** Selling licenses in India has registration and invoicing implications past a turnover
  threshold. Worth an accountant's opinion before the first invoice, not after.
- **Donations vs income.** Ko-fi and Sponsors income is taxable. Keep it separate from the licensing
  rail in your records from day one.

## 4. When links change

Donation destinations live in three places that must move together — there is no single source,
because `buildConfig` is disabled and there is no codegen:

1. `feature/settings/.../SupportLinks.kt` — the in-app Support tab.
2. `.github/FUNDING.yml` — GitHub's Sponsor button.
3. `README.md` → "Support the project", and `docs/index.html` → Support section.

`SupportLinksTest` pins the shape of each constant and fails if a setup placeholder is ever
reintroduced, but it cannot tell a valid-looking wrong address from a right one — hence §2.

## 5. Deliberately not done

Play Store publishing, in-app purchases, ads, feature gating, and F-Droid submission are all out of
scope — see [ADR 0014](adr/0014-donations-and-commercial-licensing-funnel.md) for why. Revisit once
there is real donation and enquiry volume to judge by.
