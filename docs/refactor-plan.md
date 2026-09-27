# Refactor Plan and Migration Notes

This document records the *Deep Cell Platform* refactor that made AE2Objects able to carry the full
product matrix, and the compatibility choices that keep existing worlds working. The resulting
architecture is described in [`architecture.md`](architecture.md).

## Why the old shape did not scale

The original code was correct for a single family of five item cells, but its variation points were
implicit:

- tier values were constructor literals in five registry entries;
- the inventory assumed **one stored item == one byte**;
- `DeepCellInventory` mixed filtering, mutation, AE-key NBT codecs, SavedData writes, client preview
  generation and tooltip state;
- drive models, upgrades, creative-tab entries and recipes each kept their own cell list;
- the recovery command always recreated a `256k` item cell;
- package boundaries separated technical categories (item/inventory/persistence) rather than the
  deep-cell domain.

A first refactor introduced `DeepCellSpec`, which mixed the three independent axes
(`StorageType × Tier × Runtime API`) into one object, so every tier recreated storage semantics.

## What the refactor produced

The refactor split the axes and froze the engine:

```text
CellDefinition = CellContentType × CellTier × CellForm   (pure data)
StorageChannelBinding: CellContentType -> AEKeyType        (runtime adapter)
CellForm adapter:      Drive | Portable                    (interaction/UI/power)
```

Delivered in phases, each a signed commit:

| Phase | Commit    | Summary                                                                                                                                                                                                                 |
|-------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0     | `6e8e983` | Checkpoint the pre-existing `cell/` module; fix fastutil usage.                                                                                                                                                         |
| 0b    | `f4c49c6` | Characterization tests for capacity + persistence.                                                                                                                                                                      |
| 1     | `6f7f283` | `cell/model`: `CellContentType`, `CellTier`, `CellForm`, `CellDefinition`, `DeepCellCatalog`, `CellCapacity`, `CellUpgradeProfile`.                                                                                     |
| 2     | `b0a6cf2` | `cell/channel` + Item/Fluid bindings; delete `DeepCellSpec` (definition holds no `AEKeyType`).                                                                                                                          |
| 3     | `5b11f6f` | `cell/persistence` + `cell/storage` split: contents/session/record/repository/codec/context; legacy decode + `cell_item`.                                                                                               |
| 4     | `b9458b9` | Rewrite `DeepCellInventory` as a pure `StorageCell`; extract `DeepCellFilter`, `NestedCellPolicy`, `DeepCellInventoryFactory`. **Engine frozen.**                                                                       |
| 5     | `f0b90ab` | `cell/stack` + `cell/item` + `cell/ui`: client snapshot, workbench support, drive item, tooltip/tint; delete the giant interface.                                                                                       |
| 6     | `6e8b031` | Catalog-driven registration (`ModItems`, `RegisteredCells`, `RegisteredHousings`, `CellRegistrationPlan`, `CellComponentSources`); rename `Ae2ObjectsItems`→`ModItems`, `Ae2ObjectsDataComponents`→`ModDataComponents`. |
| 7     | `f1888ff` | Fluid drive cells `1k`–`256k` + `deep_fluid_cell_housing`; texture tooling.                                                                                                                                             |
| 8     | `77c11cb` | Portable item + fluid cells; half capacity, flat 1 AE/t drain, energy capability, void card; portable IDs drop "storage".                                                                                               |
| 9     | `652b8c9` | MEGA `1m`–`256m` tiers (always registered, recipes gated on `megacells`) + Applied Mekanistics chemical skeleton.                                                                                                       |
| 10    | `fb4299a` | NeoForge GameTests (dedicated source set, world-level cases).                                                                                                                                                           |

The hard invariant: after **Phase 4** the storage engine (`DeepCellInventory`, `DeepCellSession`,
`DeepCellContents`, `DeepCellFilter`, `NestedCellPolicy`, `CellContentsCodec`) is **frozen**. Later
phases added content without touching it.

## Compatibility decisions

The refactor intentionally preserves:

- registry IDs `ae2objects:deep_item_storage_cell_{1k,4k,16k,64k,256k}` and
  `ae2objects:deep_item_cell_housing`;
- SavedData identity `ae2objects:storage_manager`;
- data-component registry IDs `ae2objects:cell_id`, `ae2objects:cell_item_count`,
  `ae2objects:cell_type_count`, `ae2objects:fuzzy_mode`;
- persisted NBT fields `keys`, `amts`, `item_count`, plus the new optional `cell_item`;
- the AE2 client preview component `ae2:storage_cell_inv` (≤10 entries, amount descending).

`cell_item_count` and persisted `item_count` are now semantically the stored **native amount** (not
just items), so they work for fluids and chemicals without a data migration.

## Boundary rules that must keep holding

- `CellDefinition` references no `AEKeyType`; the core/domain references no appmek/Mekanism.
- `DeepCellInventory` contains no `ITEM`/`FLUID`/`CHEMICAL`/`DRIVE`/`PORTABLE` branches and no
  concrete item dependency.
- Tooltip/tint never construct a `DeepCellInventory`; the client never reaches
  `SavedDataCellRepository`.
- All concrete cell identity is expressed by `CellDefinition`; the 60 target cells are all
  expressible by `DeepCellCatalog`.
- One implementation each for UUID lifecycle, capacity and the AE-key codec.

## Deferred work (tracked, not blocking)

- **Applied Mekanistics chemistry** — no 26.1 build exists yet, so only the `AppMekIntegration` seam
  is present (placeholder resources; no chemical recipes). When appmek ships 26.1, register the
  chemical `StorageChannelBinding` + portable menu, enforce the radioactive attribute check, emit
  recipes and replace placeholder models. See [`integrations.md`](integrations.md).
- **MEGA Cells tier art/recipes** — MEGA-tier deep cells are id-only placeholders (recipes are
  condition-gated raw JSON, models reuse the `256k` art) until MEGA ships a 26.1 build.
- **In-game verification** with each optional mod once those builds exist.
