package top.likoslupus.ae2objects.cell.inventory;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.ids.AEComponents;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.IPartitionList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.DeepCellStackData;
import top.likoslupus.ae2objects.cell.model.CellCapacity;
import top.likoslupus.ae2objects.cell.persistence.DeepCellStorage;
import top.likoslupus.ae2objects.cell.persistence.DeepCellStorageIo;
import top.likoslupus.ae2objects.cell.persistence.DeepStorageManager;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * AE2 storage adapter for all deep-cell key types.
 *
 * <p>The inventory owns runtime mutation and filtering only. Key serialization lives in
 * {@link DeepCellStorageIo}, world persistence in {@link DeepStorageManager}, and byte/unit math in
 * the cell spec.</p>
 */
public final class DeepCellInventory implements StorageCell {

    private final DeepCellItem cellItem;
    private final CellCapacity capacity;
    private final ItemStack stack;
    private final @Nullable ISaveProvider container;
    private final @Nullable DeepStorageManager storageManager;

    private IPartitionList partitionList;
    private IncludeExclude partitionListMode;
    private int storedTypes;
    private long storedAmount;
    private @Nullable Object2LongMap<AEKey> storedAmounts;
    private boolean persisted = true;

    private DeepCellInventory(
            DeepCellItem cellItem,
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            @Nullable DeepStorageManager storageManager
    ) {
        this.cellItem = cellItem;
        this.capacity = cellItem.capacity();
        this.stack = stack;
        this.container = saveProvider;
        this.storageManager = storageManager;
        initSummary();
        updateFilter();
    }

    private void initSummary() {
        var cellId = getCellUUID();
        if (cellId == null) {
            storedTypes = 0;
            storedAmount = 0L;
            return;
        }

        if (storageManager != null) {
            var storage = storageManager.getOrCreateCell(
                    cellId,
                    DeepCellStackData.registeredItemId(stack)
            );
            storedTypes = storage.storedTypesCount();
            storedAmount = storage.storedAmount();
        } else {
            storedTypes = DeepCellStackData.storedTypes(stack);
            storedAmount = DeepCellStackData.storedAmount(stack);
        }
    }

    private void updateFilter() {
        var builder = IPartitionList.builder();
        var upgrades = getUpgradesInventory();
        var config = getConfigInventory();
        var hasInverter = upgrades != null && upgrades.isInstalled(AEItems.INVERTER_CARD);

        if (cellItem.supportsFuzzy()
                && upgrades != null
                && upgrades.isInstalled(AEItems.FUZZY_CARD)
        ) {
            builder.fuzzyMode(getFuzzyMode());
        }

        builder.addAll(config.keySet());
        partitionListMode = hasInverter
                ? IncludeExclude.BLACKLIST
                : IncludeExclude.WHITELIST;
        partitionList = builder.build();
    }

    public @Nullable UUID getCellUUID() {
        return DeepCellStackData.cellId(stack);
    }

    public @Nullable IUpgradeInventory getUpgradesInventory() {
        return cellItem.getUpgrades(stack);
    }

    public ConfigInventory getConfigInventory() {
        return cellItem.getConfigInventory(stack);
    }

    public FuzzyMode getFuzzyMode() {
        return cellItem.supportsFuzzy()
                ?
                stack.getOrDefault(
                        Ae2ObjectsDataComponents.FUZZY_MODE.get(),
                        FuzzyMode.IGNORE_ALL
                )
                : FuzzyMode.IGNORE_ALL;
    }

    public static @Nullable DeepCellInventory createInventory(
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            @Nullable DeepStorageManager storageManager
    ) {
        requireNonNull(stack, "Cannot create cell inventory for null ItemStack");

        if (!(stack.getItem() instanceof DeepCellItem cellItem)
                || !cellItem.isStorageCell(stack)
        ) {
            return null;
        }

        return new DeepCellInventory(
                cellItem,
                stack,
                saveProvider,
                storageManager
        );
    }

