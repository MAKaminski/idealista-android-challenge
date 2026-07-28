# CLAUDE.md

Context file for AI coding tools (Claude Code, Copilot, Cursor) working in this repository.
It is committed deliberately: the challenge asks candidates to show how they leverage AI tooling.

---

## 1. What this project is

An Android app for the **idealista Android challenge**: browse a list of property ads, open an ad's
detail screen, and favorite ads — showing the date each ad was favorited.

The upstream spec lives in [`README.md`](README.md). Its hard requirements are non-negotiable:

- **Kotlin**, and the two mandatory screens use **XML views** (not Compose).
- At least two screens: **list** and **detail**.
- **Favorite** an ad, and display the **date it was favorited**.

Everything else — modularization, Compose, Room, CI — is a bonus we opted into. Never trade a hard
requirement for a bonus.

## 2. Status

| Phase | State |
|---|---|
| Steps 1–9 — build, data, both XML screens, Compose, tests, CI, docs | ✅ **complete** |
| Beyond the brief — accordions, filters, links, insets, six languages, map, translation | ✅ **complete** |
| Verification | `lint testDebugUnitTest assembleDebug` green · **122 tests** |

The app is feature-complete against the brief. Two things are not covered by an executed test: the
Espresso end-to-end test (authored and compiling, needs a device) and the window-insets fix (needs a
real window). Applying a locale is a third: Robolectric's sandbox has no per-app locale store, so
that call cannot be unit-tested at all. All three are stated as such in `docs/TESTING.md` rather
than counted as passing.

Do not describe unwritten code as if it exists. `docs/DELIVERY_LOG.md` is the source of truth for
what has actually been built and verified.

## 3. Commands

The Android SDK is **not** installed in a fresh container. Bootstrap once:

```bash
# cmdline-tools, then:
sdkmanager "platform-tools" "platforms;android-37.1" "build-tools;37.0.0"
echo "sdk.dir=$HOME/Android/sdk" > local.properties
```

Then (all commands from the repo root, via the wrapper — never a system `gradle`):

```bash
./gradlew runDebug               # build, install and LAUNCH on a device/emulator
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # all JVM + Robolectric tests
./gradlew lint                   # Android lint
./gradlew :feature:list:test     # a single module's tests
```

There is **no emulator** in this environment. Espresso/instrumented tests are authored and wired
into CI, but they cannot be executed locally — say so rather than reporting them as passing.

## 4. Pinned toolchain

