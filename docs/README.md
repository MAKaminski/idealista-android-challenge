# Documentation

Delivery documentation for the idealista Android challenge. Written for a reviewer who wants to
check the work, not just read about it.

| Document | What it answers |
|---|---|
| [`PLAN.md`](PLAN.md) | What we're building, in what order, and how each step is verified |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Module layout, layering rules, data flow, screen composition |
| [`API.md`](API.md) | The two endpoints, every field, and the three quirks that shape the design |
| [`TESTING.md`](TESTING.md) | Test tiers, what runs headless, what doesn't, how to run them |
| [`DECISIONS/`](DECISIONS/) | ADRs — each significant choice with its trade-off |
| [`AI_USAGE.md`](AI_USAGE.md) | Which AI tools were used, what they got wrong, how it was caught |
| [`DELIVERY_LOG.md`](DELIVERY_LOG.md) | What has actually been delivered and verified, with commands |
| [`../CLAUDE.md`](../CLAUDE.md) | Project context for AI tools — the committed "AI context file" |

## Current state

Research, planning and documentation are done. **No application code exists yet.**
[`DELIVERY_LOG.md`](DELIVERY_LOG.md) is the source of truth for that — if something is described in
the present tense elsewhere but is missing from the delivery log, it hasn't been built.

## Reading order

1. [`API.md`](API.md) — the mock API is quirkier than it looks, and it drives the design
2. [`ARCHITECTURE.md`](ARCHITECTURE.md) — how those quirks are contained
3. [`PLAN.md`](PLAN.md) — the delivery sequence
4. [`DECISIONS/ADR-0005-detail-merge-strategy.md`](DECISIONS/ADR-0005-detail-merge-strategy.md) — the
   one decision most likely to be questioned in review
