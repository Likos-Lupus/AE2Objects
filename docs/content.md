# Content

Complete catalog of AE2Objects content. All IDs are in the `ae2objects` namespace unless a foreign
mod is named explicitly.

Tier placeholders used below: `1k`, `4k`, `16k`, `64k`, `256k`, `1m`, `4m`, `16m`, `64m`, `256m`.

- **Shipped so far:** the item deep cells `1k`–`256k` and `deep_item_cell_housing`.
- **Planned:** all fluid / chemical / MEGA-tier / portable variants listed here.

## 1. Housings

Housings are the craftable "shell". A cell is made from a housing plus a storage component (and, for
portable cells, an energy cell and chest).

| Item ID                      | Description                          |
|------------------------------|--------------------------------------|
| `deep_item_cell_housing`     | ME Deep Item Cell Housing (existing) |
| `deep_fluid_cell_housing`    | ME Deep Fluid Cell Housing           |
| `deep_chemical_cell_housing` | ME Deep Chemical Cell Housing        |

## 2. Non-portable storage cells

These go into ME Drives and ME Chests. 30 cells total (3 key types × 10 tiers).

### 2.1 Item deep cells

Bounded by byte capacity, with **no type limit**. Accepts `AEKeyType.items()`.

| ID                            | Tier |
|-------------------------------|------|
| `deep_item_storage_cell_1k`   | 1k   |
| `deep_item_storage_cell_4k`   | 4k   |
| `deep_item_storage_cell_16k`  | 16k  |
| `deep_item_storage_cell_64k`  | 64k  |
| `deep_item_storage_cell_256k` | 256k |
| `deep_item_storage_cell_1m`   | 1m   |
| `deep_item_storage_cell_4m`   | 4m   |
| `deep_item_storage_cell_16m`  | 16m  |
| `deep_item_storage_cell_64m`  | 64m  |
| `deep_item_storage_cell_256m` | 256m |

### 2.2 Fluid deep cells

Accepts `AEKeyType.fluids()` (`AEFluidKey`). No type limit.

| ID                             | Tier |
|--------------------------------|------|
| `deep_fluid_storage_cell_1k`   | 1k   |
| `deep_fluid_storage_cell_4k`   | 4k   |
| `deep_fluid_storage_cell_16k`  | 16k  |
| `deep_fluid_storage_cell_64k`  | 64k  |
| `deep_fluid_storage_cell_256k` | 256k |
| `deep_fluid_storage_cell_1m`   | 1m   |
| `deep_fluid_storage_cell_4m`   | 4m   |
| `deep_fluid_storage_cell_16m`  | 16m  |
| `deep_fluid_storage_cell_64m`  | 64m  |
| `deep_fluid_storage_cell_256m` | 256m |

### 2.3 Chemical deep cells

Accepts `me.ramidzkh.mekae2.ae2.MekanismKeyType.TYPE` (**optional** — only active with Applied
Mekanistics). No type limit. Invalid chemicals (per `ChemicalAttributeValidator.DEFAULT`) are
rejected on insert.

| ID                                | Tier |
|-----------------------------------|------|
| `deep_chemical_storage_cell_1k`   | 1k   |
| `deep_chemical_storage_cell_4k`   | 4k   |
| `deep_chemical_storage_cell_16k`  | 16k  |
| `deep_chemical_storage_cell_64k`  | 64k  |
| `deep_chemical_storage_cell_256k` | 256k |
| `deep_chemical_storage_cell_1m`   | 1m   |
| `deep_chemical_storage_cell_4m`   | 4m   |
| `deep_chemical_storage_cell_16m`  | 16m  |
| `deep_chemical_storage_cell_64m`  | 64m  |
| `deep_chemical_storage_cell_256m` | 256m |

## 3. Portable storage cells

Portable cells act like a pocket ME Chest and can be charged. They use the ID pattern
`deep_portable_<type>_cell_<tier>`. 30 cells total.

