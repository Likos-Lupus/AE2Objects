package top.likoslupus.ae2objects.cell.inventory;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.persistence.DeepStorageAccess;

import org.jspecify.annotations.Nullable;

/** AE2 cell-handler entry point. */
public final class DeepCellHandler implements ICellHandler {

    public static final DeepCellHandler INSTANCE = new DeepCellHandler();

    private DeepCellHandler() {
    }

    @Override
    public boolean isCell(ItemStack stack) {
        return stack.getItem() instanceof DeepCellItem;
    }

    @Override
    public @Nullable DeepCellInventory getCellInventory(
            ItemStack stack,
            @Nullable ISaveProvider container
    ) {
        return DeepCellInventory.createInventory(stack, container, DeepStorageAccess.getOrNull());
    }

}
