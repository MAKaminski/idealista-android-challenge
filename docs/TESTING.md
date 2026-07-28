# Testing

**37 automated tests, all passing.** This document says which tiers were actually executed and which
were not — the distinction is kept honest rather than implied.

```bash
./gradlew testDebugUnitTest        # every executable tier below (37 tests)
./gradlew :core:data:testDebugUnitTest --rerun-tasks   # one module, forced to re-run
./gradlew lint                     # Android lint
./gradlew connectedDebugAndroidTest   # Espresso — needs a device/emulator
```

Test names print on the console: Gradle stays silent for passing tests by default, which makes a
green run indistinguishable from a run that executed nothing.

## What runs, and where

| Tier | Tool | Module | Count | Executed? |
|---|---|---|---|---|
| API contract | MockWebServer + the committed fixtures | `:core:data` | 6 | ✅ |
| Mappers | JUnit | `:core:data` | 9 | ✅ |
| Repository (+ Room) | Room in-memory + Robolectric + Turbine | `:core:data` | 8 | ✅ |
| List ViewModel | coroutines-test + Turbine | `:feature:list` | 5 | ✅ |
| Detail ViewModel | coroutines-test + Turbine | `:feature:detail` | 5 | ✅ |
| Compose UI | `createComposeRule` under Robolectric | `:feature:favorites` | 4 | ✅ |
| End-to-end | Espresso + Hilt test runner | `:app` | 1 | ❌ **authored and compiling, not executed** |

The Espresso test is compiled on every build (`assembleDebugAndroidTest` runs in CI), so it cannot
rot — but running it needs a device, and neither the development container nor the CI job has one.
Claiming it as passing would be a lie; deleting it would lose a real artifact. It is marked in its
own KDoc as well as here.

## The load-bearing tests

Two properties of this app are not obvious and would break silently. Both are pinned at more than one
level, and neither test may be deleted:

**1. The favorited date is the same everywhere.**
`favoriting an ad surfaces the same date on the list and the detail` (`:core:data`) — favorites are
stored once and joined into a single `observeAds()`, so no two screens can disagree.

**2. The detail screen shows the ad you opened.**
`detail for ad 3 shows ad 3 identity not ad 1` (`:core:data`) and
`opening ad 3 shows ad 3 identity not ad 1` (`:feature:detail`) — the mock endpoint returns ad 1 for
every request, so identity comes from the cached ad (ADR-0005). Without these, the app looks correct
on ad 1 and lies on the other three.

Supporting guards worth keeping: `a refresh does not clear existing favorites`,
`a refresh failure with a populated cache still shows the ads`, and
`an unknown field added upstream does not break parsing`.

## Fixtures

`list.json` and `detail.json` at the repo root are the real upstream payloads. A Gradle `Sync` task
copies them into `:core:data`'s test resources, so there is exactly one copy in the repository and a
schema drift fails the contract tests rather than going unnoticed.

## Bugs these tests actually caught

Not hypothetical — each of these was found by a test failing, not by review:

- **The retry button was permanently dead after a network failure.** `catch` sat outside
  `flatMapLatest`, completing the whole flow chain. The UI looked normal.
  (`retry re-subscribes after a failure`)
- **Six repository tests failed on a `DispatchException`** because a `TestDispatcher` built in
  `@Before` carries its own scheduler, which cannot be mixed with `runTest`'s.
- **CI was red while local was green**: Robolectric's SDK 36 sandbox needs a Java 21 JVM.

## What is deliberately not tested

- Third-party behaviour (Retrofit's parsing, Coil's decoding) — covered at the boundary by the
  contract tests instead.
- Exact pixel layout. The Compose and fragment tests assert content and interaction, not geometry.
- The mock API's own data oddities (ads marked `rent` at sale-level prices). That is upstream mock
  data; the app renders it as given and `API.md` records it.