| Type     | ID pattern                                   |
|----------|----------------------------------------------|
| Item     | `deep_portable_item_cell_<tier>`     |
| Fluid    | `deep_portable_fluid_cell_<tier>`    |
| Chemical | `deep_portable_chemical_cell_<tier>` |

Each family contains the same ten tiers `1k … 256m`, e.g.:

```
deep_portable_item_cell_1k      … deep_portable_item_cell_256m
deep_portable_fluid_cell_1k     … deep_portable_fluid_cell_256m
deep_portable_chemical_cell_1k  … deep_portable_chemical_cell_256m
```

Portable cells store **half** of their tier's byte capacity, mirroring vanilla AE2 portable cells,
and have a flat idle drain of **1 AE/t** (drive cells instead scale their drain with the tier). They
still have **no type limit**.

## 4. ID / naming scheme

```
deep_<type>_storage_cell_<tier>            non-portable
deep_portable_<type>_cell_<tier>   portable
deep_<type>_cell_housing                   housing
```

- `<type>` ∈ `item`, `fluid`, `chemical`
- `<tier>` ∈ `1k`, `4k`, `16k`, `64k`, `256k`, `1m`, `4m`, `16m`, `64m`, `256m`

## 5. Creative tab

All content is added to AE2's main creative tab (`AECreativeTabIds.MAIN`):

1. `deep_item_cell_housing`
2. `deep_fluid_cell_housing`
3. `deep_chemical_cell_housing`
4. All non-portable deep cells (30)
5. All portable deep cells (30)

## 6. Upgrade support matrix

Upgrades are added through AE2's `Upgrades.add(...)`. "Max" is the number of upgrade slots.

| Cell family             | Fuzzy Card | Inverter Card | Overflow Card | Energy Card   | Max slots |
|-------------------------|------------|---------------|---------------|---------------|-----------|
| Item (non-portable)     | yes (1)    | yes (1)       | yes (1)       | —             | 3         |
| Fluid (non-portable)    | **no**     | yes (1)       | yes (1)       | —             | 2         |
| Chemical (non-portable) | **no**     | yes (1)       | yes (1)       | —             | 2         |
| Portable item           | yes (1)    | yes (1)       | yes (1)       | yes (up to 4) | 4         |
| Portable fluid          | **no**     | yes (1)       | yes (1)       | yes (up to 4) | 3         |
| Portable chemical       | **no**     | yes (1)       | yes (1)       | yes (up to 4) | 3         |

Rationale: AE2's own fluid cells do not accept the Fuzzy Card, so deep fluid/chemical cells follow
suit. Because deep cells have **no type limit**, the Equal Distribution Card does not apply (AE2
divides capacity by the type count), but the Overflow Destruction Card still does — overflow beyond
the byte capacity is destroyed.

## 7. Behavior

### 7.1 Tooltip

- Byte usage line: `bytesUsed(usedBytes, totalBytes)`.
- Type line: current type count vs. `∞` (no type limit).
- Partition/fuzzy line when the cell is pre-formatted (item cells only).
- Content preview: up to 10 stacks, sorted by amount descending.

### 7.2 Disassembly

Shift-right-click with the cell in hand while it is empty disassembles it, returning:

- the core storage component,
- the housing,
- any installed upgrade cards.

Portable cells consume their stored energy safely and return the energy cell with any remaining
charge transferred into it.

### 7.3 Cloning

Copying a deep cell (container-menu copy) creates an independent cell: a fresh UUID and a deep copy
of the stored contents.

### 7.4 Commands

| Command                      | Permission  | Description                                             |
|------------------------------|-------------|---------------------------------------------------------|
| `/ae2objects getuuid`        | all players | Show/copy the UUID of the held deep cell.               |
| `/ae2objects recover <UUID>` | gamemasters | Spawn a deep cell bound to an existing UUID (recovery). |
