package top.likoslupus.ae2objects.data;

import com.google.common.hash.Hashing;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import top.likoslupus.ae2objects.Ae2Objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Emits the empty structure templates used by the GameTests.
 *
 * <p>Vanilla {@code minecraft:empty} is only 1x1x1, so the tests need a larger all-air plot. The
 * game test framework clears the plot (air above, stone below) before placing the template, so an
 * all-air template of the right size is sufficient; blocks are placed in code.</p>
 */
public final class GameTestStructureProvider implements DataProvider {

    private final PackOutput.PathProvider structures;

    public GameTestStructureProvider(PackOutput output) {
        this.structures = output.createPathProvider(PackOutput.Target.DATA_PACK, "structure");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        save(cache, Ae2Objects.id("deep_cell_plot"), 9, 6, 9);
        return CompletableFuture.completedFuture(null);
    }

    private void save(CachedOutput cache, Identifier id, int x, int y, int z) {
        var root = new CompoundTag();

        var size = new ListTag();
        size.add(IntTag.valueOf(x));
        size.add(IntTag.valueOf(y));
        size.add(IntTag.valueOf(z));
        root.put("size", size);

        var paletteEntry = new CompoundTag();
        paletteEntry.put("Name", StringTag.valueOf("minecraft:air"));
        var palette = new ListTag();
        palette.add(paletteEntry);
        root.put("palette", palette);

        root.put("blocks", new ListTag());
        root.put("entities", new ListTag());

        try {
            var out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, out);
            var bytes = out.toByteArray();
            cache.writeIfNeeded(
                    structures.file(id, "nbt"),
                    bytes,
                    Hashing.sha256().hashBytes(bytes)
            );
        } catch (IOException e) {
            throw new RuntimeException("Couldn't write game test structure " + id, e);
        }
    }

    @Override
    public String getName() {
        return "AE2Objects Game Test Structures";
    }

}
