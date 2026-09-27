# AE2Objects Platform Redesign — Implementation Plan

This document records the **work items** and the **important implementation details** for rebuilding
AE2Objects as a *Deep Cell Platform* that can carry the full product matrix:

```text
Item / Fluid / Chemical  ×  1k .. 256m  ×  Drive / Portable
```

It is the execution companion to the architecture redesign. It supersedes the architecture sections
of [`architecture.md`](architecture.md) and [`refactor-plan.md`](refactor-plan.md); those documents
are rewritten in the final phase. [`content.md`](content.md), [`values.md`](values.md) and
[`integrations.md`](integrations.md) remain the product/behaviour specification and are not changed
by this plan.

---

## 1. Goal and core idea

The previous refactor introduced `DeepCellSpec`, which mixes three independent axes into one object:

```text
Storage Type  (Item / Fluid / Chemical)
Tier          (1k .. 256m)
Runtime API   (AEKeyType, validator, ...)
```

Every tier therefore had to recreate the storage semantics of a type. The redesign separates the
axes:

```text
CellDefinition = CellContentType × CellTier × CellForm      (pure data)
StorageChannelBinding: CellContentType -> AEKeyType          (runtime adapter)
CellForm adapter:      Drive | Portable                      (interaction/UI/power)
```

Consequences:

- `CellDefinition` never references `AEKeyType`, `Item`, `DeferredItem`, `MenuType`, `SavedData`,
  `Predicate<AEKey>`, or any foreign mod class.
- Adding a Storage Type, Tier, or Form must not modify the storage engine.

The hard invariant of this plan:

> After Phase 4 the storage engine is **frozen**. Later phases that need to change
> `DeepCellInventory`, `DeepCellSession`, `DeepCellContents`, `DeepCellFilter`,
> `NestedCellPolicy` or `CellContentsCodec` indicate a boundary was drawn in the wrong place.

---

## 2. Locked decisions

1. **Checkpoint first.** The currently uncommitted `cell/` refactor (the `DeepCellSpec` version) is
   verified (build + tests) and committed as a checkpoint before any transformation.
2. **Applied Mekanistics is deferred.** Mekanism / Applied Mekanistics has no build for Minecraft
   26.1.2 yet, so Chemical **cannot be compiled this round**:
    - no foreign dependencies are added;
    - no foreign imports exist outside a future `integration/appmek`;
    - Chemical stays fully described in the product model but is **inactive** at runtime. When
      appmek ships for 26.1.2, only `integration/appmek` plus build metadata change.
3. **GameTests are in scope** for this round (NeoForge GameTest harness + world-level cases).
4. **Phased commits**; the final target is the complete Item + Fluid matrix across all ten tiers and
   both forms, with Chemical ready-but-inactive.
5. **Docs are updated**: `architecture.md` and `refactor-plan.md` are rewritten to this design and
   the docs map is updated.

---

## 3. Target package layout

```text
top.likoslupus.ae2objects
├─ Ae2Objects.java
├─ cell/
│  ├─ model/        CellContentType, CellTier, CellForm, CellDefinition, DeepCellCatalog,
│  │                CellCapacity, CellUpgradeProfile
│  ├─ channel/      StorageChannelBinding, StorageChannelRegistry, PortableMenuBinding
│  ├─ storage/      DeepCellInventory, DeepCellInventoryFactory, DeepCellContents,
│  │                DeepCellSession, DeepCellFilter, NestedCellPolicy
│  ├─ persistence/  CellRecord, CellRepository, SavedDataCellRepository, CellContentsCodec
│  ├─ stack/        CellStackData, CellStackSnapshot, CellView
│  ├─ item/         DeepCellDefinitionProvider, DeepDriveCellItem, DeepPortableCellItem,
│  │                CellWorkbenchSupport, CellDisassemblyService
│  └─ ui/           DeepCellTooltip
├─ content/         ModItems, ModDataComponents, RegisteredCells, RegisteredHousings,
│                   CellRegistrationPlan, CellComponentSources
├─ integration/
│  ├─ ae2/          Ae2Bootstrap, ItemChannelBinding, FluidChannelBinding, PortableMenuRegistry,
│  │                Ae2CellHandlerRegistration, Ae2UpgradeRegistration, Ae2DriveModelRegistration
│  ├─ appmek/       AppMekBootstrap (no-op seam this round)
│  └─ megacells/    MegaCellsRecipeSources
├─ platform/        IntegrationSet, ServerCellContext
├─ command/         Ae2ObjectsCommand, CellRecoveryService, CellIdentityService
├─ data/            Ae2ObjectsDataGenerator, CellRecipeProvider, CellModelProvider
└─ mixin/           DeepCellCopyMixin
```

