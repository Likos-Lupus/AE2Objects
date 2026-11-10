package top.likoslupus.ae2objects.cell.storage;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ICellWorkbenchItem;

/**
 * Nested storage-cell rule.
 *
 * <p>Mirrors AE2's own rule but works for every registered {@code ICellHandler}: an item that is a
 * storage cell may only be stored when that cell reports
 * {@link appeng.api.storage.cells.StorageCell#canFitInsideCell()}. Deep cells always report
 * {@code false}, so they can never be nested.</p>
 *
 * <p>Most items are not storage cells at all. Checking that cheaply first avoids creating an
 * {@link net.minecraft.world.item.ItemStack} and taking AE2's global handler lock on every single
 * item insertion.</p>
 */
public final class NestedCellPolicy {

    private NestedCellPolicy() {
    }

    public static boolean accepts(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return true;
        }

        // All AE2 storage cells (and both deep-cell forms) implement ICellWorkbenchItem. Items
        // that do not implement it are treated as ordinary items, matching the pre-refactor
        // behaviour. A custom ICellHandler whose item deliberately does not implement this
        // marker is also treated as an ordinary item, as it was before the deep-cell refactor.
        if (!(itemKey.getItem() instanceof ICellWorkbenchItem)) {
            return true;
        }

        var inventory = StorageCells.getCellInventory(itemKey.toStack(), null);
        return inventory == null || inventory.canFitInsideCell();
    }

}
