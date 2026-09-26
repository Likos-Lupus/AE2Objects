# Architecture

This document describes the refactored internal architecture of AE2Objects and the extension seams
reserved for the planned fluid / chemical / MEGA-tier / portable expansion.

## 1. Design goals

The architecture is intentionally centered on the **deep-cell domain**, rather than on technical
buckets such as `item`, `storage`, and `registry`.

Goals:

- one storage implementation for every AE key type;
- no type limit: capacity is based only on total native amount;
- a single tier model for the shipped `1k`–`256k` cells and planned `1m`–`256m` cells;
- normal and portable cells share the same inventory, persistence and cloning semantics;
- optional integrations never leak foreign classes into shared packages;
- registration, AE2 model wiring, upgrade wiring, creative-tab population and recipes consume a
  common registration catalog instead of parallel hard-coded lists;
- preserve existing registry IDs and existing world SavedData fields.

Non-goals:

- replacing AE2's storage framework;
- making MEGA Cells or Applied Mekanistics required dependencies;
- redesigning AE2's own partition/upgrades/menu behavior.

## 2. Package layout

| Package | Responsibility |
| --- | --- |
| `top.likoslupus.ae2objects` | NeoForge entry point only. |
| `...cell` | Deep-cell domain model: `CellTier`, `DeepCellSpec`, capacity math, item contract, stack metadata. |
| `...cell.item` | Concrete item forms. Currently `DeepStorageCellItem`; later `DeepPortableCellItem`. |
| `...cell.inventory` | AE2 runtime adapter: `DeepCellInventory`, `DeepCellHandler`, tooltip projection. |
| `...cell.persistence` | UUID storage repository, SavedData model and AE-key serialization boundary. |
| `...registry` | NeoForge registrations plus the `DeepCellRegistration` catalog. |
| `...integration.ae2` | Required AE2 binding: cell handler, drive models and upgrades. |
| `...integration.megacells` | Planned optional MEGA Cells content/source binding. |
| `...integration.appmek` | Planned optional chemical key type, validator and menu binding. |
| `...data` | Data generation. |
| `...command` | `/ae2objects` commands. |
| `...client` | Client-only bootstrap. |
| `...mixin` | Container-copy hook for independent UUID cloning. |

The important dependency direction is:

```text
bootstrap / registry / integrations
            │
            ▼
        cell domain
       /           \
 inventory       persistence
       \           /
            AE2 API
```

Optional integrations may construct domain objects, but the domain never imports optional-mod
classes.

## 3. Domain model

### 3.1 `CellTier`

`CellTier` is the single source of truth for tier IDs, byte capacity and idle drain. It already
contains all ten documented tiers:

```text
1k, 4k, 16k, 64k, 256k, 1m, 4m, 16m, 64m, 256m
```

Only the first five are registered as content today. MEGA integration can opt into the remaining
five without changing inventory code or adding another capacity table.

### 3.2 `DeepCellSpec`

A `DeepCellSpec` defines storage semantics independently of the concrete item form:

- tier;
- `AEKeyType`;
- whether fuzzy partitioning is supported;
- an extra `Predicate<AEKey>` content validator.

The validator is the isolation seam for Applied Mekanistics. The future appmek integration can build
this predicate using `MekanismKey` and `ChemicalAttributeValidator` inside
`integration.appmek`; shared code only sees a `Predicate<AEKey>`.

### 3.3 `DeepCellCapacity`

All byte/native-unit conversions live in `DeepCellCapacity`:

```text
totalAmount     = totalBytes * amountPerByte
usedBytes       = storedAmount / amountPerByte
freeBytes       = totalBytes - usedBytes
remainingAmount = totalAmount - storedAmount
```

`remainingAmount`, not `freeBytes`, is used to clamp insertion. This matters for fluid/chemical key
types where one byte represents 1000 native units.

### 3.4 `DeepCellItem`

`DeepCellItem` is the common capability contract for normal and portable cells. It exposes a
`DeepCellSpec`, derives key type/bytes/idle drain from that spec, centralizes the generic blacklist
rules, and owns independent-storage cloning semantics.

A future portable implementation can therefore be:

```java
final class DeepPortableCellItem extends AbstractPortableCell implements DeepCellItem {
    // portable menu / energy behavior only
}
```

It does not need a second storage implementation.

## 4. Runtime inventory

`DeepCellInventory` implements AE2 `StorageCell` and is deliberately restricted to runtime storage
behavior:

- partition / inverter / fuzzy filtering;
- insert and extract;
- cell status;
- lazy content loading;
- triggering persistence after mutation.

It does **not** own SavedData codecs, tooltip formatting or registration.

### 4.1 Key-type-aware capacity

Insertion first checks `DeepCellSpec.accepts(key)`, then partition rules and nested-cell rules. The
accepted amount is:

```text
min(requestedAmount, capacity.remainingAmount(storedAmount))
```

The same path works for item, fluid and chemical keys. Extraction is fully `long`-based; there is no
`Integer.MAX_VALUE` clamp.

### 4.2 Client preview

The authoritative inventory exists only on the server. The ItemStack synchronizes:

- cell UUID;
- total stored native amount;
- stored type count;
- AE2's short `STORAGE_CELL_INV` preview.

Client-only inventory instances read this projection for status and tooltips.

## 5. Persistence

### 5.1 UUID-addressed contents