    public static boolean hasCellUUID(ItemStack cell) {
        return cell.getItem() instanceof DeepCellItem
                && cell.has(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    private static boolean isCellEmpty(@Nullable DeepCellInventory inventory) {
        return inventory == null
                || inventory.getAvailableStacks().isEmpty();
    }

    public IncludeExclude getPartitionListMode() {
        return partitionListMode;
    }

    public boolean isPreformatted() {
        return !partitionList.isEmpty();
    }

    public boolean isFuzzy() {
        return cellItem.supportsFuzzy()
                && partitionList instanceof FuzzyPriorityList;
    }

    @Override
    public CellState getStatus() {
        return statusFor(storedAmount);
    }

    private CellState statusFor(long amount) {
        return amount <= 0
                ? CellState.EMPTY
                : capacity.isFull(amount)
                        ? CellState.FULL
                        : CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return cellItem.getIdleDrain();
    }

    @Override
    public void persist() {
        if (persisted || storageManager == null) {
            return;
        }

        if (storedAmount <= 0) {
            var cellId = getCellUUID();
            if (cellId != null) {
                storageManager.removeCell(cellId);
            }
            storedAmounts = new Object2LongOpenHashMap<>();
            storedTypes = 0;
            storedAmount = 0L;
            DeepCellStackData.clearStorageIdentity(stack);
            persisted = true;
            return;
        }

        var cellId = ensureCellId();
        var result = DeepCellStorageIo.save(
                getCellItems(),
                storageManager.registries(),
                DeepCellStackData.registeredItemId(stack)
        );

        if (result.storage().storedAmount() <= 0) {
            storageManager.removeCell(cellId);
            storedAmounts = new Object2LongOpenHashMap<>();
            storedTypes = 0;
            storedAmount = 0L;
            DeepCellStackData.clearStorageIdentity(stack);
            persisted = true;
            return;
        }

        storageManager.updateCell(cellId, result.storage());
        storedAmounts = result.amounts();
        storedTypes = result.storage().storedTypesCount();
        storedAmount = result.storage().storedAmount();
        DeepCellStackData.updateSummary(stack, storedAmount, storedTypes);
        DeepCellStackData.updatePreview(stack, result.preview());
        persisted = true;
    }

    public CellState getClientStatus() {
        return statusFor(getCachedStoredAmount());
    }

    public long getCachedStoredAmount() {
        return hasCellUUID()
                ? DeepCellStackData.storedAmount(stack)
                : 0L;
    }

    public boolean hasCellUUID() {
        return stack.has(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    private UUID ensureCellId() {
        var existing = getCellUUID();
        if (existing != null) {
            return existing;
        }

        if (storageManager == null) {
            throw new IllegalStateException(
                    "Cannot allocate a deep-cell UUID without server storage"
            );
        }

        var cellId = UUID.randomUUID();
        stack.set(Ae2ObjectsDataComponents.CELL_ID.get(), cellId);
        storageManager.getOrCreateCell(cellId, DeepCellStackData.registeredItemId(stack));
        return cellId;
    }

    private DeepCellStorage getCellStorage() {
        var cellId = getCellUUID();
        return cellId == null || storageManager == null
                ? DeepCellStorage.empty()
                : storageManager.getOrCreateCell(cellId, DeepCellStackData.registeredItemId(stack));
    }

    private Object2LongMap<AEKey> getCellItems() {
        if (storedAmounts == null) {
            loadCellItems();
        }
        return requireNonNull(storedAmounts);
    }

    private void loadCellItems() {
        if (storageManager == null) {
            var loaded = new Object2LongOpenHashMap<AEKey>();
            List<GenericStack> preview = stack.getOrDefault(
                    AEComponents.STORAGE_CELL_INV,
                    List.of()
            );
            preview.stream()
                    .filter(entry ->
                            entry.amount() > 0 && cellItem.getKeyType().contains(entry.what())
                    )
                    .forEach(entry ->
                            loaded.put(entry.what(), entry.amount())
                    );
            storedAmounts = loaded;
            return;
        }

        var result = DeepCellStorageIo.load(
                getCellStorage(),
                storageManager.registries(),
                cellItem.getKeyType()
        );
        storedAmounts = result.amounts();

        if (result.repaired()) {
            saveChanges();
        }
    }

    private void saveChanges() {
        var items = getCellItems();
        storedTypes = items.size();
        storedAmount = 0L;
        items.values().forEach(amount -> storedAmount += amount);

        persisted = false;
        if (container != null) {
            container.saveChanges();
        } else {
            persist();
        }
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0
                || !cellItem.channel().accepts(what)
                || !partitionList.matchesFilter(what, partitionListMode)
                || cellItem.isBlackListed(stack, what)
                || what instanceof AEItemKey itemKey
                && itemKey.getItem() instanceof DeepCellItem
                && !isCellEmpty(createInventory(itemKey.toStack(), null, storageManager))
        ) {
            return 0;
        }

        var accepted = Math.min(amount, capacity.remainingAmount(storedAmount));
        if (accepted <= 0) {
            return 0;
        }

        if (mode == Actionable.MODULATE) {
            if (storageManager != null) {
                ensureCellId();
            }
            getCellItems().mergeLong(what, accepted, Long::sum);
            saveChanges();
        }
        return accepted;
    }

    @Override
    public long extract(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source
    ) {
        if (amount <= 0) {
            return 0;
        }

        var currentAmount = getCellItems().getLong(what);
        if (currentAmount <= 0) {
            return 0;
        }

        var extracted = Math.min(amount, currentAmount);
        if (mode == Actionable.MODULATE) {
            if (extracted == currentAmount) {
                getCellItems().removeLong(what);
            } else {
                getCellItems().put(what, currentAmount - extracted);
            }
            saveChanges();
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        getCellItems().object2LongEntrySet()
                .forEach(entry ->
                        out.add(entry.getKey(), entry.getLongValue())
                );
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

    public long getTotalBytes() {
        return capacity.bytes();
    }

    public long getUsedBytes() {
        return capacity.usedBytes(storedAmount);
    }

    public long getFreeBytes() {
        return capacity.freeBytes(storedAmount);
    }

    public long getCachedUsedBytes() {
        return capacity.usedBytes(getCachedStoredAmount());
    }

    public int getCachedStoredTypes() {
        return hasCellUUID()
                ? DeepCellStackData.storedTypes(stack)
                : 0;
    }

    public long getStoredAmount() {
        return storedAmount;
    }

    public int getStoredTypes() {
        return storedTypes;
    }

    public boolean canHoldNewAmount() {
        return capacity.remainingAmount(storedAmount) > 0;
    }

}
