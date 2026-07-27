# Testing strategy

> Status: **planned**. No tests exist yet — `DELIVERY_LOG.md` records what has actually been run.
> This document distinguishes what *will run in CI and locally* from what *cannot run here*, and
> that distinction is kept honest as the code lands.

## Tiers

| Tier | Tooling | Representative tests | Runs headless? |
|---|---|---|---|
| Mapper / unit | JUnit4 | `listDto_withoutOptionalFeatures_mapsToAdWithNoAmenities`<br>`priceFormatter_appendsCurrencySuffix_forSpanishLocale`<br>`favoritedAt_formatsAsLocalizedMediumDate` | ✅ |
| API contract | MockWebServer + the committed fixtures | `listEndpoint_parsesAllFourAds`<br>`detailEndpoint_parsesCharacteristicsAndEnergyCertificate`<br>`unknownJsonField_doesNotBreakParsing` | ✅ |
| Repository | fakes + `kotlinx-coroutines-test` + Turbine | `togglingFavorite_emitsUpdatedDate_toListAndDetail`<br>`detailForAd3_neverShowsAd1Identity`<br>`refreshFailure_keepsCachedAds` | ✅ |
| DAO | Room in-memory + Robolectric | `favoriteInsert_emitsOnObservingFlow`<br>`deleteFavorite_removesDateFromListProjection` | ✅ |
| ViewModel | coroutines-test + Turbine | `refreshFailure_emitsErrorState_thenRetrySucceeds`<br>`stateSurvivesProcessDeath_viaSavedStateHandle` | ✅ |
| Fragment UI | Robolectric + `FragmentScenario` | `listFragment_rendersFourAds_andTogglesFavorite`<br>`detailFragment_showsGalleryAndEnergyBadge` | ✅ |
| Compose UI | `createComposeRule` (Robolectric-backed) | `favoritesScreen_showsFavoritedDate`<br>`favoritesScreen_emptyState_whenNoFavorites` | ✅ |
| End-to-end | Espresso + Hilt test runner | `tapFirstAd_opensDetail_favorite_appearsInFavoritesScreen` | ❌ **authored, not executed** — no emulator/KVM in this environment |

## Two load-bearing regression tests

These encode the app's two least obvious correctness properties. They must never be deleted:

1. **`togglingFavorite_emitsUpdatedDate_toListAndDetail`** — favorite state and its date come from a
   single `combine` in the repository, so both screens can never disagree.
2. **`detailForAd3_neverShowsAd1Identity`** — the mock detail endpoint always returns ad 1. This test
   is the guard on the merge strategy described in
   [`DECISIONS/ADR-0005-detail-merge-strategy.md`](DECISIONS/ADR-0005-detail-merge-strategy.md).

## Fixtures

`list.json` and `detail.json` at the repo root are the *real* payloads, verified against the live
endpoints. They are wired in as test resources rather than copied and hand-trimmed, so if the upstream
schema drifts the contract tests fail instead of the app failing in the user's hands.

## Running

```bash
./gradlew testDebugUnitTest        # every tier marked ✅ above
./gradlew :core:data:test          # one module
./gradlew lint                     # Android lint
./gradlew jacocoTestReport         # coverage → build/reports/jacoco/
./gradlew connectedDebugAndroidTest   # Espresso — needs a device/emulator (not available here)
```

## What we deliberately do not test

- Third-party behaviour (Retrofit's parsing, Coil's decoding) — covered by contract tests at the
  boundary instead.
- Exact pixel layout — the Robolectric fragment tests assert content and interaction, not geometry.
- The mock API's data itself (prices marked `rent` at sale-level amounts, for instance). That's
  upstream mock data; the app renders it as given and `API.md` records the oddity.
