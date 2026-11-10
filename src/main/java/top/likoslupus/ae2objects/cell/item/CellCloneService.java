package top.likoslupus.ae2objects.cell.item;

import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.persistence.CellContentsCodec;
import top.likoslupus.ae2objects.cell.persistence.CellRecord;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.ModDataComponents;

import java.util.List;
import java.util.UUID;

/**
 * Creates an independent copy of a deep cell (fresh UUID + deep copy of the stored contents).
 *
 * <p>On the client there is no authoritative repository. The client-side prediction still gets a
 * fresh UUID and a cleared summary/preview, matching the pre-refactor behaviour; the server then
 * creates the authoritative independent record.</p>
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

        var context = ServerCellContext.getForCurrentThread();
        if (context == null) {
            // Client-side prediction (or a clone before the server context exists): never touch
            // the server repository, but do not let the copy share the original UUID either.
            copy.set(ModDataComponents.CELL_ID.get(), UUID.randomUUID());
            CellStackData.updateSummary(copy, 0L, 0);
            CellStackData.updatePreview(copy, List.of());
            return copy;
        }

        var definition = provider.definition();
        var newCellId = UUID.randomUUID();
        var itemId = CellStackData.registeredItemId(original);
        var record = context.repository().find(oldCellId)
                .orElseGet(() -> CellRecord.empty(itemId))
                .withCellItemIdIfMissing(itemId);

        context.repository().put(newCellId, record);
        copy.set(ModDataComponents.CELL_ID.get(), newCellId);
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
