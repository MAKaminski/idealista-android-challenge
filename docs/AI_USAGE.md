# AI usage

The challenge asks candidates to use AI tools and to submit the context files they used. This is the
honest record of that: which tool, what it was asked, what it got right, and — more usefully — what
it got wrong and how that was caught.

**Tools:** Claude Code (model `claude-opus-5`), running headless in a Linux container with shell,
web access and a GitHub connection.
**Context files committed:** [`../CLAUDE.md`](../CLAUDE.md) (project rules for any AI tool),
plus this `docs/` package as shared background.

## Working method

The rule for this project: **AI proposes, the shell verifies.** Anything an AI asserts about the API
or the toolchain is checked against the actual endpoint or the actual Maven metadata before it is
written down as fact. Version numbers in particular are exactly the kind of thing a language model
gets confidently wrong, because its training data has a cutoff and Gradle plugins do not.

## Session 1 — research and planning (2026-07-27)

**Prompt:** *"checkout the repo, and perform the research required, then develop a plan. Be sure to
include a doc folder along with a Claude.md file — we want to be transparent about delivery."*

### What the AI did

1. Read `README.md`, `list.json`, `detail.json`; parsed both payloads programmatically to derive the
   real field sets rather than eyeballing them.
2. Diffed the two endpoints' schemas and found the three quirks now documented in [`API.md`](API.md).
3. Queried live Maven/Google metadata for every candidate dependency instead of recalling versions.
4. Fetched the AGP release notes and migration notes for the breaking changes in AGP 9.
5. Probed the container: JDK, Gradle, Android SDK presence, disk, and network reachability of
   `dl.google.com`, `repo1.maven.org`, `services.gradle.org` and the idealista image CDN.
6. Asked the four scope questions whose answers actually change the work (ambition, DI, Compose
   placement, toolchain currency) rather than assuming.
7. Wrote this documentation set.

### Corrections the verification step caught

| Model's initial assumption | What checking actually showed |
|---|---|
| AGP 8.x is current | **AGP 9.3.1** is current; AGP 9 changed several defaults (built-in Kotlin, non-final R class, ProGuard file rules) |
| Kotlin can be the newest release (2.4.10) | **KSP publishes nothing above 2.3.10**, so Kotlin is capped at 2.3.10 |
| Hilt is orthogonal to the AGP choice | **Hilt 2.59+ dropped AGP 8 support**, and 2.59 has a known `ComponentTreeDeps` failure on AGP 9 — the AGP and Hilt choices are coupled, and 2.60.1 is the only comfortable pairing |
| KAPT is available for Room/Hilt | AGP 9's built-in Kotlin is **incompatible with KAPT** — KSP only |
| The detail endpoint returns the ad you asked for | It **always returns ad 1**, with a different id type and a different price shape — which is what forced the merge strategy in ADR-0005 |

Five of the six assumptions a plausible-sounding first draft would have shipped were wrong. That is
the argument for the verify-then-write rule, not a footnote to it.

### Accepted / rejected

- **Accepted:** the module layout, the repository `combine` for favorites, the detail-merge strategy,
  the test tier list, and the version pins — each after the checks above.
- **Rejected:** a suggestion to normalise the two endpoints behind one shared DTO. The id types and
  price shapes genuinely differ; a shared DTO would have hidden the difference and made the merge bug
  easier to reintroduce.
- **Rejected:** claiming an emulator-based E2E tier. There is no KVM here, so those tests are marked
  authored-but-not-executed in [`TESTING.md`](TESTING.md) instead of being reported as passing.
- **Deferred to the user:** ambition level, Hilt vs manual DI, Compose placement, and how aggressive
  to be on toolchain currency. These are taste and risk calls, not technical facts.

## Session 2 — build scaffold (2026-07-27)

**Prompt:** *"Start on step 1"*

The AI wrote the eight-module scaffold, convention plugins and version catalog in one pass, then
spent four build cycles being corrected by the compiler:

| The AI wrote | The build said |
|---|---|
| `gradle wrapper --gradle-version 9.5` | there is no Gradle 9.5 — 9.5.0 and 9.6.1 exist |
| Kotlin/KSP pinned to 2.3.10 | AGP 9 manages Kotlin itself (2.2.10) and pins KSP `2.2.10-2.0.2` |
| `compileSdk = 36` | `core-ktx:1.19.0` requires 37+, and platforms now carry a minor version |
| standalone KSP with built-in Kotlin | "Using kotlin.sourceSets DSL is not allowed with built-in Kotlin" |
| a JDK 17 toolchain | no JDK 17 on the machine → added the foojay resolver so the build provisions it |

Every one of those was a plausible-looking line that a reviewer skimming the diff would have waved
through. None survived contact with `./gradlew`. This is the concrete case for the working method
above: the AI is fast at producing a *shape* that is right and *details* that are wrong, so the value
comes from running it, not from reading it.

**Hand-decided, not AI-suggested:** leaving the `disallowKotlinSourceSets` warning visible instead of
suppressing it (it's an honest signal that KSP hasn't finished its own AGP 9 migration), and amending
ADR-0001 with the corrections rather than silently re-pinning the versions.

## Sessions 3–6 — the app (2026-07-28)