Deep-cell contents remain external to the ItemStack:

```text
ItemStack
  └─ cell_id UUID
       │
       ▼
DeepStorageManager (SavedData)
  └─ Map<UUID, DeepCellStorage>
       │
       ├─ serialized AE keys
       ├─ parallel long amounts
       ├─ total stored amount
       └─ optional original cell item ID
```

`DeepStorageManager` is now a repository-style API. Callers can find, create, replace and remove
records but no longer receive its mutable backing map.

### 5.2 `DeepCellStorage`

`DeepCellStorage` is an immutable record from the caller's perspective. Mutable `ListTag` and
`long[]` values are defensively copied on input and output.

The legacy persisted payload is intentionally unchanged:

```text
keys
amts
item_count
```

`item_count` now means **stored native amount**. The old name is retained only for save
compatibility.

A new optional codec field, `cell_item`, stores the original registered item ID. Old records decode
without it.

### 5.3 `DeepCellStorageIo`

AE key serialization requires registry context, so it lives in a dedicated boundary instead of the
inventory or SavedData record. Loading also normalizes malformed records (mismatched key/amount
lists, invalid keys, wrong key types) on the next save.

### 5.4 Server access bridge

AE2's cell-handler API does not provide a level/server parameter. `DeepStorageAccess` is therefore a
small lifecycle-owned bridge to the current server's `DeepStorageManager`. It is populated on
`ServerStartedEvent` and cleared on `ServerStoppedEvent`; global state does not leak into the domain
model beyond this boundary.

## 6. Stack metadata

`DeepCellStackData` is the only shared helper that knows the ItemStack data-component contract.

| Registry ID | Java meaning | Type |
| --- | --- | --- |
| `ae2objects:cell_id` | storage identity | UUID |
| `ae2objects:cell_item_count` | stored native amount (legacy ID retained) | long |
| `ae2objects:cell_type_count` | stored distinct key count | int |
| `ae2objects:fuzzy_mode` | item-cell fuzzy mode | `FuzzyMode` |

This avoids spreading component-name semantics through inventory, commands, cloning and UI code.

## 7. Registration catalog

The five shipped item cells are no longer five independent blocks of registry/model/upgrade code.
They are generated from:

- the five AE2 `CellTier`s;
- the corresponding AE2 core components;
- one item-family spec.

Every registered normal cell produces a `DeepCellRegistration` descriptor. The same descriptors are
consumed by:

- creative-tab population;
- AE2 drive model registration;
- AE2 upgrade registration;
- recipe generation.

Adding a normal fluid family therefore does not require another set of switch statements or static
lists.

Core/housing references are lazy suppliers. This is important for future optional integrations:
MEGA component lookup can be isolated and deferred instead of forcing foreign classes to load while
the shared registry class initializes.

## 8. Recovery and cloning

### 8.1 Independent cloning

The `DeepCellCopyMixin` container-copy hook calls `DeepCellItem.copyWithIndependentStorage`:

1. copy the ItemStack;
2. on the authoritative server, allocate a fresh UUID;
3. reuse the immutable storage snapshot under the new UUID;
4. keep the synchronized summary/preview on the new stack.

On the client, no unbacked UUID is invented; the server remains authoritative.

### 8.2 Recovery

New/visited SavedData records remember their original `cell_item` registry ID. `/ae2objects recover`
uses that ID and verifies that the resolved item implements `DeepCellItem`.

Legacy orphan records have no `cell_item`; for compatibility they fall back to the historical 256k
item cell behavior. Once such a recovered/used cell is persisted again, its type metadata becomes
explicit.

This is required before multiple key types and portable forms exist; a UUID alone cannot otherwise
identify which cell item should be reconstructed.

## 9. Optional integration boundaries

### 9.1 AE2 fluids

No foreign dependency is required. Create specs with:

```java
DeepCellSpec.fluids(tier)
```

Fluid specs use `AEKeyType.fluids()` with the deep-cell ratio `amountPerByte = 1000` and disable fuzzy behavior.

### 9.2 MEGA Cells

Planned `integration.megacells` responsibilities:

- resolve `1m`–`256m` core components;
- register the extra normal/portable items using `CellTier.megaTiers()`;
- add mod-loaded conditions to recipes.

The core inventory and persistence packages do not change.

### 9.3 Applied Mekanistics

Planned `integration.appmek` responsibilities:

- obtain `MekanismKeyType.TYPE`;
- create a chemical `DeepCellSpec` with `supportsFuzzy = false`;
- supply the chemical validator predicate;
- bind the chemical portable menu;
- register content only from the guarded integration bootstrap.

No shared class imports `MekanismKey`, `MekanismKeyType` or Mekanism APIs.

## 10. Portable cells

Portable cells should reuse these existing pieces unchanged:

- `CellTier`;
- `DeepCellSpec`;
- `DeepCellItem`;
- `DeepCellInventory` / `DeepCellHandler`;
- `DeepStorageManager` / `DeepCellStorageIo`;
- `DeepCellStackData`;
- clone/recovery metadata.

The portable class only adds form-specific concerns: menu type, battery, charge rate, energy-card
handling and portable disassembly output.

## 11. Dependency rule

The intended dependency rule for future work is simple:

> A new key type or tier may add a spec, registration and integration binding; it must not add a new
> inventory, persistence manager or copy of the capacity algorithm.
