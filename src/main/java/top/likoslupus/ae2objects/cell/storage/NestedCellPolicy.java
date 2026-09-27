package top.likoslupus.ae2objects.cell.storage;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;

/**
 * Nested storage-cell rule.
 *
 * <p>Mirrors AE2's own rule but works for every registered {@code ICellHandler}: an item that is a
 * storage cell may only be stored when that cell reports
 * {@link appeng.api.storage.cells.StorageCell#canFitInsideCell()}. Deep cells always report
 * {@code false}, so they can never be nested.</p>
 */
public final class NestedCellPolicy {

    private NestedCellPolicy() {
    }

    public static boolean accepts(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return true;
        }

        var inventory = StorageCells.getCellInventory(itemKey.toStack(), null);
        return inventory == null || inventory.canFitInsideCell();
    }

}
