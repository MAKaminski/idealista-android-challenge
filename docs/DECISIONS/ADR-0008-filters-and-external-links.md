# ADR-0008 — Client-side filters scoped to the list payload, and external links via Custom Tabs

**Status:** Accepted · 2026-07-28

## Context

Two gaps showed up once the app was usable on a device.

**The list had no way to narrow four ads into the ones you want.** Real-estate browsing is a
filtering activity — operation, budget, rooms, amenities — and the list payload carries most of the
fields that would answer those questions.

**The payloads contain real URLs and real coordinates that went nowhere.** Every ad has a
`propertyCode` that maps to an idealista listing URL, `latitude`/`longitude`, and a gallery of
publicly-served photo URLs. All of it was rendered as pixels and none of it was reachable.

Both raise the same question: what should the app do with data it only *partly* has?

## Decision

### Filters: only criteria every list ad can answer

Offered, because they are present on all four list ads:

| Filter | Field |
|---|---|
| Sale / Rent | `operation` |
| 3+ rooms, 2+ baths | `rooms`, `bathrooms` |
| Exterior | `exterior` |
| Parking | `parkingSpace` |
| Price ascending | `priceInfo.price.amount` |
| Favorites only | local Room state |

Offered but opt-in, because the keys **vary per ad**: air conditioning, swimming pool, terrace,
garden. An unset key is treated as absent, and selected amenities are AND-ed — asking for a pool and
a terrace means both.

**Not** offered: lift, floor plan, community costs, energy certificate. Those live only in
`moreCharacteristics` on the detail response, which — per ADR-0005 — always describes ad 1. A filter
built on them would either match everything or match nothing, and either way would look like a bug.

The logic lives in `:core:model` as pure functions (`AdFilters`, `Ad.matches`,
`List<Ad>.applyFilters`) and the ViewModel combines it as a fourth flow over the Room cache.
Filtering is therefore client-side: it works offline, costs no request, and is unit-testable with no
Android runtime at all.

A distinct `NoMatches` state was added rather than reusing `Empty`. "No ads loaded" and "no ads match
your filters" have different causes and different fixes; showing one message for both would make the
filters look like a failed request.

### Links: Custom Tabs, with an honest note about the mock data

| Destination | Built from | Opens as |
|---|---|---|
| The listing page | `https://www.idealista.com/inmueble/{propertyCode}/` | Custom Tab |
| The property on a map | `geo:` URI from `latitude`/`longitude` | whichever app claims `geo:` |
| A gallery photo, full size | `multimedia.images[].url` | Custom Tab |

Custom Tabs over a raw browser hand-off: the user keeps the app's colours and the back stack, and
returns with one gesture. `ExternalLinks` falls back to `ACTION_VIEW` when no Custom Tabs provider
exists, and a device with no handler at all gets a Toast rather than an `ActivityNotFoundException`.

## Consequences

- **The listing URLs are well-formed but will not resolve.** The mock property codes are `1`–`4`;
  real idealista codes are eight digits. The URL *shape* is production-correct and the KDoc on
  `AdLinks` says so plainly. Constructing a plausible-looking link and quietly hoping nobody clicks
  it would be worse than saying it.
- The photo and map links do work — those URLs and coordinates are real.
- Filters over four ads are close to a demonstration rather than a necessity. They are built to scale
  anyway: pure predicates over the cache, so moving to server-side filtering later means changing
  where `applyFilters` is called, not rewriting the UI.
- `androidx.browser` is a new dependency in `:core:designsystem`, exposed as `api` alongside the rest
  of the shared toolkit.
- The chip row is **rebuilt from state** on every emission rather than mutated in place. Slightly
  more allocation per emission; in exchange a chip cannot render a filter the ViewModel does not
  hold.
