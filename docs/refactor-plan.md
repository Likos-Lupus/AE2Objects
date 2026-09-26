# Refactor Plan and Migration Notes

This document records the architectural refactor performed before implementing the expansion in
`content.md`, `values.md` and `integrations.md`.

## Why the old shape did not scale

The original code was correct for one family of five item cells, but most future variation points
were implicit:

- tier values were constructor literals in five registry entries;
- the inventory assumed one stored item == one byte;
- `DeepCellInventory` mixed filtering, mutation, AE-key NBT codecs, SavedData writes, client preview
  generation and tooltip-facing state;
- drive models, upgrades, creative tab entries and recipes maintained separate cell lists;
- the recovery command always recreated a 256k item cell;
- package boundaries separated technical class categories rather than the deep-cell domain.

That would turn the documented `3 key types × 10 tiers × normal/portable` matrix into repeated code
and condition-heavy special cases.

## Implemented refactor

1. **Domain model introduced** — `CellTier`, `DeepCellSpec`, and `DeepCellCapacity` make tier
   values, key type and native-unit math explicit.
2. **Feature-oriented packages** — all deep-cell behavior now lives below `cell`, split only at real
   boundaries (`item`, `inventory`, `persistence`).
3. **Inventory reduced to an adapter** — AE-key serialization moved to `DeepCellStorageIo`; tooltip
   rendering moved to `DeepCellTooltip`; stack component access moved to `DeepCellStackData`.
4. **Immutable persistence snapshots** — `DeepCellStorage` defensively copies mutable NBT/array
   values; `DeepStorageManager` no longer exposes its mutable map.
5. **Data-driven shipped content** — the five current cells are generated from the tier list and
   produce a shared `DeepCellRegistration` catalog consumed by models, upgrades, creative tabs and
   recipes.
6. **Future-safe recovery** — SavedData records can carry the source cell registry ID while still
   decoding legacy records.
7. **Long-safe storage operations** — insertion is clamped in native units and extraction no longer
   truncates at `Integer.MAX_VALUE`.
8. **Optional-integration seam** — validators are functions inside `DeepCellSpec`; optional API
   types can remain inside their integration packages.

## Compatibility decisions

The refactor intentionally preserves:

- all existing item registry IDs;
- `ae2objects:storage_manager` SavedData identity;
- legacy persisted `keys`, `amts`, and `item_count` fields;
- existing data-component registry IDs, including `cell_item_count`;
- existing recipe IDs and resource paths for the five shipped cells.

`cell_item_count` and persisted `item_count` are now treated semantically as **stored native
amount** so they also work for fluids and chemicals. Renaming those registry/save keys would require
a data migration and provides no runtime benefit.

## Expansion order after this refactor

The recommended implementation sequence is:

1. add vanilla AE2 fluid housing + five `1k`–`256k` normal fluid cells using `DeepCellSpec.fluids`;
2. add portable item/fluid cell form reusing `DeepCellItem` and `DeepCellInventory`;
3. add the optional-integration bootstrap and MEGA `1m`–`256m` tier registrations;
4. add Applied Mekanistics chemical specs/validator/menu entirely under `integration.appmek`;
5. generate the complete recipe/resource/lang matrix and add conditional recipes for optional
   component sources;
6. add GameTests covering drive insertion, partition cards, cloning, recovery and optional-mod
   absence in addition to the unit tests for capacity/persistence.

At each step the storage engine should remain unchanged. If a feature requires copying
`DeepCellInventory` or adding a key-type `if/else` inside persistence, that is a signal that the
integration boundary is being crossed in the wrong direction.