### Dependency rule (hard code-review constraint)

| Package              | May depend on                                                                                                  | Must not depend on                                             |
|----------------------|----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `cell.model`         | JDK, `Identifier`                                                                                              | any AE/NeoForge/foreign type, `Item`, registry, `SavedData`    |
| `cell.channel`       | `cell.model`, AE `AEKey` API                                                                                   | item classes, `SavedData`, NeoForge registration, foreign mods |
| `cell.storage`       | `cell.model`, `cell.channel`, `cell.persistence` abstractions, `cell.stack` abstractions, AE `StorageCell` API | foreign mods, concrete item classes, datagen, commands         |
| `cell.persistence`   | AE key codecs, Minecraft `SavedData`/NBT                                                                       | item classes, foreign mods                                     |
| `cell.item`          | `model`, `stack`, workbench support, AE item APIs                                                              | `SavedData` maps, AE key codecs                                |
| `integration.appmek` | core + AppMek + Mekanism (future)                                                                              | nothing depends back on it                                     |

---

## 4. Domain model (Phase 1)

```java
public enum CellContentType {
    ITEM("item", 1L, true),
    FLUID("fluid", 1_000L, false),
    CHEMICAL("chemical", 1_000L, false)
}

public enum CellTier {
    K1("1k", 1_000, 0.5), …

    M256("256m",256_000_000,5.0);
}

public enum CellForm {
    DRIVE,
    PORTABLE
}

public record CellDefinition(
        CellContentType type,
        CellTier tier,
        CellForm form
) {

    public Identifier itemId();        // deep_<type>_storage_cell_<tier>

    // deep_portable_<type>_cell_<tier>
    public Identifier driveModelId();  // block/drive/cells/<itemId>  (DRIVE only)

    public String translationKey();    // text.ae2objects.deep_<type>_storage_cells

}

public record CellCapacity(
        long bytes,
        long amountPerByte
) {

    public long totalAmount();            // Math.multiplyExact(bytes, amountPerByte)

    public long remainingAmount(long storedAmount);

    public long usedBytes(long storedAmount); // storedAmount / amountPerByte  (see §9)

    public boolean isFull(long storedAmount);

}

public record CellUpgradeProfile(
        boolean fuzzy,
        boolean inverter,
        int maxEnergyCards,
        int totalSlots
) {

}
```

`DeepCellCatalog.ALL_CELLS` contains the **complete product model**: `3 × 10 × 2 = 60`
`CellDefinition`s, including Chemical. The catalog is a product catalog, not the set of registered
NeoForge items.

`CellUpgradeProfile` (from `docs/content.md` §6):

| Type     | Form     | Fuzzy | Inverter | Void | Energy | Slots |
|----------|----------|------:|---------:|-----:|-------:|------:|
| Item     | Drive    |     1 |        1 |    1 |      0 |     3 |
| Fluid    | Drive    |     0 |        1 |    1 |      0 |     2 |
| Chemical | Drive    |     0 |        1 |    1 |      0 |     2 |
| Item     | Portable |     1 |        1 |    1 |     ≤4 |     4 |
| Fluid    | Portable |     0 |        1 |    1 |     ≤4 |     3 |
| Chemical | Portable |     0 |        1 |    1 |     ≤4 |     3 |

---

## 5. Runtime channel abstraction (Phase 2)

```java
public interface StorageChannelBinding {

    CellContentType type();

    AEKeyType keyType();

    boolean accepts(AEKey key);

}
```

`StorageChannelRegistry` is an `EnumMap<CellContentType, StorageChannelBinding>` with
`require(...)` / `find(...)`.

