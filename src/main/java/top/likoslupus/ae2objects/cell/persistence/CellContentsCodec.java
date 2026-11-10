package top.likoslupus.ae2objects.cell.persistence;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
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
import java.util.stream.IntStream;

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
        var entries = Math.min(amounts.length, tags.size());
        var contents = new DeepCellContents(entries);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var total = 0L;

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

            if (contents.amount(key) > 0) {
                repaired = true;
            }
            contents.insert(key, amount);
            total += amount;
        }

        if (total != record.storedAmount()) {
            repaired = true;
        }

        return new Decoded(contents, repaired);
    }

    public static CellRecord encode(
            DeepCellContents contents,
            HolderLookup.Provider registries,
            String cellItemId
    ) {
        return encodeWithPreview(contents, registries, cellItemId).record();
    }

    public static Encoded encodeWithPreview(
            DeepCellContents contents,
            HolderLookup.Provider registries,
            String cellItemId
    ) {
        var keys = new ListTag();
        var amounts = new LongArrayList(contents.typeCount());
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var preview = new PreviewCollector();
        var total = 0L;

        for (var entry : contents.asMap().object2LongEntrySet()) {
            var amount = entry.getLongValue();
            if (amount <= 0) {
                continue;
            }

            preview.offer(entry.getKey(), amount);

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

        var record = new CellRecord(
                keys,
                amounts.toArray(new long[0]),
                total,
                Optional.of(cellItemId)
        );
        return new Encoded(record, preview.toList());
    }

    public static List<GenericStack> preview(DeepCellContents contents) {
        var preview = new PreviewCollector();
        contents.asMap().object2LongEntrySet().forEach(entry ->
                preview.offer(entry.getKey(), entry.getLongValue())
        );
        return preview.toList();
    }

    /**
     * Keeps only the largest {@link #PREVIEW_SIZE} entries while scanning, avoiding a full list
     * allocation and sort for cells with many types.
     */
    private static final class PreviewCollector {

        private final AEKey[] keys = new AEKey[PREVIEW_SIZE];
        private final long[] amounts = new long[PREVIEW_SIZE];
        private int size;

        void offer(AEKey key, long amount) {
            if (amount <= 0) {
                return;
            }

            if (size < PREVIEW_SIZE) {
                keys[size] = key;
                amounts[size] = amount;
                size++;
                return;
            }

            var minIndex = 0;
            for (var index = 1; index < size; index++) {
                if (amounts[index] < amounts[minIndex]) {
                    minIndex = index;
                }
            }

            if (amount > amounts[minIndex]) {
                keys[minIndex] = key;
                amounts[minIndex] = amount;
            }
        }

        List<GenericStack> toList() {
            return List.copyOf(
                    IntStream.range(0, size)
                            .mapToObj(index -> new GenericStack(keys[index], amounts[index]))
                            .sorted(Comparator.comparingLong(GenericStack::amount).reversed())
                            .collect(Collectors.toCollection(() -> new ArrayList<>(size)))
            );
        }

    }

    public record Decoded(
            DeepCellContents contents,
            boolean repaired
    ) {

    }

    public record Encoded(
            CellRecord record,
            List<GenericStack> preview
    ) {

    }

}
