package top.likoslupus.ae2objects.storage;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.me.cells.BasicCellHandler;
import appeng.util.ConfigInventory;
import com.google.common.base.Preconditions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface DeepCellItem extends ICellWorkbenchItem {

    AEKeyType getKeyType();

    int getBytes(ItemStack cellItem);

    default boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
        if (requestedAddition instanceof AEItemKey itemKey
                && itemKey.getItem() instanceof IBasicCellItem
        ) {
            var inv = BasicCellHandler.INSTANCE.getCellInventory(
                    itemKey.toStack(),
                    null
            );
            return inv != null && inv.getUsedBytes() > 0;
        }
        return false;
    }

    default boolean storableInStorageCell() {
        return false;
    }

    default boolean isStorageCell(ItemStack i) {
        return true;
    }

    double getIdleDrain();

    ConfigInventory getConfigInventory(ItemStack is);

    default void addCellInformationToTooltip(
            ItemStack is,
            List<Component> lines
    ) {
        Preconditions.checkArgument(is.getItem() == this);
        DeepCellHandler.INSTANCE.addCellInformationToTooltip(is, lines);
    }

    default Optional<TooltipComponent> getCellTooltipImage(ItemStack is) {
        Preconditions.checkArgument(is.getItem() == this);
        return DeepCellHandler.INSTANCE.getTooltipImage(is);
    }

    ItemStack clone(ItemStack item);

}
