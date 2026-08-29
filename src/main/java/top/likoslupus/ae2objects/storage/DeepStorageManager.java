package top.likoslupus.ae2objects.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import top.likoslupus.ae2objects.Ae2Objects;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public class DeepStorageManager extends SavedData {

    public static final String MANAGER_NAME = "storage_manager";

    public static final Codec<DeepStorageManager> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, DeepCellStorage.CODEC)
                            .fieldOf("cells")
                            .forGetter(DeepStorageManager::getCells)
            ).apply(
                    instance,
                    DeepStorageManager::new
            ));

    public static final SavedDataType<DeepStorageManager> TYPE = new SavedDataType<>(
            Ae2Objects.id(MANAGER_NAME),
            DeepStorageManager::new,
            CODEC
    );

    private final Map<UUID, DeepCellStorage> cells;
    private @Nullable WeakReference<HolderLookup.Provider> registries;

    public DeepStorageManager() {
        this.cells = new HashMap<>();
        this.setDirty();
    }

    public DeepStorageManager(Map<UUID, DeepCellStorage> cells) {
        this.cells = new HashMap<>(cells);
        this.setDirty();
    }

    public Map<UUID, DeepCellStorage> getCells() {
        return cells;
    }

    public void updateCell(
            UUID uuid,
            DeepCellStorage dataStorage
    ) {
        cells.put(uuid, dataStorage);
        setDirty();
    }

    public void removeCell(UUID uuid) {
        cells.remove(uuid);
        setDirty();
    }

    public boolean hasUUID(UUID uuid) {
        return cells.containsKey(uuid);
    }

    public DeepCellStorage getOrCreateCell(UUID uuid) {
        return cells.computeIfAbsent(
                uuid,
                _ -> {
                    setDirty();
                    return new DeepCellStorage();
                }
        );
    }

    public void modifyCell(
            UUID cellId,
            ListTag stackKeys,
            long[] stackAmounts,
            long itemCount
    ) {
        var cellToModify = getOrCreateCell(cellId);
        cellToModify.update(stackKeys, stackAmounts, itemCount);
        updateCell(cellId, cellToModify);
    }

    public static DeepStorageManager getInstance(MinecraftServer server) {
        var manager = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        manager.registries = new WeakReference<>(server.registryAccess());
        return manager;
    }

    public HolderLookup.Provider getRegistries() {
        var r = this.registries;
        if (r == null) {
            throw new IllegalStateException(
                    "DeepStorageManager was not initialized properly with registries."
            );
        }
        var currentRegistries = r.get();
        if (currentRegistries == null) {
            throw new IllegalStateException(
                    "Using a DeepStorageManager whose server was already closed"
            );
        }
        return currentRegistries;
    }

}