Bindings registered this round:

- `ItemChannelBinding` → `AEKeyType.items()`
- `FluidChannelBinding` → `AEKeyType.fluids()`

`amountPerByte` is **not** part of the binding — it is an AE2Objects product rule owned by
`CellContentType`. This avoids conflating AE2 Basic-Cell capacity with Deep-Cell capacity.

**Milestone:** `DeepCellSpec` is deleted and `CellDefinition` holds no `AEKeyType`.

---

## 6. Storage engine (Phases 3–4)

### 6.1 `DeepCellContents`

Single source of truth for loaded contents:

```java
amount map(Object2LongMap<AEKey>)  +
cached totalAmount
```

There must not be three drifting counters (`amounts` / `storedAmount` / `storedTypes`) as in the
current inventory; `typeCount()` derives from the map size.

### 6.2 `DeepCellSession`

Owns the **per-ItemStack server lifecycle**: UUID, lazy load, dirty flag, repository access,
serialize/deserialize, publish the ItemStack snapshot, delete the record when empty. It does **not**
know partition, fuzzy, capacity, insert policy or `CellState`.

### 6.3 `CellRecord` / `CellRepository` / `SavedDataCellRepository` / `CellContentsCodec`

- `CellRecord` is the persisted value (renamed from `DeepCellStorage`): `keys`, `amounts`,
  `storedAmount`, optional `cellItemId`, keeping legacy NBT `keys`/`amts`/`item_count`.
- `CellRepository` is a tiny interface (`find`/`put`/`remove`/`contains`) that must not mention
  `HolderLookup`, `MinecraftServer`, `ItemStack` or `AEKey`.
- `SavedDataCellRepository` implements the `ae2objects:storage_manager` SavedData; it no longer
  holds a `WeakReference<HolderLookup.Provider>`.
- `CellContentsCodec` owns `DeepCellContents ↔ CellRecord`, and is the only place that touches
  `AEKey.CODEC`, `NbtOps`, registries and the channel. It repairs (and reports `repaired`) invalid
  keys, wrong key type, zero/negative amounts, duplicate keys, length mismatch and stored-amount
  mismatch.
- `ServerCellContext(repository, registries)` is installed on `ServerStartedEvent` and cleared on
  `ServerStoppedEvent`; `platform` owns this lifecycle.

### 6.4 `DeepCellInventory`

Final shape — implements `StorageCell` only:

```text
accept (channel) -> filter (partition/fuzzy) -> nested-cell policy -> capacity clamp
-> insert/extract/available/status/idle drain -> persist delegation to Session
```

It must not contain:

```java
if(definition.type() ==ITEM) …
        if(definition.

form() ==PORTABLE) …
        if(key instanceof AEFluidKey) …
        if(key instanceof MekanismKey) …
```

### 6.5 `DeepCellInventoryFactory`

The single composition root: resolve definition → channel → workbench state → build
`DeepCellFilter` → obtain `ServerCellContext` → open `DeepCellSession` → construct
`DeepCellInventory`. It only resolves dependencies and constructs; it contains no capacity,
insertion, recovery or recipe logic.

### 6.6 UUID lifecycle (single implementation)

| Situation                             | Behaviour                                                        |
|---------------------------------------|------------------------------------------------------------------|
| New empty cell                        | no UUID, no `CellRecord`                                         |
| `SIMULATE` insertion                  | no UUID, no record, no ItemStack mutation                        |
| First successful `MODULATE` insertion | allocate UUID                                                    |
| Cell becomes empty                    | delete record, clear UUID, clear client snapshot (pristine cell) |

---

## 7. Client projection (Phase 5)

Server and client are two different models; do not reuse one nullable-repository inventory.

```java
public record CellStackSnapshot(
        @Nullable UUID id,
        long storedAmount,
        int storedTypes,
        List<GenericStack> preview,
        FuzzyMode fuzzyMode
) {

}

public final class CellStackData {          // sole owner of the data-component contract

    CellStackSnapshot read(ItemStack stack);

    void publish(ItemStack stack, UUID id, DeepCellContents contents, List<GenericStack> preview);

    void clear(ItemStack stack);

}

public final class CellView {               // pure functions

    static CellState status(CellDefinition definition, CellStackSnapshot snapshot);

}
```

