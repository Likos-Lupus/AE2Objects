package top.likoslupus.ae2objects.cell.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import top.likoslupus.ae2objects.Ae2Objects;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link CellRepository} backed by a world-scoped {@link SavedData}.
 *
 * <p>Identity ({@code ae2objects:storage_manager}) and the legacy record layout are preserved.</p>
 */
public final class SavedDataCellRepository extends SavedData implements CellRepository {

    public static final String NAME = "storage_manager";

    public static final Codec<SavedDataCellRepository> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, CellRecord.CODEC)
                            .fieldOf("cells")
                            .forGetter(SavedDataCellRepository::cellsForCodec)
            )
            .apply(instance, SavedDataCellRepository::new)
    );

    public static final SavedDataType<SavedDataCellRepository> TYPE = new SavedDataType<>(
            Ae2Objects.id(NAME),
            SavedDataCellRepository::new,
            CODEC
    );

    private final Map<UUID, CellRecord> cells;

    public SavedDataCellRepository() {
        this.cells = new HashMap<>();
    }

    private SavedDataCellRepository(Map<UUID, CellRecord> cells) {
        this.cells = new HashMap<>(cells);
    }

    public static SavedDataCellRepository getInstance(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private Map<UUID, CellRecord> cellsForCodec() {
        return Map.copyOf(cells);
    }

    @Override
    public Optional<CellRecord> find(UUID id) {
        return Optional.ofNullable(cells.get(id));
    }

    @Override
    public boolean contains(UUID id) {
        return cells.containsKey(id);
    }

    @Override
    public void put(UUID id, CellRecord record) {
        cells.put(id, record);
        setDirty();
    }

    @Override
    public void remove(UUID id) {
        if (cells.remove(id) != null) {
            setDirty();
        }
    }

}
