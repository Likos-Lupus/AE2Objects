package top.likoslupus.ae2objects.cell.persistence;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;
import top.likoslupus.ae2objects.cell.storage.DeepCellContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry-aware serialization boundary between AE keys and the registry-agnostic
 * {@link CellRecord}.
 */
public final class CellContentsCodec {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PREVIEW_SIZE = 10;

    private CellContentsCodec() {
    }

    public static Decoded decode(
            CellRecord record,
            HolderLookup.Provider registries,
            AEKeyType expectedKeyType
    ) {
        var amounts = record.stackAmounts();
        var tags = record.stackKeys();
        var repaired = amounts.length != tags.size();
        var loaded = new Object2LongOpenHashMap<AEKey>();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var total = 0L;

        var entries = Math.min(amounts.length, tags.size());
        for (var index = 0; index < entries; index++) {
            var amount = amounts[index];
            var key = AEKey.CODEC
                    .parse(ops, tags.getCompoundOrEmpty(index))
                    .result()
                    .orElse(null);

            if (amount <= 0 || key == null || !expectedKeyType.contains(key)) {
                repaired = true;
                continue;
            }

            if (loaded.containsKey(key)) {
                repaired = true;
            }
            loaded.mergeLong(key, amount, Long::sum);
            total += amount;
        }

        if (total != record.storedAmount()) {
            repaired = true;
        }

        return new Decoded(new DeepCellContents(loaded), repaired);
    }

    public static CellRecord encode(
            DeepCellContents contents,
            HolderLookup.Provider registries,
            String cellItemId
    ) {
        var keys = new ListTag();
        var amounts = new LongArrayList(contents.typeCount());
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var total = 0L;

        for (var entry : contents.asMap().object2LongEntrySet()) {
            var amount = entry.getLongValue();
            if (amount <= 0) {
                continue;
            }

            var encoded = AEKey.CODEC
                    .encodeStart(ops, entry.getKey())
                    .result()
                    .orElse(null);
            if (!(encoded instanceof CompoundTag compoundKey)) {
                LOGGER.error(
                        "Failed to serialize deep-cell key {}; dropping the invalid entry",
                        entry.getKey()
                );
                continue;
            }

            keys.add(compoundKey);
            amounts.add(amount);
            total += amount;
        }

        return new CellRecord(
                keys,
                amounts.toArray(new long[0]),
                total,
                Optional.of(cellItemId)
        );
    }

    public static List<GenericStack> preview(DeepCellContents contents) {
        var preview = contents.asMap().object2LongEntrySet().stream()
                .filter(entry -> entry.getLongValue() > 0)
                .map(entry ->
                        new GenericStack(entry.getKey(), entry.getLongValue())
                )
                .sorted(Comparator.comparingLong(GenericStack::amount).reversed())
                .collect(Collectors.toCollection(
                        () -> new ArrayList<>(contents.typeCount())
                ));
        if (preview.size() > PREVIEW_SIZE) {
            preview.subList(PREVIEW_SIZE, preview.size()).clear();
        }
        return List.copyOf(preview);
    }

    public record Decoded(
            DeepCellContents contents,
            boolean repaired
    ) {

    }

}
