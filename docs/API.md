# API contract

Two static endpoints, served from GitHub Pages. Both were re-fetched live on 2026-07-27 and matched
byte-for-byte against the fixtures committed at the repo root (`list.json`, `detail.json`), which are
reused as test fixtures so schema drift fails the build.

| | URL |
|---|---|
| List | https://idealista.github.io/android-challenge/list.json |
| Detail | https://idealista.github.io/android-challenge/detail.json |

Both are unauthenticated `GET`s returning `application/json`. Images are served from
`https://img4.idealista.com/...` (`image/webp`, verified reachable).

---

## `list.json`

A JSON **array** of 4 objects (`propertyCode` `"1"`–`"4"`).

| Field | Type | Notes |
|---|---|---|
| `propertyCode` | String | **String**, not a number. Primary key. |
| `thumbnail` | String | Single image URL; equals `multimedia.images[0].url`. |
| `floor` | String? | `"2"`, `"6"`, `"4"` — string, may be absent. |
| `price` | Double | Duplicated by `priceInfo.price.amount`. |
| `priceInfo.price.amount` | Double | **Nested twice** — see the detail shape below. |
| `priceInfo.price.currencySuffix` | String | `"€"` — a suffix, so format as `1.195.000 €`. |
| `propertyType` | String | `"flat"` for all four. |
| `operation` | String | `"sale"` or `"rent"`. Note ads 2 and 4 are `rent` with sale-sized prices — mock data, render it as given. |
| `size` | Double | m². |
| `exterior` | Boolean | |
| `rooms` / `bathrooms` | Int | |
| `address` | String | Street, e.g. `"calle de Lagasca"`. |
| `province` / `municipality` / `district` / `neighborhood` | String | |
| `country` | String | `"es"`. |
| `latitude` / `longitude` | Double | **Flat** here; nested under `ubication` in detail. |
| `description` | String | Long Spanish text. |
| `features` | Object? | **Varying keys** — see below. |
| `parkingSpace` | Object? | Ad 2 only: `hasParkingSpace`, `isParkingSpaceIncludedInPrice`. |
| `multimedia.images[]` | Array | `url`, `tag` only. |

### `features` varies per ad

| Ad | Keys present |
|---|---|
| 1 | `hasAirConditioning`, `hasBoxRoom` |
| 2 | `hasAirConditioning`, `hasBoxRoom` |
| 3 | `hasAirConditioning`, `hasBoxRoom` |
| 4 | `hasAirConditioning`, `hasBoxRoom`, `hasSwimmingPool`, `hasTerrace`, `hasGarden` |

Every flag is modelled as nullable with a `false` default, and the parser runs with
`ignoreUnknownKeys = true` so a new key added upstream cannot crash the app.

---

## `detail.json`

A **single object** — and, per the upstream README, *"the response is always the same"*.

| Field | Type | Notes |
|---|---|---|
| `adid` | **Int** | `1`. Different name **and type** from `propertyCode`. |
| `price` | Double | |
| `priceInfo.amount` / `.currencySuffix` | Double / String | **One level shallower** than the list shape. |
| `operation`, `propertyType`, `extendedPropertyType`, `homeType`, `state` | String | |
| `multimedia.images[]` | Array | `url`, `tag`, **`localizedName`**, **`multimediaId`** — richer than list. |
| `propertyComment` | String | The detail-screen description (list calls it `description`). |
| `ubication.latitude` / `.longitude` | Double | **Nested** here. |
| `country` | String | |
| `moreCharacteristics` | Object | `communityCosts`, `roomNumber`, `bathNumber`, `exterior`, `housingFurnitures`, `agencyIsABank`, `energyCertificationType`, `flatLocation`, `modificationDate` (**epoch millis**), `constructedArea`, `lift`, `boxroom`, `isDuplex`, `floor`, `status`. |
| `energyCertification` | Object | `title`, `energyConsumption.type`, `emissions.type` (letter grades). |

---

## The three quirks that drive design

### 1. The detail endpoint ignores the id

Binding the response straight to the UI would show **ad 1's price, address and location for every
ad**. The repository therefore composes a detail model from two sources:

| Comes from the **cached list ad** | Comes from the **detail response** |
|---|---|
| `propertyCode`, address, district, neighborhood | `moreCharacteristics` (lift, community costs, …) |
| price + currency, operation, property type | `energyCertification` |
| thumbnail, coordinates | full image gallery (with `localizedName`) |
| | `propertyComment` |

`adid`, `priceInfo` and `ubication` from the detail payload are **discarded on purpose**. A
regression test asserts that opening ad 3 never renders ad 1's identity. See
[`DECISIONS/ADR-0005-detail-merge-strategy.md`](DECISIONS/ADR-0005-detail-merge-strategy.md).

### 2. The id and price shapes diverge

`String propertyCode` vs `Int adid`, and `priceInfo.price.amount` vs `priceInfo.amount`. The two DTO
hierarchies stay separate — no "shared" DTO — and both map into the same `:core:model` types.

### 3. Optional objects

`features` and `parkingSpace` are absent on some ads. Treat every amenity as
`Boolean? = null → false`, and never index into `multimedia.images` without a bounds check (an ad
with no images must render a placeholder, not crash).
