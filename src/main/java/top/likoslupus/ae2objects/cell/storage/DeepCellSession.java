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
 *
 * <p>The server context is resolved lazily for the current thread. This keeps client-side
 * inventories on the lightweight item summary/preview path and avoids cross-thread access to the
 * world repository in single-player.</p>
 */
public final class DeepCellSession {

    private final ItemStack stack;
    private final CellDefinition definition;

    private @Nullable DeepCellContents contents;
    private boolean dirty;

    public DeepCellSession(
            ItemStack stack,
            CellDefinition definition
    ) {
        this.stack = stack;
        this.definition = definition;
        this.dirty = false;
    }

    public ItemStack stack() {
        return stack;
    }

    public CellDefinition definition() {
        return definition;
    }

    public boolean hasIdentity() {
        return cellId() != null;
    }

    public @Nullable UUID cellId() {
        return CellStackData.cellId(stack);
    }

    /**
     * Total stored amount without forcing a full key decode when possible.
     *
     * <p>Loaded contents use their incrementally maintained total. On the server, an unloaded cell
     * reads the persisted record total. On the client, the synchronized item summary is used.</p>
     */
    public long storedAmount() {
        var context = ServerCellContext.getForCurrentThread();
        if (context == null) {
            return knownStoredAmount();
        }

        if (contents != null) {
            return contents.totalAmount();
        }

        var id = cellId();
        if (id == null) {
            return 0L;
        }

        return context.repository().find(id)
                .map(CellRecord::storedAmount)
                .orElse(0L);
    }

    /** Cached summary stored on the item; cheap and always available, even on the client. */
    public long knownStoredAmount() {
        return CellStackData.storedAmount(stack);
    }

    public int storedTypes() {
        var context = ServerCellContext.getForCurrentThread();
        return context == null
                ? knownStoredTypes()
                : contents().typeCount();
    }

    public int knownStoredTypes() {
        return CellStackData.storedTypes(stack);
    }

    public DeepCellContents contents() {
        if (contents != null) {
            return contents;
        }

        var loaded = loadContents();
        if (loaded == null) {
            // Server thread, but the server context is not available yet (e.g. very early
            // lifecycle). Do not cache a preview here; retry once the context exists.
            return new DeepCellContents();
        }
        contents = loaded;
        return contents;
    }

    private @Nullable DeepCellContents loadContents() {
        var context = ServerCellContext.getForCurrentThread();
        if (context == null) {
            return ServerCellContext.isServerThread()
                    ? null
                    : DeepCellContents.fromPreview(stack, keyType());
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

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void persist() {
        var context = ServerCellContext.getForCurrentThread();
        if (!dirty || context == null) {
            return;
        }

        var current = contents();
        if (current.isEmpty()) {
            clearIdentity();
            return;
        }

        if (!ensureIdentity()) {
            return;
        }

        var id = requireNonNull(cellId());
        var encoded = CellContentsCodec.encodeWithPreview(
                current,
                context.registries(),
                CellStackData.registeredItemId(stack)
        );
        var record = encoded.record();
        context.repository().put(id, record);
        CellStackData.updateSummary(
                stack,
                record.storedAmount(),
                record.storedTypesCount()
        );
        CellStackData.updatePreview(
                stack,
                encoded.preview()
        );
        dirty = false;
    }

    public void clearIdentity() {
        var id = cellId();
        var context = ServerCellContext.getForCurrentThread();
        if (id != null && context != null) {
            context.repository().remove(id);
        }
        CellStackData.clearStorageIdentity(stack);
        contents = new DeepCellContents();
        dirty = false;
    }

    public boolean ensureIdentity() {
        if (cellId() != null) {
            return true;
        }

        var context = ServerCellContext.getForCurrentThread();
        if (context == null) {
            return false;
        }

        var id = UUID.randomUUID();
        stack.set(ModDataComponents.CELL_ID.get(), id);
        context.repository().put(
                id,
                CellRecord.empty(CellStackData.registeredItemId(stack))
        );
        return true;
    }

}
