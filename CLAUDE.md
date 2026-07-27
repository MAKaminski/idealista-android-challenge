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
| Research + planning + docs | ✅ delivered (this commit) |
| Gradle scaffold and app code | ⛔ not started |

Do not describe unwritten code as if it exists. `docs/DELIVERY_LOG.md` is the source of truth for
what has actually been built and verified.

## 3. Commands

The Android SDK is **not** installed in a fresh container. Bootstrap once:

```bash
# cmdline-tools + platform 36 + build-tools 36.0.0 (~2-3 GB)
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

Then (all commands from the repo root, via the wrapper — never a system `gradle`):

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # all JVM + Robolectric tests
./gradlew lint                   # Android lint
./gradlew :feature:list:test     # a single module's tests
```

There is **no emulator** in this environment. Espresso/instrumented tests are authored and wired
into CI, but they cannot be executed locally — say so rather than reporting them as passing.

## 4. Pinned toolchain

AGP 9.3.1 · Gradle 9.5 · Kotlin 2.3.10 · KSP 2.3.10 · Hilt 2.60.1 · Room 2.8.4
compileSdk 36 · targetSdk 36 · minSdk 24 (with core library desugaring) · JDK 17 toolchain

Constraints that are easy to break — check `docs/DECISIONS/` before changing any of these:

- **KSP publishes no 2.4.x**, so Kotlin cannot go above 2.3.10.
- **Hilt 2.59+ dropped AGP 8 support**; AGP 9 and Hilt 2.60.1 move together. Hilt 2.59 has a known
  `ComponentTreeDeps` failure on AGP 9 — do not downgrade to it.
- AGP 9 enables **built-in Kotlin**, which is **incompatible with KAPT**. Annotation processing is
  **KSP only**. Do not add `kapt` anywhere.
- AGP 9 disallows `getDefaultProguardFile("proguard-android.txt")` — use `proguard-android-optimize.txt`.
- AGP 9 makes the R class non-final: `R.id.x` cannot appear in a `when` branch. Use `if/else`.

All versions live in `gradle/libs.versions.toml`. Never hardcode a version in a module script.

## 5. Module map and dependency rules

```
:app                  Application, Hilt setup, MainActivity, nav graph
:core:model           pure-Kotlin domain models — no Android dependencies
:core:data            Retrofit + kotlinx.serialization, Room, DTOs, mappers, repositories
:core:designsystem    Material 3 theme, shared styles/drawables, Compose theme
:core:testing         test rules, fakes, JSON fixtures
:feature:list         XML list screen
:feature:detail       XML detail screen (+ one embedded ComposeView component)
:feature:favorites    Compose-only screen
```

Rules:

- `:feature:*` → `:core:data` → `:core:model`. Features never depend on each other.
- **DTOs never leave `:core:data`.** Map to `:core:model` types at the data-source boundary.
- `:core:model` stays free of Android imports so it tests on the JVM.

## 6. API quirks — do NOT "fix" these

The mock API is quirky by design. An agent that normalizes these away breaks the app. Full field
tables are in [`docs/API.md`](docs/API.md).

1. **`detail.json` always returns the same payload** (ad 1) no matter which ad was opened. The
   repository therefore **merges**: identity fields (id, address, district, price, operation,
   thumbnail) come from the cached list ad; only rich fields (characteristics, energy certificate,
   gallery, long comment) come from the detail response. The detail DTO's `adid`, `priceInfo` and
   `ubication` are discarded on purpose.
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
- Navigation Component + SafeArgs for screen-to-screen arguments; `SavedStateHandle` for state that
  must survive process death.
- All user-facing text in `strings.xml` (`values/` English, `values-es/` Spanish — the ad data is
  Spanish). No hardcoded strings in layouts or code.
- `contentDescription` on every meaningful image and toggle.
- Dates are stored as epoch millis and formatted with `java.time` +
  `DateTimeFormatter.ofLocalizedDate(MEDIUM)` in the system zone.

## 8. Testing requirements

Every behavioural change ships with a test. See [`docs/TESTING.md`](docs/TESTING.md) for the tiers.
Two regression tests are load-bearing and must never be deleted:

- favoriting an ad surfaces the same date on **both** the list and the detail screen;
- opening ad 3's detail never renders ad 1's identity (the merge-strategy guard).

## 9. Definition of done

1. `./gradlew lint testDebugUnitTest assembleDebug` passes.
2. New behaviour has a test; the two guards above still pass.
3. `docs/DELIVERY_LOG.md` has an entry with the command run and its **actual** result.
4. If an AI tool produced a non-trivial chunk of the change, `docs/AI_USAGE.md` records what was
   accepted, and what was rejected or corrected by hand.
