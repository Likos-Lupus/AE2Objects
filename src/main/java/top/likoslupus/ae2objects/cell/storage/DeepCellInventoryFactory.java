package top.likoslupus.ae2objects.cell.storage;

import appeng.api.storage.cells.ISaveProvider;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.inventory.DeepCellInventory;
import top.likoslupus.ae2objects.cell.item.CellWorkbenchSupport;
import top.likoslupus.ae2objects.cell.item.DeepCellDefinitionProvider;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Composition root for deep-cell inventories.
 *
 * <p>Resolves the definition and channel from the item and opens the session. The workbench
 * filter is supplied lazily so status/tint queries do not build config or upgrade inventories.</p>
 */
public final class DeepCellInventoryFactory {

    private DeepCellInventoryFactory() {
    }

    public static @Nullable DeepCellInventory create(
            ItemStack stack,
            @Nullable ISaveProvider saveProvider
    ) {
        requireNonNull(stack, "Cannot create cell inventory for null ItemStack");

        if (!(stack.getItem() instanceof DeepCellDefinitionProvider provider)) {
            return null;
        }

        var definition = provider.definition();
        var channel = StorageChannelRegistry.INSTANCE.require(definition.type());
        var session = new DeepCellSession(stack, definition);

        return new DeepCellInventory(
                definition,
                channel,
                saveProvider,
                session,
                () -> CellWorkbenchSupport.filter(stack, definition)
        );
    }

}
