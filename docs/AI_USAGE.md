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

## Sessions 2+ — implementation

Not started. Each implementation session appends here: what was generated, what was hand-corrected,
and the actual command output backing the claim, mirrored in
[`DELIVERY_LOG.md`](DELIVERY_LOG.md).
