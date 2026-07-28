# ADR-0010 — OpenStreetMap tiles via osmdroid, not Google Maps

**Status:** Accepted · 2026-07-28

## Context

Every ad in `list.json` carries `latitude` and `longitude`, and a map is the first thing anyone wants
when comparing properties. Until now those coordinates only powered a `geo:` link that handed the
user off to another app entirely.

The default choice is the Google Maps SDK. It is excellent, and it needs an API key. A key in a
public submission repository is a credential a reviewer would be right to flag; a key omitted from
the repository is a map that does not work on the reviewer's machine. Neither is acceptable.

## Decision

**osmdroid** with OpenStreetMap's standard (Mapnik) tiles. No key, no account, no build-time secret,
and a genuinely pannable and zoomable map rather than a static image.

| Piece | Where |
|---|---|
| `MapBounds` — the rectangle containing every located ad | `:core:model`, pure Kotlin, JVM-tested |
| `MapViewModel` — cache → pins, `Content` / `Empty` | `:feature:map` |
| `MapFragment` — tiles, markers, the selection card | `:feature:map` |

The map reads the **same Room cache the list does**, so it needs no request of its own and a favorite
toggled elsewhere is already true here. It refreshes on init so opening the map first, on a cold
start, still fills it.

Tapping a pin raises a card; tapping the card calls `onAdSelected`, which `MainActivity` routes to
the same detail screen the list opens. The map does not know the detail screen exists — the same rule
every other feature follows.

Ads with no coordinates are filtered out rather than defaulted. `(0, 0)` is in the Gulf of Guinea,
and one such pin would zoom the map out to include Africa. There is a test named after exactly that.

## Consequences

- The repository carries no secrets, and the map works for a reviewer who has cloned it.
- **Tiles come from the OSM foundation's public servers.** Their usage policy requires an
  identifying user agent, which `MapFragment` sets to the package name — the library's default would
  pool this app with every other osmdroid client and get rate-limited. Acceptable for a submission;
  a production app would pay for a tile host.
- osmdroid is a `View`, not a Compose surface, which suits a project whose mandatory screens are XML.
- The `MapView` holds tile threads and a bitmap cache, so the fragment pumps `onResume`/`onPause` and
  calls `onDetach` in `onDestroyView`. Skipping that leaks both.
- **Not screenshotted.** Tiles are fetched at runtime, so the offscreen renderer produces an empty
  grid. The README says so rather than shipping a picture of nothing.
