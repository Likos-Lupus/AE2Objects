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
import top.likoslupus.ae2objects.cell.channel.StorageChannelBinding;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.inventory.DeepCellTooltip;
import top.likoslupus.ae2objects.cell.model.CellCapacity;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.persistence.CellContentsCodec;
import top.likoslupus.ae2objects.cell.persistence.CellRecord;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Marker and capability contract shared by every deep-cell item form.
 *
 * <p>The item only describes its {@link CellDefinition}; every storage-type specific value (key
 * type, amount per byte, fuzzy support) is derived from the model plus the runtime channel
 * registry.</p>
 */
public interface DeepCellItem extends ICellWorkbenchItem {

    default int getBytes(ItemStack cellItem) {
        return definition().tier().bytes();
    }

    CellDefinition definition();

    default double getIdleDrain() {
        return definition().tier().idleDrain();
    }

    default boolean supportsFuzzy() {
        return definition().type().supportsFuzzy();
    }

    default CellCapacity capacity() {
        return new CellCapacity(definition().tier().bytes(), amountPerByte());
    }

    default long amountPerByte() {
        return definition().type().amountPerByte();
    }

    default boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
        if (!channel().accepts(requestedAddition)) {
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

    default StorageChannelBinding channel() {
        return StorageChannelRegistry.INSTANCE.require(definition().type());
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

        var context = ServerCellContext.getOrNull();
        if (context == null) {
            // The authoritative clone happens server-side.
            // Do not invent an unbacked UUID client-side.
            return copy;
        }

        var newCellId = UUID.randomUUID();
        var itemId = DeepCellStackData.registeredItemId(original);
        var record = context.repository().find(oldCellId)
                .orElseGet(() -> CellRecord.empty(itemId))
                .withCellItemIdIfMissing(itemId);

        context.repository().put(newCellId, record);
        copy.set(
                Ae2ObjectsDataComponents.CELL_ID.get(),
                newCellId
        );
        DeepCellStackData.updateSummary(
                copy,
                record.storedAmount(),
                record.storedTypesCount()
        );

        var decoded = CellContentsCodec.decode(
                record,
                context.registries(),
                getKeyType()
        );
        DeepCellStackData.updatePreview(
                copy,
                CellContentsCodec.preview(decoded.contents())
        );
        return copy;
    }

    default AEKeyType getKeyType() {
        return channel().keyType();
    }

}
