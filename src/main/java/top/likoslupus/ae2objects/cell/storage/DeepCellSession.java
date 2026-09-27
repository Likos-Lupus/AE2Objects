package top.likoslupus.ae2objects.cell.storage;

import appeng.api.stacks.AEKeyType;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.persistence.CellContentsCodec;
import top.likoslupus.ae2objects.cell.persistence.CellRecord;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.ModDataComponents;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Server-side storage lifecycle for one deep-cell {@link ItemStack}.
 *
 * <p>Owns the UUID, lazy loading, the dirty flag, repository access and the published ItemStack
 * snapshot. It deliberately knows nothing about partitioning, fuzzy mode, capacity limits or the
 * AE2 {@code StorageCell} interface.</p>
 */
public final class DeepCellSession {

    private final ItemStack stack;
    private final CellDefinition definition;
    private final @Nullable ServerCellContext context;

    private @Nullable DeepCellContents contents;
    private boolean dirty;

    public DeepCellSession(
            ItemStack stack,
            CellDefinition definition,
            @Nullable ServerCellContext context
    ) {
        this.stack = stack;
        this.definition = definition;
        this.context = context;
        this.dirty = false;
    }

    public ItemStack stack() {
        return stack;
    }

    public CellDefinition definition() {
        return definition;
    }

    public @Nullable ServerCellContext context() {
        return context;
    }

    public boolean hasIdentity() {
        return cellId() != null;
    }

    public @Nullable UUID cellId() {
        return CellStackData.cellId(stack);
    }

    /** Cached summary stored on the item; cheap and always available, even on the client. */
    public long knownStoredAmount() {
        return CellStackData.storedAmount(stack);
    }

    public int knownStoredTypes() {
        return CellStackData.storedTypes(stack);
    }

    public long storedAmount() {
        return contents().totalAmount();
    }

    public DeepCellContents contents() {
        if (contents == null) {
            contents = loadContents();
        }
        return contents;
    }

    private DeepCellContents loadContents() {
        if (context == null) {
            return DeepCellContents.fromPreview(stack, keyType());
        }

        var id = cellId();
        if (id == null) {
            return new DeepCellContents();
        }

        var record = context.repository().find(id).orElse(null);
        if (record == null) {
            return new DeepCellContents();
        }

        var decoded = CellContentsCodec.decode(
                record,
                context.registries(),
                keyType()
        );
        if (decoded.repaired()) {
            dirty = true;
        }
        return decoded.contents();
    }

    public AEKeyType keyType() {
        return StorageChannelRegistry.INSTANCE.require(definition.type()).keyType();
    }

    public int storedTypes() {
        return contents().typeCount();
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void persist() {
        if (!dirty || context == null) {
            return;
        }

        var current = contents();
        if (current.isEmpty()) {
            clearIdentity();
            return;
        }

        ensureIdentity();
        var id = requireNonNull(cellId());
        var record = CellContentsCodec.encode(
                current,
                context.registries(),
                CellStackData.registeredItemId(stack)
        );
        context.repository().put(id, record);
        CellStackData.updateSummary(
                stack,
                current.totalAmount(),
                current.typeCount()
        );
        CellStackData.updatePreview(
                stack,
                CellContentsCodec.preview(current)
        );
        dirty = false;
    }

    public void clearIdentity() {
        var id = cellId();
        if (id != null && context != null) {
            context.repository().remove(id);
        }
        CellStackData.clearStorageIdentity(stack);
        contents = new DeepCellContents();
        dirty = false;
    }

    public void ensureIdentity() {
        if (cellId() != null) {
            return;
        }

        if (context == null) {
            throw new IllegalStateException(
                    "Cannot allocate a deep-cell UUID without server storage"
            );
        }

        var id = UUID.randomUUID();
        stack.set(ModDataComponents.CELL_ID.get(), id);
        context.repository().put(
                id,
                CellRecord.empty(CellStackData.registeredItemId(stack))
        );
    }

}
