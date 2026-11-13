# AE2Objects Documentation

AE2Objects adds **Deep Storage Cells**
to [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2):
storage cells with **no type limit** and a large, simple byte-based capacity.

The mod's catalog model covers the full product matrix; the currently registered subset is:

- **Items** and **vanilla AE2 fluids** (`AEFluidKey`), drive **and portable**, `1k`–`256k`
- MEGA Cells tier sizes `1M`–`256M` defined in the catalog, disabled until a MEGA Cells 26.x build
- **Applied Mekanistics** chemicals as a ready-but-inactive seam (no 26.x appmek build yet)

## Documentation map

| Document                                           | Purpose                                                                                                                                                            |
|----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`architecture.md`](architecture.md)               | Internal design: domain model, channel abstraction, storage engine, persistence, client projection, item layer, registration, integrations, lifecycle, game tests. |
| [`content.md`](content.md)                         | Item catalog: housings, storage cells, portable cells, ID scheme, upgrade support, behavior.                                                                       |
| [`values.md`](values.md)                           | Numeric reference: tiers, byte capacities, idle drain, key-type unit math, portable values.                                                                        |
| [`integrations.md`](integrations.md)               | AE2 / MEGA Cells / Applied Mekanistics integration details and soft-dependency rules.                                                                              |
| [`refactor-plan.md`](refactor-plan.md)             | Refactor rationale, compatibility choices, boundary rules and deferred work.                                                                                       |
| [`implementation-plan.md`](implementation-plan.md) | Historical work items and key implementation details for the platform redesign (phases, layout, tests).                                                            |

## Conventions used in these documents

- **Namespace** — all items/IDs belong to the `ae2objects` namespace unless a foreign mod is
  explicitly named (e.g. `ae2:`, `megacells:`, `appmek:`).
- **Tier notation** — `1k`, `4k`, `16k`, `64k`, `256k`, `1m`, `4m`, `16m`, `64m`, `256m`. Lower-case
  `k`/`m` is used in registry IDs; upper-case `K`/`M` may appear in prose.
- **"Deep" cell** — a storage cell with no limit on the number of stored *types*; only the total
  byte capacity is bounded.
- **bytes** — the abstract capacity unit of an AE2 storage cell. How many items/fluids/chemicals a
  byte holds depends on the key type (see [`values.md`](values.md)).
- **Engine frozen** — after the platform redesign the storage engine must not change for new
  content; see [`refactor-plan.md`](refactor-plan.md).

## Status

- **Shipped:** item + fluid drive cells (`1k`–`256k`), portable item + fluid cells, housings,
  GameTests, on Minecraft 26.1.2 / 26.2 / 26.3.
- **Ready-but-inactive:** chemical cells. Applied Mekanistics has no 26.x build yet, so the chemical
  channel/housing/models are placeholders (dormant) and no chemical recipes are emitted.
- **Catalog-only:** MEGA-tier `1m`–`256m` cells are defined but not registered or craftable until
  MEGA Cells ships a 26.x build.
