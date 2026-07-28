# Testing

**122 automated tests, all passing.** This document says which tiers were actually executed and which
were not — the distinction is kept honest rather than implied.

```bash
./gradlew testDebugUnitTest        # every executable tier below (122 tests)
./gradlew screenshots              # regenerate docs/screenshots — opt-in, writes into docs/
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
| **Filter, link, language and map-bounds logic** | plain JUnit, no Android runtime | `:core:model` | 38 | ✅ |
| **Image alignment** | JUnit against the real `list.json` | `:feature:list` | 6 | ✅ |
| Design system (accordion) | Robolectric | `:core:designsystem` | 6 | ✅ |
| Design system (external links) | Robolectric + shadow intents | `:core:designsystem` | 3 | ✅ |
| List ViewModel | coroutines-test + Turbine | `:feature:list` | 9 | ✅ |
| Filter chip row | Robolectric | `:feature:list` | 8 | ✅ |
| Detail ViewModel (incl. translation) | coroutines-test + Turbine | `:feature:detail` | 10 | ✅ |
| Map ViewModel | coroutines-test + Turbine | `:feature:map` | 5 | ✅ |
| Compose UI | `createComposeRule` under Robolectric | `:feature:favorites` | 4 | ✅ |
| Language picker | `createComposeRule` under Robolectric | `:feature:settings` | 7 | ✅ |
| End-to-end | Espresso + Hilt test runner | `:app` | 1 | ❌ **authored and compiling, not executed** |
| Window insets | — | — | 0 | ❌ **not covered — see below** |
| Applying a locale | — | — | 0 | ❌ **not testable — see below** |
| ML Kit translation itself | — | — | 0 | ❌ **needs a device — see below** |
| Screenshot rendering | Robolectric native graphics | 5 modules | 6 | ⚙️ opt-in, not part of the suite |

`MlKitAdTextTranslator` is not unit-testable either: ML Kit downloads its models from Google's
servers onto a real device, and there is none here. What *is* tested is everything around it — the
detail ViewModel shows content while a translation is pending, falls back to the Spanish original
when one cannot be produced, and asks for nothing at all when the app follows the system language.
The ML Kit class is written and unrun, and this table says zero rather than omitting the row.

The screenshot tests are **generators, not assertions**: they render the real screens to PNGs and
write them into `docs/`. They are skipped unless `./gradlew screenshots` sets the opt-in flag, so an
ordinary test run neither rewrites committed images nor reaches a CDN. They still exercise the real
layouts and adapters, so a screen that stops inflating breaks them.

`AppLocales.apply` is not unit-testable and the tests for it were **deleted rather than weakened**.
On the API 33+ path `AppCompatDelegate.setApplicationLocales` forwards to the framework's
`LocaleManager`, and Robolectric's sandbox has no per-app locale store — a probe confirmed the call
is a no-op there and reads back empty. A test that passed would have been testing a stub. What is
covered instead is the tag logic either side of it (`:core:model`) and the picker's behaviour
(`:feature:settings`); the two-line delegate call between them is verified by running the app.

Translation completeness *is* enforced: lint's `MissingTranslation` fails the build for any string
absent from any of the five locales. Verified by deleting one and watching it go red, not assumed.

Window insets are the other behaviour changed in this repo with no automated cover. They need a real
window with real system bars; Robolectric does not dispatch them meaningfully and a Compose test
harness reports zero insets. The fix is asserted by inspection against the two screenshots that
reported it. A reviewer on a cutout display should re-check it by eye.

The Espresso test is compiled on every build (`assembleDebugAndroidTest` runs in CI), so it cannot
rot — but running it needs a device, and neither the development container nor the CI job has one.
Claiming it as passing would be a lie; deleting it would lose a real artifact. It is marked in its
own KDoc as well as here.

`:core:model` is a plain JVM module with no build variants, so `testDebugUnitTest` would skip it
entirely. Its convention plugin registers that name as an alias for `test` — otherwise the one
command the docs and CI run would quietly under-report itself by eighteen tests.

## The load-bearing tests

Four properties of this app are not obvious and would break silently. Each is pinned at more than one
level, and none of these tests may be deleted:

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

**4. A filter chip can never show a state the ViewModel does not hold.**
`bindFilters` rebuilds the whole row from `AdFilters` on every emission rather than mutating chips in
place, and `rebinding replaces the row rather than appending to it` plus
`tapping an active chip reports the transform that turns it off` (`:feature:list`) pin both halves.
The transforms themselves are pure and covered in `:core:model`, so the UI test only has to prove the
wiring.

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