AGP 9.3.1 · Gradle 9.6.1 · Kotlin 2.2.10 (**AGP's built-in Kotlin**) · KSP 2.2.10-2.0.2 ·
Hilt 2.60.1 · Room 2.8.4 · Compose BOM 2026.06.01 · Coil **3.4.0** (3.5.0 ships Kotlin 2.4 metadata
this compiler cannot read) · Robolectric pinned to `sdk=36`
compileSdk 37 + compileSdkMinor 1 · targetSdk 37 · minSdk 24 (core library desugaring) · JDK 17
toolchain (auto-provisioned by the foojay resolver)

Constraints that are easy to break — all of them cost a build cycle to discover, so check
`docs/DECISIONS/ADR-0001-toolchain.md` before changing any:

- **Kotlin is managed by AGP**, not by us. AGP 9's built-in Kotlin is 2.2.10 and AGP's POM pins the
  matching KSP `2.2.10-2.0.2`. The catalog's `kotlin` entry exists only so `:core:model` and
  `build-logic` use the same compiler. Raising Kotlin means raising AGP.
- AGP 9's built-in Kotlin is **incompatible with KAPT**. Annotation processing is **KSP only** —
  never add `kapt`.
- Standalone KSP registers generated dirs via `kotlin.sourceSets`, which built-in Kotlin forbids;
  `android.disallowKotlinSourceSets=false` in `gradle.properties` is what makes Room and Hilt work.
  Its "experimental option" warning is left visible on purpose — don't suppress it.
- **Hilt 2.59+ dropped AGP 8 support**; AGP 9 and Hilt 2.60.1 move together. Hilt 2.59 has a known
  `ComponentTreeDeps` failure on AGP 9 — do not downgrade to it.
- `androidx.core:core-ktx:1.19.0` requires compileSdk 37+, and platforms are minor-versioned now:
  `compileSdk = 37` **plus** `compileSdkMinor = 1`.
- AGP 9 disallows `getDefaultProguardFile("proguard-android.txt")` — use `proguard-android-optimize.txt`.
- AGP 9 makes the R class non-final: `R.id.x` cannot appear in a `when` branch. Use `if/else`.
- The Gradle `wrapper` task cannot run while AGP 9 is on the root classpath (the system Gradle can't
  load it). To regenerate it, move `build.gradle.kts`/`settings.gradle.kts` aside first.

All versions live in `gradle/libs.versions.toml`. Never hardcode a version in a module script — SDK
levels live in `build-logic`'s `Sdk` object and module scripts only set their `namespace`.

## 5. Module map and dependency rules

```
:app                  Application, Hilt setup, MainActivity, bottom nav, screen wiring
:core:model           pure-Kotlin domain models, filter logic, URL builders — no Android deps
:core:data            Retrofit + kotlinx.serialization, Room, DTOs, mappers, repositories
:core:designsystem    Material 3 theme + matching Compose theme, drawables, shared formatters,
                      AccordionSection, ExternalLinks
:core:testing         shared test fixtures (TestAds)
:feature:list         XML list screen
:feature:detail       XML detail screen (+ an embedded ComposeView characteristics panel)
:feature:favorites    Compose-only screen
:feature:map          XML map screen — osmdroid over OpenStreetMap tiles
:feature:settings     Compose-only settings screen — the language picker
```

Rules:

- `:feature:*` → `:core:data` → `:core:model`. Features never depend on each other.
- **DTOs never leave `:core:data`.** Map to `:core:model` types at the data-source boundary.
- `:core:model` stays free of Android imports so it tests on the JVM. It is a plain JVM module, so
  its convention plugin aliases `testDebugUnitTest` to `test` — otherwise the one command CI runs
  would skip it silently.

## 6. API quirks — do NOT "fix" these

The mock API is quirky by design. An agent that normalizes these away breaks the app. Full field
tables are in [`docs/API.md`](docs/API.md).

1. **`detail.json` always returns the same payload** (ad 1) no matter which ad was opened. The
   repository therefore **merges**: identity — id, address, district, price, operation, thumbnail
   **and the photo gallery** — comes from the cached list ad; only characteristics, the energy
   certificate and the long comment come from the detail response. The detail DTO's `adid`,
   `priceInfo`, `ubication` and `multimedia` are discarded on purpose.
   **Photos are identity.** Each list ad carries its own `multimedia.images`; the response's images
   are ad 1's rooms. Taking them from the response shipped the wrong flat's photos on three of four
   ads — see ADR-0005 and the alignment tests before touching this.
2. The id is a **`String` (`propertyCode`) in list** and an **`Int` (`adid`) in detail**; the price
   is `priceInfo.price.amount` in list but `priceInfo.amount` in detail. The two DTO hierarchies are
   separate — do not extract a "shared" DTO.
3. `features` keys **vary per ad** (`hasSwimmingPool`, `hasTerrace` and `hasGarden` appear on ad 4
   only) and `parkingSpace` appears on ad 2 only. Every such field is nullable with a safe default,
   and the JSON parser runs with `ignoreUnknownKeys = true`.

## 7. Conventions

- ViewBinding in every Fragment; no `findViewById`, no synthetics.
- One `StateFlow<UiState>` per ViewModel, sealed states (`Loading`/`Content`/`Empty`/`Error`),
  collected with `repeatOnLifecycle`. No `LiveData` in new code.
- Coroutines only; inject dispatchers, never hardcode `Dispatchers.IO` in a class under test.
- Screen-to-screen wiring lives in `MainActivity`, not inside a feature — a feature exposes a
  callback (`onAdSelected`) and the host decides what to open, so features stay independent.
  Arguments travel as fragment arguments and are read through `SavedStateHandle`, which also gets
  process-death survival. SafeArgs is deliberately not used (see the delivery log for step 5).
- All user-facing text in `strings.xml`, in **six** locales: `values/` (en), `values-es/`,
  `values-fr/`, `values-pt/`, `values-it/`, `values-zh/`. No hardcoded strings in layouts or code. A string added
  to one locale and not the rest fails `lint` on `MissingTranslation` — translate all five, and add
  the new module's folders too if you create one.
- The in-app language is the **platform's** per-app locale via `AppLocales`/`AppCompatDelegate`.
  Never add a SharedPreference for it: on API 33+ the system stores the choice and shows it in its
  own settings, and a private copy is a second source of truth that goes stale (ADR-0009).
- `contentDescription` on every meaningful image and toggle.
- Dates are stored as epoch millis and formatted with `java.time` +
  `DateTimeFormatter.ofLocalizedDate(MEDIUM)` in the system zone.
- The app draws **edge-to-edge**. Anything that owns a system bar consumes its inset:
  `fitsSystemWindows` on an `AppBarLayout`, `windowInsetsPadding` in Compose, an
  `OnApplyWindowInsetsListener` for the bottom nav. Never fix an overlap with a fixed margin — it is
  right on one device and wrong on every other.
- Filtering is **client-side over the Room cache** and lives as pure functions in `:core:model`
  (`AdFilters`, `Ad.matches`, `applyFilters`). Only offer a filter the **list** payload can answer —
  detail-only fields describe ad 1 for every ad (see §6), so a filter over them is a lie. The chip
  row is rebuilt from state on every emission; do not mutate chips in place.
- Ad **content** (descriptions, comments) arrives in Spanish whatever the UI language is. It is
  translated on-device through `AdTextTranslator`, downstream of the content emission so the screen
  never waits, and every failure path falls back to the original (ADR-0011). Never block a render on
  a translation, and never show a translation without saying it is one.
- The map is osmdroid over OpenStreetMap — **no API key anywhere in this repo**. Ads without
  coordinates are filtered out, never defaulted to `(0, 0)` (ADR-0010).
- Screenshots are generated, not captured: `./gradlew screenshots` renders the real screens offscreen
  into `docs/screenshots`. Never hand-edit those PNGs.
- External destinations go through `ExternalLinks` (Custom Tabs, `ACTION_VIEW` fallback, Toast when
  nothing handles it). Never `startActivity` a URL directly from a fragment.

## 8. Testing requirements

Every behavioural change ships with a test. See [`docs/TESTING.md`](docs/TESTING.md) for the tiers.
Two regression tests are load-bearing and must never be deleted:

- favoriting an ad surfaces the same date on **both** the list and the detail screen;
- opening ad 3's detail never renders ad 1's identity (the merge-strategy guard) — pinned twice, in
  `:core:data` and in `:feature:detail`;
- every photo shown belongs to the ad it appears under — the detail gallery comes from the **cached
  ad**, never from the response, whose images are always ad 1's.

## 9. Definition of done

1. `./gradlew lint testDebugUnitTest assembleDebug` passes.
2. New behaviour has a test; the two guards above still pass.
3. `docs/DELIVERY_LOG.md` has an entry with the command run and its **actual** result.
4. If an AI tool produced a non-trivial chunk of the change, `docs/AI_USAGE.md` records what was
   accepted, and what was rejected or corrected by hand.
