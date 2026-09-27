package top.likoslupus.ae2objects.cell.storage;

import appeng.api.storage.cells.ISaveProvider;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.inventory.DeepCellInventory;
import top.likoslupus.ae2objects.platform.ServerCellContext;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Composition root for deep-cell inventories.
 *
 * <p>Resolves the definition and channel from the item, reads the workbench state, builds the
 * filter and opens the session. It only assembles collaborators; it contains no storage
 * policy.</p>
 */
public final class DeepCellInventoryFactory {

    private DeepCellInventoryFactory() {
    }

    public static @Nullable DeepCellInventory create(
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

        var upgrades = cellItem.getUpgrades(stack);
        var config = cellItem.getConfigInventory(stack);
        var filter = DeepCellFilter.create(
                config,
                upgrades,
                cellItem.getFuzzyMode(stack),
                cellItem.supportsFuzzy()
        );
        var session = new DeepCellSession(stack, cellItem.definition(), context);

        return new DeepCellInventory(
                cellItem,
                stack,
                saveProvider,
                session,
                filter
        );
    }

}
