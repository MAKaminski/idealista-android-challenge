# ADR-0004 — Room as the single source of truth

**Status:** Accepted · 2026-07-27

## Context

Favorites must survive app restarts, and each favorite carries the **date it was favorited** — so the
storage holds a value per key, not just a set of ids. The spec also lists persistent storage as a
bonus.

`DataStore<Preferences>` could store a favorites map with no code generation at all. It would be the
lighter dependency, but it stores loose key-value pairs, has no query surface, and gives nothing for
caching the ad list.

## Decision

Use **Room 2.8.4** (KSP) with two tables:

```sql
ads(property_code TEXT PRIMARY KEY, …, images TEXT)      -- offline cache of list.json
favorites(property_code TEXT PRIMARY KEY, favorited_at INTEGER NOT NULL)
```

Room is the **single source of truth**: the network writes only into the cache, and the UI reads only
from the cache. The repository exposes `combine(adDao.observeAll(), favoriteDao.observeAll())`, so a
favorite toggle re-emits to the list, the detail screen and the favorites screen at once.

`favorited_at` is epoch millis (a `TypeConverter` handles `Instant` at the boundary).

## Consequences

- Offline browsing and instant favorite feedback fall out of the design rather than being added.
- DAO tests run on the JVM with an in-memory database under Robolectric.
- Adds KSP codegen and a schema to keep (exported to `schemas/` for migration tests).
- Heavier than `DataStore` for four ads — accepted, because the cache is what makes one reactive
  source of truth possible.
