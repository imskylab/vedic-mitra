# Monetization setup checklist

The funding and licensing rails are in place in code and docs, but four values are **placeholders**
and every one of them is a dead link until you replace it. Work through this list before the next
release, and delete a rail entirely rather than shipping it broken — an absent Ko-fi row is fine, a
404 is not.

## 1. Accounts to create

| Rail | Action | Then |
|---|---|---|
| **GitHub Sponsors** | Enable Sponsors on `@imskylab` (Settings → Sponsors; needs Stripe/bank onboarding, can take a few days to approve). | `https://github.com/sponsors/imskylab` already used everywhere — no edit needed once live. |
| **Ko-fi** | Create the page and note the slug. | Replace `your-kofi-slug` (4 places, below). |
| **UPI** | Decide which VPA receives donations — ideally one separate from your personal banking. | Replace `your-vpa@bank` (3 places, below). |
| **Licensing email** | Create a dedicated alias, e.g. `licensing@` on a domain you control, or a purpose-made mailbox. Do **not** use your employer address. | Replace `licensing@example.com` (4 places, below). |

## 2. Placeholders, and every file they appear in

Replace all occurrences — `git grep` each token to confirm none are left:

```bash
git grep -n "your-kofi-slug\|your-vpa@bank\|licensing@example.com"
```

| Placeholder | Files |
|---|---|
| `your-kofi-slug` | `feature/settings/.../SupportLinks.kt`, `.github/FUNDING.yml`, `README.md`, `docs/index.html` |
| `your-vpa@bank` | `feature/settings/.../SupportLinks.kt`, `README.md`, `docs/index.html` |
| `licensing@example.com` | `feature/settings/.../SupportLinks.kt`, `LICENSING.md`, `README.md`, `docs/COMMERCIAL_LICENSE.md` |

`SupportLinksTest` asserts the *shape* of these values, not their contents, so it will keep passing
after you substitute real ones. It fails if someone turns the UPI entry into a `upi://` URI — that
guard is deliberate; see ADR 0014.

## 3. Decisions still open

- **Pricing.** `docs/COMMERCIAL_LICENSE.md` carries a first proposal (₹24,999 Indie / ₹99,999
  Business / from ₹4,99,000 per year OEM). These are starting points, not researched market rates —
  set them where you're comfortable before publishing, and remember that raising a published price
  later is harder than lowering one.
- **The agreement.** `LICENSE-COMMERCIAL.md` is a template, not lawyer-reviewed. Have it looked at
  before you sign the first deal, and fill in `[CITY]` for jurisdiction.
- **GST.** Selling licenses commercially in India has registration and invoicing implications past a
  turnover threshold. Worth an accountant's opinion before the first invoice, not after.
- **Donations vs income.** Sponsor and Ko-fi income is taxable. Keep it separate from the licensing
  rail in your records from day one.

## 4. After merge

- [ ] Confirm GitHub renders the **Sponsor** button on the repo (Insights → Community Standards).
- [ ] Click every link in the app's Support screen on a real device.
- [ ] Click every link in `README.md`, `docs/index.html`, and `docs/COMMERCIAL_LICENSE.md`.
- [ ] Confirm `docs/PRIVACY.md` renders at
      `https://imskylab.github.io/vedic-mitra/` — or link it explicitly if Pages only serves
      `index.html`.
- [ ] Add "commercial licensing" and "sponsor" to the repo's About topics for discovery.

## 5. Deliberately not done

Play Store publishing, in-app purchases, ads, feature gating, and F-Droid submission are all out of
scope — see ADR 0014 for why, and the plan of record for what a Play milestone would involve.
Revisit once you can see real donation and enquiry volume; deciding then costs nothing, and
deciding now costs a Play Console account, a Data Safety form, and a permanent listing obligation.
