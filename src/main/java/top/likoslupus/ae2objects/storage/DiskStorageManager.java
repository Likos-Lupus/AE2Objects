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

public class DiskStorageManager extends SavedData {

    public static final String MANAGER_NAME = "storage_manager";

    public static final Codec<DiskStorageManager> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, DiskStorage.CODEC)
                            .fieldOf("disks")
                            .forGetter(DiskStorageManager::getDisks)
            ).apply(
                    instance,
                    DiskStorageManager::new
            ));

    public static final SavedDataType<DiskStorageManager> TYPE = new SavedDataType<>(
            Ae2Objects.id(MANAGER_NAME),
            DiskStorageManager::new,
            CODEC
    );

    private final Map<UUID, DiskStorage> disks;
    private @Nullable WeakReference<HolderLookup.Provider> registries;

    public DiskStorageManager() {
        this.disks = new HashMap<>();
        this.setDirty();
    }

    public DiskStorageManager(Map<UUID, DiskStorage> disks) {
        this.disks = new HashMap<>(disks);
        this.setDirty();
    }

    public Map<UUID, DiskStorage> getDisks() {
        return disks;
    }

    public void updateDisk(
            UUID uuid,
            DiskStorage dataStorage
    ) {
        disks.put(uuid, dataStorage);
        setDirty();
    }

    public void removeDisk(UUID uuid) {
        disks.remove(uuid);
        setDirty();
    }

    public boolean hasUUID(UUID uuid) {
        return disks.containsKey(uuid);
    }

    public DiskStorage getOrCreateDisk(UUID uuid) {
        return disks.computeIfAbsent(
                uuid,
                _ -> {
                    setDirty();
                    return new DiskStorage();
                }
        );
    }

    public void modifyDisk(
            UUID diskID,
            ListTag stackKeys,
            long[] stackAmounts,
            long itemCount
    ) {
        var diskToModify = getOrCreateDisk(diskID);
        diskToModify.update(stackKeys, stackAmounts, itemCount);
        updateDisk(diskID, diskToModify);
    }

    public static DiskStorageManager getInstance(MinecraftServer server) {
        var manager = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        manager.registries = new WeakReference<>(server.registryAccess());
        return manager;
    }

    public HolderLookup.Provider getRegistries() {
        var r = this.registries;
        if (r == null) {
            throw new IllegalStateException(
                    "DiskStorageManager was not initialized properly with registries."
            );
        }
        var currentRegistries = r.get();
        if (currentRegistries == null) {
            throw new IllegalStateException(
                    "Using a DiskStorageManager whose server was already closed"
            );
        }
        return currentRegistries;
    }

}
