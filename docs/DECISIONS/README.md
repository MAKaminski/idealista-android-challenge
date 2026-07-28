# Architecture Decision Records

Short records of the choices that a reviewer might otherwise have to reverse-engineer from the diff.
Format: context → decision → consequences, including the costs we accepted.

| ADR | Decision | Status |
|---|---|---|
| [0001](ADR-0001-toolchain.md) | AGP 9.3.1 + Gradle 9.5 + Kotlin 2.3.10 + KSP, validated empirically | Accepted |
| [0002](ADR-0002-modularization.md) | Multi-module by layer and feature | Accepted |
| [0003](ADR-0003-hilt.md) | Hilt for dependency injection | Accepted |
| [0004](ADR-0004-room.md) | Room as the single source of truth | Accepted |
| [0005](ADR-0005-detail-merge-strategy.md) | Merge cached list identity with the detail payload | Accepted |
| [0006](ADR-0006-compose-interop.md) | Compose in two places, XML for the mandatory screens | Accepted |
| [0007](ADR-0007-minsdk-desugaring.md) | minSdk 24 with core library desugaring for `java.time` | Accepted |
| [0008](ADR-0008-filters-and-external-links.md) | Client-side filters scoped to the list payload; Custom Tabs for external links | Accepted |
