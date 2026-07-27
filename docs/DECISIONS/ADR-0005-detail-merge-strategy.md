# ADR-0005 — Merge cached list identity with the detail payload

**Status:** Accepted · 2026-07-27

## Context

The upstream README says it plainly: *"Please note: the response is always the same."*
`detail.json` returns **ad 1** no matter which ad the user tapped. It also uses a different id type
(`Int adid` vs `String propertyCode`), a different price shape (`priceInfo.amount` vs
`priceInfo.price.amount`), nests the coordinates under `ubication`, and renames `description` to
`propertyComment`.

Binding that response directly to the detail screen produces a demo that looks fine on ad 1 and
**silently shows the wrong price, address and location for ads 2, 3 and 4**. It is exactly the kind
of thing that passes a casual click-through and fails a careful review.

Alternatives considered:

- **Render the detail response as-is.** Faithful to the endpoint, wrong for the user.
- **Ignore the detail endpoint and build the screen from the list data.** Correct, but throws away
  the characteristics, energy certificate and full gallery — the interesting half of the screen.
- **Show a "mock data" banner.** Honest, but it puts the API's problem in the user's face.

## Decision

**Compose the detail model from both sources**, in the repository, in one place:

| From the cached list ad (identity) | From the detail response (rich content) |
|---|---|
| `propertyCode`, address, district, neighborhood | `moreCharacteristics` (rooms, lift, community costs, floor…) |
| price + currency suffix, operation, property type | `energyCertification` |
| thumbnail, coordinates | full image gallery with `localizedName` |
| | `propertyComment` |

The detail payload's `adid`, `priceInfo` and `ubication` are **discarded on purpose**, with a comment
at the merge site saying why.

A regression test — `detailForAd3_neverShowsAd1Identity` — locks this in, and both `API.md` and
`CLAUDE.md` warn future contributors (human or AI) not to "fix" it by trusting the response's own id.

## Consequences

- Every ad's detail screen shows *that ad's* identity, while still displaying the rich fields only the
  detail endpoint provides.
- The detail screen requires the ad to be in the cache — natural here, since it is always reached from
  the list, and the cache is Room-backed so it survives restarts.
- If the endpoint ever becomes id-aware, the merge collapses to "use the response" and the test is the
  thing that tells us it's safe to do so.
