# Testing

**53 automated tests, all passing.** This document says which tiers were actually executed and which
were not — the distinction is kept honest rather than implied.

```bash
./gradlew testDebugUnitTest        # every executable tier below (53 tests)
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
| Mappers | JUnit | `:core:data` | 11 | ✅ |
| Repository (+ Room) | Room in-memory + Robolectric + Turbine | `:core:data` | 10 | ✅ |
| **Image alignment** | JUnit against the real `list.json` | `:feature:list` | 6 | ✅ |
| Design system (accordion) | Robolectric | `:core:designsystem` | 6 | ✅ |
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

**3. Every picture belongs to the ad it is shown under.**
`every card uses its own ad's thumbnail` and `no two cards share an image url` (`:feature:list`),
`the detail gallery shows the opened ad's photos not ad 1's` and
`every gallery photo url belongs to the cached ad` (`:core:data`). These exist because the first
implementation took the detail gallery from the response, putting ad 1's rooms on ads 2–4 — a bug
that no test caught and a person spotted by looking at the screen. The suite now asserts alignment
against the real payloads, including that the four fixtures have genuinely distinct photos so the
assertions can fail.

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
- **Ads 2–4 displayed ad 1's photos.** Found by a human looking at the app, not by the suite — the
  gap is now closed by the alignment tier above, and the lesson is recorded in ADR-0005.

## What is deliberately not tested

- Third-party behaviour (Retrofit's parsing, Coil's decoding) — covered at the boundary by the
  contract tests instead.
- Exact pixel layout. The Compose and fragment tests assert content and interaction, not geometry.
- The mock API's own data oddities (ads marked `rent` at sale-level prices). That is upstream mock
  data; the app renders it as given and `API.md` records it.
