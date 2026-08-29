package top.likoslupus.ae2objects.storage;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.IPartitionList;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import top.likoslupus.ae2objects.item.DiskDriveItem;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public class DiskCellInventory implements StorageCell {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final DiskCellItem cellType;
    private final @Nullable ISaveProvider container;
    private final AEKeyType keyType;
    private final @Nullable DiskStorageManager storageManager;
    private IPartitionList partitionList;
    private IncludeExclude partitionListMode;
    private int storedItems;
    private long storedItemCount;
    private @Nullable Object2LongMap<AEKey> storedAmounts;
    private final ItemStack i;
    private boolean isPersisted = true;

    public DiskCellInventory(
            DiskCellItem cellType,
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            @Nullable DiskStorageManager storageManager
    ) {
        this.cellType = cellType;
        this.i = stack;
        this.container = saveProvider;
        this.keyType = cellType.getKeyType();
        this.storageManager = storageManager;
        this.storedAmounts = null;
        initData();

        updateFilter();
    }

    private void updateFilter() {
        var builder = IPartitionList.builder();

        var upgrades = getUpgradesInventory();
        var config = getConfigInventory();

        var hasInverter = upgrades != null && upgrades.isInstalled(AEItems.INVERTER_CARD);
        if (upgrades != null && upgrades.isInstalled(AEItems.FUZZY_CARD)) {
            builder.fuzzyMode(getFuzzyMode());
        }

        builder.addAll(config.keySet());

        partitionListMode = (
                hasInverter
                        ? IncludeExclude.BLACKLIST
                        : IncludeExclude.WHITELIST
        );
        partitionList = builder.build();
    }

    private DiskStorage getDiskStorage() {
        return getDiskUUID() != null && storageManager != null
                ? storageManager.getOrCreateDisk(getDiskUUID())
                : DiskStorage.empty();
    }

    private void initData() {
        if (hasDiskUUID()) {
            var diskStorage = getDiskStorage();
            this.storedItems = diskStorage.getStoredTypesCount();
            this.storedItemCount = diskStorage.getItemCount();
        } else {
            this.storedItems = 0;
            this.storedItemCount = 0;
            getCellItems();
        }
    }

    public IncludeExclude getPartitionListMode() {
        return partitionListMode;
    }

    public boolean isPreformatted() {
        return !partitionList.isEmpty();
    }

    public boolean isFuzzy() {
        return partitionList instanceof FuzzyPriorityList;
    }

    public ConfigInventory getConfigInventory() {
        return this.cellType.getConfigInventory(this.i);
    }

    public FuzzyMode getFuzzyMode() {
        return this.i.getOrDefault(
                Ae2ObjectsDataComponents.FUZZY_MODE.get(),
                FuzzyMode.IGNORE_ALL
        );
    }

    @Nullable
    public IUpgradeInventory getUpgradesInventory() {
        return this.cellType.getUpgrades(this.i);
    }

    @Override
    public CellState getStatus() {
        if (this.getStoredItemCount() == 0) {
            return CellState.EMPTY;
        }
        if (this.canHoldNewItem()) {
            return CellState.NOT_EMPTY;
        }
        return CellState.FULL;
    }

    public CellState getClientStatus() {
        if (this.getNbtItemCount() == 0) {
            return CellState.EMPTY;
        }
        if (getNbtItemCount() > 0 && getNbtItemCount() != getTotalBytes()) {
            return CellState.NOT_EMPTY;
        }
        return CellState.FULL;
    }

    @Override
    public double getIdleDrain() {
        return this.cellType.getIdleDrain();
    }

    @Override
    public void persist() {
        if (this.isPersisted || storageManager == null) {
            return;
        }

        var diskUuid = getDiskUUID();
        if (storedItemCount == 0) {
            if (diskUuid != null) {
                storageManager.removeDisk(diskUuid);
                i.remove(Ae2ObjectsDataComponents.CELL_ID.get());
                i.remove(Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get());
                initData();
            }
            return;
        }

        if (diskUuid == null) {
            return;
        }

        var amountsMap = this.storedAmounts;
        if (amountsMap == null) {
            return;
        }

        long itemCount = 0;
        var amounts = new LongArrayList(amountsMap.size());
        var keys = new ListTag();
        var ops = storageManager.getRegistries()
                .createSerializationContext(NbtOps.INSTANCE);

        for (var entry : amountsMap.object2LongEntrySet()) {
            var amount = entry.getLongValue();

            if (amount > 0) {
                itemCount += amount;
                var keyTag = AEKey.CODEC.encodeStart(
                        ops,
                        entry.getKey()
                ).result().orElse(null);
                if (keyTag instanceof CompoundTag compoundKey) {
                    keys.add(compoundKey);
                    amounts.add(amount);
                }
            }
        }

        if (keys.isEmpty()) {
            storageManager.updateDisk(
                    diskUuid,
                    new DiskStorage()
            );
        } else {
            storageManager.modifyDisk(
                    diskUuid,
                    keys,
                    amounts.toArray(new long[0]),
                    itemCount
            );
        }

        this.storedItems = amountsMap.size();
        this.storedItemCount = itemCount;
        i.set(Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get(), itemCount);

        this.isPersisted = true;
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

    public static @Nullable DiskCellInventory createInventory(
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            @Nullable DiskStorageManager storageManager
    ) {
        requireNonNull(stack, "Cannot create cell inventory for null itemstack");

        if (!(stack.getItem() instanceof DiskCellItem cellType)) {
            return null;
        }

        if (!cellType.isStorageCell(stack)) {
            return null;
        }

        return new DiskCellInventory(
                cellType,
                stack,
                saveProvider,
                storageManager
        );
    }

    public boolean hasDiskUUID() {
        return i.has(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    public static boolean hasDiskUUID(ItemStack disk) {
        return disk.getItem() instanceof DiskCellItem
                && disk.has(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    public @Nullable UUID getDiskUUID() {
        return i.get(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    private boolean isStorageCell(AEItemKey key) {
        var type = getStorageCell(key);
        return type != null && !type.storableInStorageCell();
    }

    private static @Nullable DiskDriveItem getStorageCell(AEItemKey itemKey) {
        return itemKey.getItem() instanceof DiskDriveItem diskDrive
                ? diskDrive
                : null;
    }

    private static boolean isCellEmpty(@Nullable DiskCellInventory inv) {
        return inv == null || inv.getAvailableStacks().isEmpty();
    }

    protected Object2LongMap<AEKey> getCellItems() {
        if (this.storedAmounts == null) {
            this.storedAmounts = new Object2LongOpenHashMap<>();
            this.loadCellItems();
        }

        return this.storedAmounts;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        this.getCellItems().object2LongEntrySet()
                .forEach(entry -> out.add(
                        entry.getKey(),
                        entry.getLongValue()
                ));
    }

    private void loadCellItems() {
        if (this.storageManager == null) {
            return;
        }

        var diskStorage = getDiskStorage();
        var amounts = diskStorage.getStackAmounts();
        var tags = diskStorage.getStackKeys();
        if (amounts.length != tags.size()) {
            LOGGER.warn(
                    "Loading storage cell with mismatched amounts/tags: {} != {}",
                    amounts.length,
                    tags.size()
            );
        }

        var corruptedTag = false;
        var registries = storageManager.getRegistries();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        var amountsMap = this.storedAmounts;
        if (amountsMap == null) {
            amountsMap = new Object2LongOpenHashMap<>();
            this.storedAmounts = amountsMap;
        }

        for (var idx = 0; idx < amounts.length; idx++) {
            var amount = amounts[idx];
            var tag = tags.getCompoundOrEmpty(idx);
            var key = AEKey.CODEC.parse(ops, tag).result().orElse(null);

            if (amount <= 0 || key == null) {
                corruptedTag = true;
            } else {
                amountsMap.put(key, amount);
            }
        }

        if (corruptedTag) {
            this.saveChanges();
        }
    }

    protected void saveChanges() {
        var amountsMap = getCellItems();
        this.storedItems = amountsMap.size();
        this.storedItemCount = 0;
        amountsMap.values().forEach(storedAmount ->
                this.storedItemCount += storedAmount
        );

        this.isPersisted = false;
        if (this.container != null) {
            this.container.saveChanges();
        } else {
            this.persist();
        }
    }

    public long getRemainingItemCount() {
        return this.getFreeBytes() > 0
                ? this.getFreeBytes()
                : 0;
    }

    @Override
    public long insert(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source
    ) {
        if (amount == 0 || !keyType.contains(what)) {
            return 0;
        }

        if (!this.partitionList.matchesFilter(what, this.partitionListMode)) {
            return 0;
        }

        if (this.cellType.isBlackListed(this.i, what)) {
            return 0;
        }

        if (what instanceof AEItemKey itemKey && this.isStorageCell(itemKey)) {
            var meInventory = createInventory(
                    itemKey.toStack(),
                    null,
                    storageManager
            );
            if (!isCellEmpty(meInventory)) {
                return 0;
            }
        }

        if (storageManager != null && !hasDiskUUID()) {
            var newUuid = UUID.randomUUID();
            i.set(Ae2ObjectsDataComponents.CELL_ID.get(), newUuid);
            storageManager.getOrCreateDisk(newUuid);
            loadCellItems();
        }

        var currentAmount = this.getCellItems().getLong(what);
        var remainingItemCount = getRemainingItemCount();

        if (amount > remainingItemCount) {
            amount = remainingItemCount;
        }

        if (mode == Actionable.MODULATE) {
            getCellItems().put(what, currentAmount + amount);
            this.saveChanges();
        }

        return amount;
    }

    @Override
    public long extract(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source
    ) {
        var extractAmount = Math.min(Integer.MAX_VALUE, amount);

        var currentAmount = getCellItems().getLong(what);
        if (currentAmount > 0) {
            if (extractAmount >= currentAmount) {
                if (mode == Actionable.MODULATE) {
                    getCellItems().remove(what, currentAmount);
                    this.saveChanges();
                }

                return currentAmount;
            } else {
                if (mode == Actionable.MODULATE) {
                    getCellItems().put(what, currentAmount - extractAmount);
                    this.saveChanges();
                }

                return extractAmount;
            }
        }

        return 0;
    }

    public long getTotalBytes() {
        return this.cellType.getBytes(this.i);
    }

    public long getFreeBytes() {
        return this.getTotalBytes() - this.getStoredItemCount();
    }

    public long getNbtItemCount() {
        return hasDiskUUID()
                ? i.getOrDefault(Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get(), 0L)
                : 0;
    }

    public long getStoredItemCount() {
        return this.storedItemCount;
    }

    public long getStoredItemTypes() {
        return this.storedItems;
    }

    public boolean canHoldNewItem() {
        return (getFreeBytes() > 0 && getFreeBytes() != getTotalBytes());
    }

}
