# AE2Objects Documentation

AE2Objects adds **Deep Storage Cells**
to [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2):
storage cells with **no type limit** and a large, simple byte-based capacity.

This documentation covers the planned expansion of the mod to support:

- **Vanilla AE2 fluids** (`AEFluidKey`)
- **MEGA Cells** tier sizes `1M`–`256M`
- **Applied Mekanistics** chemicals (`MekanismKeyType`)
- **All portable variants** of the above

## Documentation map

| Document                             | Purpose                                                                                                     |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------|
| [`architecture.md`](architecture.md) | Internal design: storage model, cell inventory, handler, data components, integration isolation, lifecycle. |
| [`content.md`](content.md)           | Item catalog: housings, storage cells, portable cells, ID scheme, upgrade support.                          |
| [`values.md`](values.md)             | Numeric reference: tiers, byte capacities, idle drain, key-type unit math.                                  |
| [`integrations.md`](integrations.md) | AE2 / MEGA Cells / Applied Mekanistics integration details and soft-dependency rules.                       |
| [`refactor-plan.md`](refactor-plan.md) | Refactor rationale, compatibility choices, and recommended expansion order.                              |
| [`implementation-plan.md`](implementation-plan.md) | Work items and key implementation details for the platform redesign (phases, migration map, tests). |

## Conventions used in these documents

- **Namespace** — all items/IDs belong to the `ae2objects` namespace unless a foreign mod is
  explicitly named (e.g. `ae2:`, `megacells:`, `appmek:`).
- **Tier notation** — `1k`, `4k`, `16k`, `64k`, `256k`, `1m`, `4m`, `16m`, `64m`, `256m`. Lower-case
  `k`/`m` is used in registry IDs; upper-case `K`/`M` may appear in prose.
- **"Deep" cell** — a storage cell that has no limit on the number of stored *types*; only the total
  byte capacity is bounded.
- **bytes** — the abstract capacity unit of an AE2 storage cell. How many items/fluids/chemicals a
  byte holds depends on the key type (see [`values.md`](values.md)).
- **Planned vs. shipped** — the base item deep cells (`1k`–`256k`) already exist. Fluid, chemical,
  MEGA-tier, and portable variants are documented here as the target design.

## Status of this documentation

Parts of this documentation describe the **target design** of the expansion. Where a feature is not
yet implemented, the text still documents the agreed specification (IDs, values, and behavior) so
implementation can follow it directly.
