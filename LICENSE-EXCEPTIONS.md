# License Exceptions — Vedic Mitra

Vedic Mitra is licensed under the **GNU Affero General Public License, version 3 or later**
(see [LICENSE](LICENSE)). The additional permission below is granted by the copyright holder under
**section 7** of that license, and forms part of the terms under which Vedic Mitra is distributed.

---

## Additional permission — linking with proprietary platform libraries

> **Additional permission under GNU AGPL version 3 section 7**
>
> As an additional permission under section 7 of the GNU Affero General Public License version 3,
> the copyright holder of Vedic Mitra gives you permission to link Vedic Mitra, or a work based on
> Vedic Mitra, with the following, and to convey the resulting work under terms of your choice with
> respect to those components only:
>
> 1. **Google Play services** client libraries (`com.google.android.gms:*`), including but not
>    limited to Play services Location and the Google Play Billing Library;
> 2. **Google Play Core / Feature Delivery** libraries (`com.google.android.play:*`);
> 3. any **proprietary library, framework, or service supplied by the operating-system vendor or
>    device manufacturer** as part of, or as an official add-on to, the platform on which the work
>    runs.
>
> This permission applies only to the components listed above. All other terms of the GNU AGPL
> version 3 continue to apply to Vedic Mitra itself, including the obligation to make corresponding
> source available for the AGPL-covered portions.
>
> This additional permission is granted to all recipients and is **not** revoked by conveying the
> work onward. You may, at your option, remove this additional permission from any copy you convey,
> as provided by section 7 of the AGPL; downstream recipients of copies you convey unmodified
> continue to receive it.

---

## Why this exists

Vedic Mitra already depends on `com.google.android.gms:play-services-location` for fused location
on Google-services devices. That library is proprietary. Distributing an AGPL work linked against a
proprietary library is, on a strict reading, incompatible with the license unless the copyright
holder grants an exception — so this document grants one, explicitly and permanently, rather than
leaving redistributors to guess.

The permission is written to cover platform-vendor libraries generally, so that adding another
Google or OEM library later does not require a fresh licensing exercise.

## What it does not do

- It does **not** weaken the AGPL for Vedic Mitra's own code. Modifications to Vedic Mitra, and
  network deployments of it, remain subject to the AGPL, including section 13.
- It does **not** grant any rights in the Google or vendor libraries themselves. Those are governed
  by their own licenses; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
- It is **not** a commercial license. To use Vedic Mitra in a proprietary product, see
  [LICENSING.md](LICENSING.md) and [docs/COMMERCIAL_LICENSE.md](docs/COMMERCIAL_LICENSE.md).

## A note for FOSS distributors

Builds intended for FOSS-only channels can omit the Play services dependency entirely; the app's
location feature degrades to manually saved locations and coordinate entry, and every panchanga
calculation is unaffected because the astronomy runs offline. This exception exists to make the
Google-services build unambiguously distributable, not to require anyone to ship it.

---

*Granted by Jayvardhan Potabatti, sole copyright holder of Vedic Mitra. © 2026.*
