package top.likoslupus.ae2objects.cell.persistence;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry-aware serialization boundary between AE keys and the registry-agnostic SavedData model.
 */
public final class DeepCellStorageIo {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PREVIEW_SIZE = 10;

    private DeepCellStorageIo() {
    }

    public static LoadResult load(
            DeepCellStorage storage,
            HolderLookup.Provider registries,
            AEKeyType expectedKeyType
    ) {
        var amounts = storage.stackAmounts();
        var tags = storage.stackKeys();
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

            if (amount <= 0
                    || key == null
                    || !expectedKeyType.contains(key)
            ) {
                repaired = true;
                continue;
            }

            if (loaded.containsKey(key)) {
                repaired = true;
            }
            loaded.addTo(key, amount);
            total += amount;
        }

        if (total != storage.storedAmount()) {
            repaired = true;
        }

        return new LoadResult(loaded, repaired);
    }

    public static SaveResult save(
            Object2LongMap<AEKey> source,
            HolderLookup.Provider registries,
            String cellItemId
    ) {
        var keys = new ListTag();
        var amounts = new LongArrayList(source.size());
        var persisted = new Object2LongOpenHashMap<AEKey>();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var total = 0L;

        for (var entry : source.object2LongEntrySet()) {
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
            persisted.put(entry.getKey(), amount);
            total += amount;
        }

        var preview = createPreview(persisted);
        var storage = new DeepCellStorage(
                keys,
                amounts.toArray(new long[0]),
                total,
                Optional.of(cellItemId)
        );
        return new SaveResult(storage, persisted, preview);
    }

    public static List<GenericStack> createPreview(Object2LongMap<AEKey> amounts) {
        var preview = amounts.object2LongEntrySet().stream()
                .filter(entry -> entry.getLongValue() > 0)
                .map(entry ->
                        new GenericStack(entry.getKey(), entry.getLongValue())
                )
                .sorted(Comparator.comparingLong(GenericStack::amount).reversed())
                .collect(Collectors.toCollection(
                        () -> new ArrayList<>(amounts.size())
                ));
        if (preview.size() > PREVIEW_SIZE) {
            preview.subList(PREVIEW_SIZE, preview.size()).clear();
        }
        return List.copyOf(preview);
    }

    public record LoadResult(
            Object2LongMap<AEKey> amounts,
            boolean repaired
    ) {

    }

    public record SaveResult(
            DeepCellStorage storage,
            Object2LongMap<AEKey> amounts,
            List<GenericStack> preview
    ) {

    }

}
