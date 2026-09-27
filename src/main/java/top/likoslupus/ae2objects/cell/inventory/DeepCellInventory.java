package top.likoslupus.ae2objects.cell.inventory;

import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.util.ConfigInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.model.CellCapacity;
import top.likoslupus.ae2objects.cell.storage.DeepCellContents;
import top.likoslupus.ae2objects.cell.storage.DeepCellFilter;
import top.likoslupus.ae2objects.cell.storage.DeepCellSession;
import top.likoslupus.ae2objects.cell.storage.NestedCellPolicy;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Pure AE2 {@link StorageCell} engine for all deep-cell key types.
 *
 * <p>Owns acceptance, filtering, capacity clamping and the {@code StorageCell} contract only.
 * Loaded contents and persistence live in {@link DeepCellSession}; partition/fuzzy behaviour lives
 * in {@link DeepCellFilter}; nested-cell rules live in {@link NestedCellPolicy}. The engine must
 * not branch on item/fluid/chemical or drive/portable.</p>
 */
public final class DeepCellInventory implements StorageCell {

    private final DeepCellItem cellItem;
    private final CellCapacity capacity;
    private final ItemStack stack;
    private final @Nullable ISaveProvider container;
    private final DeepCellSession session;
    private final DeepCellFilter filter;

    public DeepCellInventory(
            DeepCellItem cellItem,
            ItemStack stack,
            @Nullable ISaveProvider saveProvider,
            DeepCellSession session,
            DeepCellFilter filter
    ) {
        this.cellItem = cellItem;
        this.capacity = cellItem.capacity();
        this.stack = stack;
        this.container = saveProvider;
        this.session = session;
        this.filter = filter;
    }

    public static boolean hasCellUUID(ItemStack cell) {
        return cell.getItem() instanceof DeepCellItem
                && cell.has(Ae2ObjectsDataComponents.CELL_ID.get());
    }

    private static boolean isCellEmpty(@Nullable DeepCellInventory inventory) {
        return inventory == null
                || inventory.getAvailableStacks().isEmpty();
    }

    public @Nullable UUID getCellUUID() {
        return session.cellId();
    }

    public @Nullable IUpgradeInventory getUpgradesInventory() {
        return cellItem.getUpgrades(stack);
    }

    public ConfigInventory getConfigInventory() {
        return cellItem.getConfigInventory(stack);
    }

    public IncludeExclude getPartitionListMode() {
        return filter.mode();
    }

    public boolean isPreformatted() {
        return filter.isPreformatted();
    }

    public boolean isFuzzy() {
        return filter.isFuzzy();
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
    public boolean canFitInsideCell() {
        return false;
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
    public long insert(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source
    ) {
        if (amount <= 0
                || !cellItem.channel().accepts(what)
                || !filter.accepts(what)
                || !NestedCellPolicy.accepts(what)
        ) {
            return 0;
        }

        var accepted = Math.min(amount, capacity.remainingAmount(contents().totalAmount()));
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