Data layer, both XML screens, the Compose screen and the tests. The pattern from session 2 held all
the way through: the *shape* the AI proposed was consistently right, and the *details* were
consistently wrong until something ran them.

### What the compiler, lint and the tests caught

| Where | What was wrong |
|---|---|
| Coil 3.5.0 | Ships Kotlin 2.4 metadata; the AGP-managed 2.2.10 compiler cannot read it. Pinned to 3.4.0 |
| Robolectric | No API 37 runtime; every Robolectric test failed until pinned to `sdk=36` |
| Test dispatchers | A `TestDispatcher` built in `@Before` carries its own scheduler — six tests died on `DispatchException` |
| Turbine coordinates | `app.cash:turbine` is wrong; it is `app.cash.turbine:turbine` |
| `layout_marginHorizontal` | API 26+, against minSdk 24 — lint failed the build |
| `app_name` | Lint failed on a missing Spanish translation for a brand name; marked `translatable="false"` |
| Compose test selector | `onNodeWithText` cannot click an icon; it needed `onNodeWithContentDescription` |
| CI vs local | Robolectric's SDK 36 sandbox needs a Java 21 JVM — CI was red while local was green |

### The one that mattered

`retry re-subscribes after a failure` failed on its first run and exposed a genuine defect: `catch`
outside `flatMapLatest` completed the whole flow chain, so after any network error the retry button
was permanently dead. The screen would have looked entirely normal in review and in a manual
click-through. That test is the reason it isn't shipping.

### Judgment calls made by hand, not by the AI

- Leaving the `disallowKotlinSourceSets` warning **visible** instead of suppressing it — it is honest
  signal that the project sits ahead of KSP's own AGP 9 migration.
- Skipping SafeArgs rather than adding a plugin for one string argument.
- Keeping the Espresso test in the repository, compiled on every build, while stating plainly
  everywhere that it has never been executed. The alternatives — deleting it, or counting it as
  passing — are both worse.
- Amending ADR-0001 with the evidence each time a pin turned out to be wrong, rather than quietly
  editing the version numbers.

## Session 7 — three reports from the running app (2026-07-28)

Insets, external links and filters, prompted by a user with the app in front of them. Different
failure mode from the earlier sessions: nothing here was caught by a compiler. The reports were
*"fix the margin so it doesn't look like shit"*, *"if URLs are provided then embed links"*, and
*"add filters based on the tags +/- standard RE logic"*.

### What the AI got right without prompting

Diagnosing the margin as an edge-to-edge inset bug rather than a padding value, and fixing it at all
four surfaces that own a system bar rather than the one in the screenshot. A fixed `marginTop` would
have satisfied both screenshots and been wrong on the next device.

### What was corrected by hand

| Correction | Why |
|---|---|
| The AI offered filters over lift, floor and community costs | Those are detail-only fields, and the detail response always describes ad 1 (ADR-0005). Every ad would have matched identically. Cut before writing them. |
| Listing URLs presented without qualification | The mock codes are `1`–`4`; real ones are eight digits, so the links cannot resolve. The KDoc, the ADR and the README now say so instead of letting a reviewer click and find out. |
| `:core:model` tests were not running | The suite was reported as 86 tests, but `testDebugUnitTest` skips a variant-less JVM module — eighteen of them never ran. Found by checking the task graph rather than trusting the number. Fixed with an alias in the convention plugin. |
| The insets fix was nearly logged as covered | It has no test and cannot easily have one. `TESTING.md` now carries a row that says zero, not a row that omits it. |

### The pattern worth naming

Sessions 3–6 were wrong about facts a build could check. This session was wrong about facts only a
*payload* could check — filters that compile, run, and quietly match everything. The defence is the
same either way: run it and look, rather than reading the code and agreeing with it.

## Session 8 — settings and five languages (2026-07-28)

### What was corrected by hand

| Correction | Why |
|---|---|
| The AI's first design stored the language in a `SharedPreference` and wrapped `Context` | Correct for 2020, wrong since Android 13: the system owns per-app language and shows it in its own settings, so a private copy is a second source of truth that goes stale invisibly. Replaced with `AppCompatDelegate` and no storage at all (ADR-0009). |
| `LazyColumn` for six fixed rows | Broke four tests on rows below the fold, and — more importantly — broke `selectableGroup`, so a screen reader saw six unrelated radios rather than one group. The test failure was the symptom; the container was the bug. |
| Five `AppLocales` tests that could never pass | Written confidently, failed, and a probe showed why: Robolectric has no per-app locale store, so the call under test is a no-op there. Deleted rather than made to pass against a stub. |

### The check that was worth a build cycle

Lint's `MissingTranslation` was *assumed* to be guarding five locales across five modules. Rather
than assume, one string was deleted from `values-fr` and lint run: it failed with the exact missing
key, and went green again when restored. Five languages is precisely the surface that rots quietly,
and "lint probably covers it" is not evidence.

### Honest assessment of the AI's contribution

It wrote nearly all of the code and most of the prose, and it was fast at both. It was also wrong
about a specific, checkable fact roughly once per build cycle. The delivery is only trustworthy
because every claim in it was run before it was written down — which is the entire method, and the
reason `DELIVERY_LOG.md` records commands and their real output rather than intentions.