- Tooltip and item tint read the snapshot; they never call
  `DeepCellInventory.createInventory(stack, null, null)`.
- Composition stack data is read from `CellStackData`; no other code accesses
  `Ae2ObjectsDataComponents.*` directly.
- `cell_item_count` / persisted `item_count` keep their legacy names but are semantically the stored
  **native amount** (works for items, fluids and chemicals).

---

## 8. Item layer (Phase 5)

```java
public interface DeepCellDefinitionProvider {

    CellDefinition definition();

}
```

- `DeepDriveCellItem extends Item implements DeepCellDefinitionProvider, ICellWorkbenchItem,
  AEToolItem` — holds only a `CellDefinition`; interaction, workbench bridge, tooltip delegation and
  disassembly trigger.
- `DeepPortableCellItem extends AbstractPortableCell implements DeepCellDefinitionProvider` —
  **not**
  `IBasicCellItem`. AE2's `AbstractPortableCell` already provides battery, menu opening, power and
  energy-aware disassembly. Deep storage still flows through the custom `ICellHandler`.
- `CellWorkbenchSupport` provides `config(...)`, `upgrades(..., onChanged)`, `fuzzyMode(...)`,
  `setFuzzyMode(...)` for both forms, resolving runtime info from `StorageChannelRegistry` and
  `CellUpgradeProfile`.
- `CellDisassemblyService` holds the shared storage-empty check and record/UUID cleanup; Drive and
  Portable decide the returned items. Portable reuses AE2's `StorageCellDisassemblyRecipe` for the
  energy-aware path.

**Abstracting note:** the giant `DeepCellItem` default-method interface is deleted.

---

## 9. Capacity semantics

`usedBytes = storedAmount / amountPerByte` (integer division) is kept exactly as specified by
`values.md`; this refactor does not change product rules. Any future decision that partial amounts
should occupy a whole byte is a product change and would be made only in `CellCapacity`.

---

## 10. Content registry (Phase 6)

Delete `DeepCellRegistration`. Replace with:

- `DeepCellCatalog` — the 60 product definitions.
- `CellRegistrationPlan` — which definitions are active in the current environment (driven by
  `IntegrationSet`).
- `RegisteredCells` — `Map<CellDefinition, DeferredItem<? extends Item>>`; keeps only the registry
  handle, since ids/models/translations/upgrades all derive from `CellDefinition`.
- `RegisteredHousings` — `Map<CellContentType, DeferredItem<Item>>`:
  `deep_item_cell_housing`, `deep_fluid_cell_housing`, `deep_chemical_cell_housing`.
- `CellComponentSources.forTier(tier)` → `CellComponentSource(Identifier itemId,
  Optional<IntegrationId> requiredMod)`:
    - `1k`–`256k` → `ae2:cell_component_*`, no mod;
    - `1m`–`256m` → `megacells:cell_component_*`, `MEGA_CELLS`.

Creative tab order follows `content.md` §5: item housing, fluid housing, chemical housing, all
non-portable cells, all portable cells — sorted by `form → type → tier`, not by registration order.

---

## 11. Optional integration

### 11.1 MEGA Cells (Phase 9)

MEGA only affects the **component source** and the **recipe condition**. Capacity, inventory,
persistence, `AEKey`, portable logic are untouched, so `1m`–`256m` items are always registered and
only their recipes are gated by `neoforge:mod_loaded("megacells")`. No MEGA Java API is used.

### 11.2 Applied Mekanistics (deferred)

`integration/appmek` gets only a no-op `AppMekBootstrap` guarded by
`ModList.get().isLoaded("appmek")` (string id, no foreign import) plus a `TODO` seam. The future
`ChemicalChannelBinding` (with `ChemicalAttributeValidator`) and `ChemicalPortableMenuBinding` land
there, and `CellRegistrationPlan` activates `CellContentType.CHEMICAL` only when the integration is
present. `mods.toml` and `libs.versions.toml` are **not** touched until appmek exists for 26.1.2.

