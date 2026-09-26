package top.likoslupus.ae2objects.cell.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import top.likoslupus.ae2objects.Ae2Objects;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * World-scoped repository for UUID-addressed deep-cell contents.
 */
public final class DeepStorageManager extends SavedData {

    public static final String MANAGER_NAME = "storage_manager";

    public static final Codec<DeepStorageManager> CODEC = RecordCodecBuilder
            .create(instance -> instance.group(
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, DeepCellStorage.CODEC)
                                    .fieldOf("cells")
                                    .forGetter(DeepStorageManager::cellsForCodec)
                    )
                    .apply(instance, DeepStorageManager::new)
            );

    public static final SavedDataType<DeepStorageManager> TYPE = new SavedDataType<>(
            Ae2Objects.id(MANAGER_NAME),
            DeepStorageManager::new,
            CODEC
    );

    private final Map<UUID, DeepCellStorage> cells;
    private @Nullable WeakReference<HolderLookup.Provider> registries;

    public DeepStorageManager() {
        this.cells = new HashMap<>();
    }

    private DeepStorageManager(Map<UUID, DeepCellStorage> cells) {
        this.cells = new HashMap<>(cells);
    }

    public static DeepStorageManager getInstance(MinecraftServer server) {
        var manager = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        manager.registries = new WeakReference<>(server.registryAccess());
        return manager;
    }

    private Map<UUID, DeepCellStorage> cellsForCodec() {
        return Map.copyOf(cells);
    }

    public Optional<DeepCellStorage> findCell(UUID uuid) {
        return Optional.ofNullable(cells.get(uuid));
    }

    public boolean contains(UUID uuid) {
        return cells.containsKey(uuid);
    }

    public DeepCellStorage getOrCreateCell(UUID uuid, String cellItemId) {
        var current = cells.get(uuid);
        if (current == null) {
            var created = emptyCell(cellItemId);
            cells.put(uuid, created);
            setDirty();
            return created;
        }

        var associated = current.withCellItemIdIfMissing(cellItemId);
        if (associated != current) {
            cells.put(uuid, associated);
            setDirty();
        }
        return associated;
    }

    public DeepCellStorage emptyCell(String cellItemId) {
        return DeepCellStorage.empty(cellItemId);
    }

    public void updateCell(UUID uuid, DeepCellStorage storage) {
        cells.put(uuid, storage);
        setDirty();
    }

    public void removeCell(UUID uuid) {
        if (cells.remove(uuid) != null) {
            setDirty();
        }
    }

    public HolderLookup.Provider registries() {
        var reference = registries;
        if (reference == null) {
            throw new IllegalStateException("DeepStorageManager has not been attached to a server");
        }

        var current = reference.get();
        if (current == null) {
            throw new IllegalStateException(
                    "DeepStorageManager belongs to a server that has stopped");
        }
        return current;
    }

}
