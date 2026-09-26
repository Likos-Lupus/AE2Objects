package top.likoslupus.ae2objects.cell.inventory;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.model.CellCapacity;
import top.likoslupus.ae2objects.cell.storage.DeepCellContents;
import top.likoslupus.ae2objects.cell.storage.DeepCellSession;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * AE2 storage adapter for all deep-cell key types.
 *
 * <p>The inventory owns acceptance, filtering, capacity clamping and the {@link StorageCell}
 * contract only. Loaded contents and the persistence lifecycle live in
 * {@link DeepCellSession}.</p>
 */
public final class DeepCellInventory implements StorageCell {

    private final DeepCellItem cellItem;
    private final CellCapacity capacity;
    private final ItemStack stack;
    private final @Nullable ISaveProvider container;
    private final DeepCellSession session;

    private IPartitionList partitionList;
    private IncludeExclude partitionListMode;

    private DeepCellInventory(
            DeepCellItem cellItem,
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            @Nullable ServerCellContext context
    ) {
        this.cellItem = cellItem;
        this.capacity = cellItem.capacity();
        this.stack = stack;
        this.container = saveProvider;
        this.session = new DeepCellSession(stack, cellItem.definition(), context);
        updateFilter();
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

    public static boolean hasCellUUID(ItemStack cell) {
        return cell.getItem() instanceof DeepCellItem
                && cell.has(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    public @Nullable UUID getCellUUID() {
        return session.cellId();
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
        return statusFor(
                session.context() == null
                        ? session.knownStoredAmount()
                        : session.storedAmount()
        );
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
        session.persist();
    }

    public CellState getClientStatus() {
        return statusFor(session.knownStoredAmount());
    }

    public long getCachedStoredAmount() {
        return session.knownStoredAmount();
    }

    public int getCachedStoredTypes() {
        return session.knownStoredTypes();
    }

    public boolean hasCellUUID() {
        return session.hasIdentity();
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0
                || !cellItem.channel().accepts(what)
                || !partitionList.matchesFilter(what, partitionListMode)
                || cellItem.isBlackListed(stack, what)
                || what instanceof AEItemKey itemKey
                && itemKey.getItem() instanceof DeepCellItem
                && !isCellEmpty(createInventory(itemKey.toStack(), null, session.context()))
        ) {
            return 0;
        }

        var accepted = Math.min(
                amount,
                capacity.remainingAmount(contents().totalAmount())
        );
        if (accepted <= 0) {
            return 0;
        }

        if (mode == Actionable.MODULATE) {
            if (session.context() != null) {
                session.ensureIdentity();
            }
            contents().insert(what, accepted);
            saveChanges();
        }
        return accepted;
    }

    private static boolean isCellEmpty(@Nullable DeepCellInventory inventory) {
        return inventory == null
                || inventory.getAvailableStacks().isEmpty();
    }

    public static @Nullable DeepCellInventory createInventory(
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            @Nullable ServerCellContext context
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
                context
        );
    }

    private DeepCellContents contents() {
        return session.contents();
    }

    private void saveChanges() {
        session.markDirty();
        if (container != null) {
            container.saveChanges();
        } else {
            session.persist();
        }
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

        var currentAmount = contents().amount(what);
        if (currentAmount <= 0) {
            return 0;
        }

        var extracted = Math.min(amount, currentAmount);
        if (mode == Actionable.MODULATE) {
            contents().extract(what, extracted);
            saveChanges();
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        contents().asMap().object2LongEntrySet()
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
        return capacity.usedBytes(session.storedAmount());
    }

    public long getFreeBytes() {
        return capacity.freeBytes(session.storedAmount());
    }

    public long getCachedUsedBytes() {
        return capacity.usedBytes(session.knownStoredAmount());
    }

    public long getStoredAmount() {
        return session.storedAmount();
    }

    public int getStoredTypes() {
        return session.storedTypes();
    }

    public boolean canHoldNewAmount() {
        return capacity.remainingAmount(contents().totalAmount()) > 0;
    }

}