### 11.3 `IntegrationSet`

```java
public record IntegrationSet(
        boolean megaCells,
        boolean appliedMekanistics
) {

}
```

Detection is centralised here; `ModList.get().isLoaded(...)` must not be scattered across items,
inventory, tooltips and datagen.

---

## 12. Bootstrap order (`Ae2Objects`)

```text
Ae2Objects constructor
└─ IntegrationSet.detect()
   ├─ CellRuntimeBindings.bootstrapRequired()   // ITEM + FLUID channels, portable menus
   ├─ OptionalIntegrations.prepare(...)          // appmek no-op seam
   ├─ ModItems.defineContent(...)                // housings, drive cells, portable cells
   ├─ ModItems.register(modBus); ModDataComponents.register(modBus)
   └─ setup events: cell handler, upgrades, drive models, creative tab
```

Foreign classes are only touched after the corresponding mod is confirmed loaded.

---

## 13. Commands and services (Phase 5–6)

- `Ae2ObjectsCommand` keeps only command wiring (permissions, arguments, messages).
- `CellRecoveryService.recover(UUID)` owns repository lookup, registry lookup, preview rebuild and
  legacy fallback.
- `CellIdentityService` owns UUID read/copy for `/ae2objects getuuid`.
- `CellCloneService.copyIndependent(ItemStack)` owns clone (fresh UUID + deep copy); the mixin only
  calls it and never invents a UUID client-side.

Legacy records without `cell_item` recover as a `256k` item Drive cell; the field is written on the
next persist.

---

## 14. Compatibility guardrails (must hold in every phase)

- Registry ids `ae2objects:deep_item_storage_cell_{1k,4k,16k,64k,256k}` and
  `ae2objects:deep_item_cell_housing`.
- SavedData id `ae2objects:storage_manager`.
- Data component ids `ae2objects:cell_id`, `ae2objects:cell_item_count`,
  `ae2objects:cell_type_count`, `ae2objects:fuzzy_mode`.
- Persisted NBT `keys`, `amts`, `item_count`; new optional `cell_item`.
- Client preview via `ae2:storage_cell_inv` (≤10 entries, amount descending).

---

## 15. Work items by phase (commit sequence)

| #  | Commit                                               | Work items                                                                                                                                       | Gate                          |
|----|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| 0  | `refactor: checkpoint deep-cell module`              | Verify current tree, commit as-is                                                                                                                | build + `test` green          |
| 0b | `test: characterize deep-cell behavior`              | capacity, insert/extract/SIMULATE, partition whitelist/blacklist, fuzzy, UUID lifecycle, empty cleanup, clone, recovery, NBT round-trip, preview | tests green                   |
| 1  | `refactor(domain): CellDefinition model`             | `cell/model/*`, map 5 item cells                                                                                                                 | registry ids unchanged        |
| 2  | `refactor(channel): decouple key types`              | channel interface + registry + Item/Fluid bindings; delete `DeepCellSpec`                                                                        | definition has no `AEKeyType` |
| 3  | `refactor(persistence): contents/session/repository` | contents, session, record, repository, codec, context; legacy decode + `cell_item`                                                               | legacy world loads            |
| 4  | `refactor(storage): StorageCell engine`              | inventory rewrite, filter, nested policy, factory                                                                                                | **engine frozen**             |
| 5  | `refactor(item): client view + item layer`           | stack data/snapshot/view, workbench support, definition provider, drive item, tooltip/tint; delete giant interface                               | tooltip/tint correct          |
| 6  | `refactor(content): catalog-driven registration`     | mod items, registered cells/housings, registration plan, component sources; delete descriptor; creative/upgrades/models/datagen from definitions | no engine change              |
| 7  | `feat(fluid): deep fluid drive cells`                | 1k probe → 1k–256k + housing + resources                                                                                                         | engine untouched              |
| 8  | `feat(portable): deep portable cells`                | 1k item probe → item+fluid 1k–256m; disassembly recipes; battery/charge/dye                                                                      | engine untouched              |
| 9  | `feat(megacells): 1m–256m tiers`                     | enable item/fluid MEGA tiers; conditional recipes                                                                                                | engine untouched              |
| 10 | `feat(gametest): world-level tests`                  | GameTest harness + cases (§17)                                                                                                                   | gametests pass                |
| 11 | `docs: rewrite architecture & refactor plan`         | rewrite docs, update map, note chemical pending appmek                                                                                           | —                             |
| —  | deferred                                             | `integration/appmek` chemistry + compileOnly deps                                                                                                | when appmek 26.1.2 exists     |

