package top.likoslupus.ae2objects.cell;

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
import top.likoslupus.ae2objects.cell.inventory.DeepCellTooltip;
import top.likoslupus.ae2objects.cell.persistence.DeepCellStorageIo;
import top.likoslupus.ae2objects.cell.persistence.DeepStorageAccess;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Marker and capability contract shared by every deep-cell item form.
 */
public interface DeepCellItem extends ICellWorkbenchItem {

    default AEKeyType getKeyType() {
        return cellSpec().keyType();
    }

    DeepCellSpec cellSpec();

    default int getBytes(ItemStack cellItem) {
        return cellSpec().tier().bytes();
    }

    default double getIdleDrain() {
        return cellSpec().tier().idleDrain();
    }

    default long amountPerByte() {
        return cellSpec().capacity().amountPerByte();
    }

    default boolean supportsFuzzy() {
        return cellSpec().supportsFuzzy();
    }

    default boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
        if (!cellSpec().accepts(requestedAddition)) {
            return true;
        }

        if (requestedAddition instanceof AEItemKey itemKey
                && itemKey.getItem() instanceof IBasicCellItem
        ) {
            var inventory = BasicCellHandler.INSTANCE.getCellInventory(itemKey.toStack(), null);
            return inventory != null && inventory.getUsedBytes() > 0;
        }

        return false;
    }

    default boolean storableInStorageCell() {
        return false;
    }

    default boolean isStorageCell(ItemStack stack) {
        return true;
    }

    ConfigInventory getConfigInventory(ItemStack stack);

    default void addCellInformationToTooltip(ItemStack stack, List<Component> lines) {
        Preconditions.checkArgument(stack.getItem() == this);
        DeepCellTooltip.addCellInformation(stack, lines);
    }

    default Optional<TooltipComponent> getCellTooltipImage(ItemStack stack) {
        Preconditions.checkArgument(stack.getItem() == this);
        return DeepCellTooltip.getTooltipImage(stack);
    }

    default ItemStack copyWithIndependentStorage(ItemStack original) {
        var copy = original.copy();
        var oldCellId = DeepCellStackData.cellId(original);
        if (oldCellId == null) {
            return copy;
        }

        var manager = DeepStorageAccess.getOrNull();
        if (manager == null) {
            // The authoritative clone happens server-side.
            // Do not invent an unbacked UUID client-side.
            return copy;
        }

        var newCellId = UUID.randomUUID();
        var itemId = DeepCellStackData.registeredItemId(original);
        var storage = manager.findCell(oldCellId)
                .orElseGet(() -> manager.emptyCell(itemId))
                .withCellItemIdIfMissing(itemId);

        manager.updateCell(newCellId, storage);
        copy.set(Ae2ObjectsDataComponents.CELL_ID.get(), newCellId);
        DeepCellStackData.updateSummary(
                copy,
                storage.storedAmount(),
                storage.storedTypesCount()
        );

        var loaded = DeepCellStorageIo.load(
                storage,
                manager.registries(),
                cellSpec().keyType()
        );
        DeepCellStackData.updatePreview(
                copy,
                DeepCellStorageIo.createPreview(loaded.amounts())
        );
        return copy;
    }

}
