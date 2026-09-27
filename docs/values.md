# Values

Numeric reference for AE2Objects deep cells: tier definitions, byte capacities per key type, idle
drain, and the key-type unit math used by the cell inventory.

## 1. Tier definitions

Each tier is defined by a display prefix, a total byte capacity, and an idle power drain (AE/t).

| Tier | Prefix |       Bytes | Idle drain (AE/t) |
|------|--------|------------:|------------------:|
| 1k   | `1k`   |       1,000 |               0.5 |
| 4k   | `4k`   |       4,000 |               1.0 |
| 16k  | `16k`  |      16,000 |               1.5 |
| 64k  | `64k`  |      64,000 |               2.0 |
| 256k | `256k` |     256,000 |               2.5 |
| 1m   | `1m`   |   1,000,000 |               3.0 |
| 4m   | `4m`   |   4,000,000 |               3.5 |
| 16m  | `16m`  |  16,000,000 |               4.0 |
| 64m  | `64m`  |  64,000,000 |               4.5 |
| 256m | `256m` | 256,000,000 |               5.0 |

> **Decimal tiers by design.** The MEGA-tier values are decimal (`1M = 1,000,000`), matching the
> existing `1k`–`256k` deep cells (`bytes = kilobytes × 1000`). This is intentionally simpler than
> MEGA Cells' binary tiers (`1M = 1,048,576`) — see the appendix.

## 2. Capacity per key type

Storage capacity in native units. There is **no type limit**; only the total byte capacity counts.

Rules:

- **Items** — `1 byte = 1 item` (`amountPerByte = 1`).
- **Fluids** — `1 byte = 1000 mB = 1 bucket` (`amountPerByte = 1000`).
- **Chemicals** — `1 byte = 1000 units` (`amountPerByte = 1000`).

| Tier |       Bytes |    Item cap |  Fluid cap (mB) | Fluid cap (B) |    Chemical cap |
|------|------------:|------------:|----------------:|--------------:|----------------:|
| 1k   |       1,000 |       1,000 |       1,000,000 |         1,000 |       1,000,000 |
| 4k   |       4,000 |       4,000 |       4,000,000 |         4,000 |       4,000,000 |
| 16k  |      16,000 |      16,000 |      16,000,000 |        16,000 |      16,000,000 |
| 64k  |      64,000 |      64,000 |      64,000,000 |        64,000 |      64,000,000 |
| 256k |     256,000 |     256,000 |     256,000,000 |       256,000 |     256,000,000 |
| 1m   |   1,000,000 |   1,000,000 |   1,000,000,000 |     1,000,000 |   1,000,000,000 |
| 4m   |   4,000,000 |   4,000,000 |   4,000,000,000 |     4,000,000 |   4,000,000,000 |
| 16m  |  16,000,000 |  16,000,000 |  16,000,000,000 |    16,000,000 |  16,000,000,000 |
| 64m  |  64,000,000 |  64,000,000 |  64,000,000,000 |    64,000,000 |  64,000,000,000 |
| 256m | 256,000,000 | 256,000,000 | 256,000,000,000 |   256,000,000 | 256,000,000,000 |

## 3. Key-type unit math

The inventory converts between stored **units** (what the AE network moves) and **bytes** (the
capacity accounting unit) using the deep-cell family's explicit `amountPerByte`. This is
intentionally not inherited from AE2's basic-cell `AEKeyType#getAmountPerByte()` ratio.

| Key type               | `amountPerByte` | Unit symbol         | 1 byte stores |
|------------------------|----------------:|---------------------|---------------|
| `AEKeyType.items()`    |               1 | (item)              | 1 item        |
| `AEKeyType.fluids()`   |            1000 | `B` (mB internally) | 1000 mB       |
| `MekanismKeyType.TYPE` |            1000 | chemical unit       | 1000 units    |

Formulas implemented by `CellCapacity`:

```
amountPerByte    = definition.type().amountPerByte()
usedBytes        = storedAmount / amountPerByte
freeBytes        = bytes - usedBytes
remainingAmount  = bytes * amountPerByte - storedAmount
```

Insertion is clamped to `remainingAmount`; there is no per-type byte overhead.

## 4. Numeric limits

- Stored amounts and `remainingAmount` are `long`. The largest value is the `256m` fluid/chemical
  cell: `256,000,000 bytes × 1000 = 256,000,000,000` units (~2.56 × 10¹¹), well within `long`.
- `getBytes()` returns an `int`. The largest tier, `256m`, is `256,000,000`, within the `int` range
  (~2.147 × 10⁹). Byte arithmetic internally uses `long`.
- The legacy `cell_item_count` data component and persisted `item_count` field store the native
  `storedAmount` as `long`.

## 5. Portable cell values

Portable deep cells store **half** the tier's byte capacity (like vanilla AE2 portable cells) and
have a fixed idle drain of **1 AE/t**. Additional runtime values:

| Property          | Value                                          |
|-------------------|------------------------------------------------|
| Charge rate       | `80 + 80 × energyCardMultiplier` AE/t          |
| Battery           | `AEConfig.instance().getPortableCellBattery()` |
| Energy Card slots | up to 4                                        |
| Default color     | `0x80Caff` (dyeable)                           |

## 6. Appendix — comparison with AE2 / MEGA Cells

| System                     |       1k-equivalent | Item cap (1 type) | Uses per-type overhead   | Type limit |
|----------------------------|--------------------:|------------------:|--------------------------|------------|
| AE2 item cell (1k)         |         1,024 bytes |             8,128 | yes (`bytesPerType = 8`) | 63         |
| AE2 fluid cell (1k)        |         1,024 bytes |     8,000 buckets | yes                      | 5–18       |
| MEGA Cells (`1M`)          |     1,048,576 bytes |        ~2 M items | yes                      | 63         |
| **AE2Objects deep (`1k`)** |     **1,000 bytes** |         **1,000** | **no**                   | **∞**      |
| **AE2Objects deep (`1m`)** | **1,000,000 bytes** |     **1,000,000** | **no**                   | **∞**      |

Notes:

- AE2's `getAmountPerByte()` is `8` for items and `8000` for fluids; AE2Objects deliberately uses a
  flat `1 byte = 1 unit` model for simplicity.
- MEGA Cells tiers are binary (`bytes = 1024 × 4^(index-1)`); AE2Objects MEGA-tier deep cells are
  decimal (`bytes = 1,000,000 × 4^(index-6)`). Both are "1M" in name only and are not expected to
  match numerically.
- MEGA Cells and Applied Mekanistics are both optional integrations; see
  [`integrations.md`](integrations.md).