---

## 16. Unit test matrix (final)

- **Domain** — catalog = 60, unique ids, 3 types × 10 tiers × 2 forms complete.
- **Capacity** — every type×tier; `256m` fluid/chemical = `256_000_000_000` native units in `long`.
- **Upgrade profiles** — the six combinations match `content.md` §6.
- **Channels** — ITEM rejects fluid, FLUID rejects item, CHEMICAL rejects non-chemical and invalid
  attributes (synthetic binding until appmek exists).
- **Persistence** — legacy decode, `cell_item` decode, wrong-key-type repair, duplicate merge,
  invalid key drop, amount-mismatch repair, empty cleanup, clone independent UUID.
- **Optional integration** — AE2-only, +MEGA, +AppMek, +both (activation/registration only).
- **Portable** — half tier capacity, fixed 1 AE/t drain, battery value, charge rate, energy-card
  multiplier, menu per type, default colour, energy-safe disassembly.

---

## 17. GameTest plan (Phase 10)

- `@GameTestHolder("ae2objects")` test classes under `.../gametest/`, using an empty/simple
  structure template and `@PrefixGameTestTemplate`.
- Cases: drive insert/extract; ME Chest compatibility; ME Drive compatibility; partition whitelist;
  partition blacklist; fuzzy (item only); fluid rejects fuzzy; clone from a container menu;
  recovery; portable open; portable power.
- Build: add a game-test server run to `neoForge.runs`; `forge.enabledGameTestNamespaces` is already
  `ae2objects`. Confirm the exact ModDevGradle game-test run API before writing the first test.

---

## 18. Verification per phase

- `./gradlew :26.1.2:buildAndCollect` and `./gradlew :26.1.2:test`.
- After resource-affecting phases (7–9): `./gradlew :26.1.2:runServerData` to regenerate
  recipes/models/lang into `src/generated/resources` and `src/main/resources`.
- Manual `runClient` smoke after phases 4, 5, 7, 8, 9.

---

## 19. Open items to resolve during implementation

1. Exact NeoForge conditional-recipe datagen API (`RecipeOutput.withConditions` +
   `ModLoadedCondition`).
2. ModDevGradle `gameTestServer` run configuration specifics for this version.
3. `NestedCellPolicy` semantics vs AE2's own nested-cell rule (prefer
   `StorageCells.getCellInventory(...)` / `StorageCell.canFitInsideCell()`; deep cells report
   `canFitInsideCell() == false`).
4. `AbstractPortableCell` menu resolution: populate `PortableMenuRegistry` (AE2 bindings) **before**
   portable item registration.
5. `StorageCellDisassemblyRecipe` datagen shape (`cell` + `cell_disassembly_items`).

---

## 20. Acceptance criteria

All of the following must hold when the plan is complete:

- `CellDefinition` references no `AEKeyType`; core/domain references no AppMek/Mekanism.
- `DeepCellInventory` contains no `ITEM`/`FLUID`/`CHEMICAL`/`DRIVE`/`PORTABLE` branches and no
  concrete item dependency.
- Tooltip/tint never construct a `DeepCellInventory`.
- The client never reaches `SavedDataCellRepository`.
- `SavedDataCellRepository` holds no `HolderLookup.Provider`.
- `DeepCellSpec`, `DeepCellItem` (giant interface) and `DeepCellRegistration` are gone.
- All concrete cell identity is expressed by `CellDefinition`.
- All 60 target cells are expressible by `DeepCellCatalog`; Fluid, MEGA tiers, Chemical and Portable
  need no new inventory; UUID lifecycle, capacity and the AE-key codec each have exactly one
  implementation.
- Existing registry ids and the SavedData format remain compatible.
