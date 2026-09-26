package top.likoslupus.ae2objects.cell.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Arrays;
import java.util.Optional;

/**
 * Persisted representation of one deep cell.
 *
 * <p>The legacy NBT field names are intentionally retained for world compatibility. The optional
 * {@code cell_item} field records the source cell registry id so recovery can restore the correct
 * family/tier/form.</p>
 */
public record CellRecord(
        ListTag stackKeys,
        long[] stackAmounts,
        long storedAmount,
        Optional<String> cellItemId
) {

    public static final Codec<CellRecord> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    CompoundTag.CODEC.fieldOf("data")
                            .forGetter(CellRecord::toNbt),
                    Codec.STRING.optionalFieldOf("cell_item")
                            .forGetter(CellRecord::cellItemId)
            )
            .apply(instance, CellRecord::fromNbt)
    );

    private static final String STACK_KEYS = "keys";
    private static final String STACK_AMOUNTS = "amts";
    private static final String STORED_AMOUNT_TAG = "item_count";

    public CellRecord {
        stackKeys = stackKeys.copy();
        stackAmounts = Arrays.copyOf(stackAmounts, stackAmounts.length);
        storedAmount = Math.max(0L, storedAmount);
    }

    public static CellRecord empty(String itemId) {
        return new CellRecord(
                new ListTag(),
                new long[0],
                0L,
                Optional.of(itemId)
        );
    }

    public static CellRecord empty() {
        return new CellRecord(
                new ListTag(),
                new long[0],
                0L,
                Optional.empty()
        );
    }

    public static CellRecord fromNbt(CompoundTag nbt) {
        return fromNbt(nbt, Optional.empty());
    }

    private static CellRecord fromNbt(
            CompoundTag nbt,
            Optional<String> cellItemId
    ) {
        var stackKeys = nbt.getList(STACK_KEYS).orElseGet(ListTag::new);
        var stackAmounts = nbt.getLongArray(STACK_AMOUNTS).orElse(new long[0]);
        var storedAmount = nbt.getLongOr(STORED_AMOUNT_TAG, 0L);
        return new CellRecord(stackKeys, stackAmounts, storedAmount, cellItemId);
    }

    @Override
    public ListTag stackKeys() {
        return stackKeys.copy();
    }

    @Override
    public long[] stackAmounts() {
        return Arrays.copyOf(stackAmounts, stackAmounts.length);
    }

    public int storedTypesCount() {
        return stackAmounts.length;
    }

    public CellRecord withCellItemIdIfMissing(String itemId) {
        return cellItemId.isPresent()
                ? this
                : new CellRecord(stackKeys, stackAmounts, storedAmount, Optional.of(itemId));
    }

    public CompoundTag toNbt() {
        var nbt = new CompoundTag();
        nbt.put(STACK_KEYS, stackKeys.copy());
        nbt.putLongArray(STACK_AMOUNTS, Arrays.copyOf(stackAmounts, stackAmounts.length));
        if (storedAmount != 0) {
            nbt.putLong(STORED_AMOUNT_TAG, storedAmount);
        }
        return nbt;
    }

}
