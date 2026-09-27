package top.likoslupus.ae2objects.cell.item;

import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.persistence.CellContentsCodec;
import top.likoslupus.ae2objects.cell.persistence.CellRecord;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.UUID;

/**
 * Creates an independent copy of a deep cell (fresh UUID + deep copy of the stored contents).
 *
 * <p>On the client there is no authoritative repository, so the copy is left without an invented
 * UUID and the server performs the real clone.</p>
 */
public final class CellCloneService {

    private CellCloneService() {
    }

    public static ItemStack copyIndependent(ItemStack original) {
        var copy = original.copy();
        var oldCellId = CellStackData.cellId(original);
        if (oldCellId == null
                || !(original.getItem() instanceof DeepCellDefinitionProvider provider)
        ) {
            return copy;
        }

        var context = ServerCellContext.getOrNull();
        if (context == null) {
            return copy;
        }

        var definition = provider.definition();
        var newCellId = UUID.randomUUID();
        var itemId = CellStackData.registeredItemId(original);
        var record = context.repository().find(oldCellId)
                .orElseGet(() -> CellRecord.empty(itemId))
                .withCellItemIdIfMissing(itemId);

        context.repository().put(newCellId, record);
        copy.set(Ae2ObjectsDataComponents.CELL_ID.get(), newCellId);
        CellStackData.updateSummary(
                copy,
                record.storedAmount(),
                record.storedTypesCount()
        );

        var keyType = StorageChannelRegistry.INSTANCE.require(definition.type()).keyType();
        var decoded = CellContentsCodec.decode(
                record,
                context.registries(),
                keyType
        );
        CellStackData.updatePreview(
                copy,
                CellContentsCodec.preview(decoded.contents())
        );
        return copy;
    }

}
