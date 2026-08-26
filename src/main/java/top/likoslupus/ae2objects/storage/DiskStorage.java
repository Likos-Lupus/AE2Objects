package top.likoslupus.ae2objects.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Arrays;

public final class DiskStorage {

    private static final String STACK_KEYS = "keys";
    private static final String STACK_AMOUNTS = "amts";
    private static final String ITEM_COUNT_TAG = "item_count";

    public static final Codec<DiskStorage> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    CompoundTag.CODEC.fieldOf("data")
                            .forGetter(DiskStorage::toNbt)
            ).apply(
                    instance,
                    DiskStorage::fromNbt
            ));

    private ListTag stackKeys;
    private long[] stackAmounts;
    private long itemCount;

    public DiskStorage() {
        this.stackKeys = new ListTag();
        this.stackAmounts = new long[0];
        this.itemCount = 0;
    }

    public DiskStorage(
            ListTag stackKeys,
            long[] stackAmounts,
            long itemCount
    ) {
        this.stackKeys = stackKeys != null
                ? stackKeys.copy()
                : new ListTag();
        this.stackAmounts = stackAmounts != null
                ? Arrays.copyOf(stackAmounts, stackAmounts.length)
                : new long[0];
        this.itemCount = itemCount;
    }

    public ListTag getStackKeys() {
        return stackKeys.copy();
    }

    public long[] getStackAmounts() {
        return Arrays.copyOf(stackAmounts, stackAmounts.length);
    }

    public long getItemCount() {
        return itemCount;
    }

    public int getStoredTypesCount() {
        return stackAmounts.length;
    }

    public void update(
            ListTag newKeys,
            long[] newAmounts,
            long newItemCount
    ) {
        this.stackKeys = newKeys != null
                ? newKeys.copy()
                : new ListTag();
        this.stackAmounts = newAmounts != null
                ? Arrays.copyOf(newAmounts, newAmounts.length)
                : new long[0];
        this.itemCount = newItemCount;
    }

    public DiskStorage copy() {
        return new DiskStorage(this.stackKeys, this.stackAmounts, this.itemCount);
    }

    public static DiskStorage empty() {
        return new DiskStorage();
    }

    public CompoundTag toNbt() {
        var nbt = new CompoundTag();
        nbt.put(STACK_KEYS, stackKeys.copy());
        nbt.putLongArray(STACK_AMOUNTS, Arrays.copyOf(stackAmounts, stackAmounts.length));
        if (itemCount != 0) {
            nbt.putLong(ITEM_COUNT_TAG, itemCount);
        }
        return nbt;
    }

    public static DiskStorage fromNbt(CompoundTag nbt) {
        var stackKeys = nbt.getList(STACK_KEYS).orElseGet(ListTag::new);
        var stackAmounts = nbt.getLongArray(STACK_AMOUNTS).orElse(new long[0]);
        var itemCount = nbt.getLongOr(ITEM_COUNT_TAG, 0L);
        return new DiskStorage(stackKeys, stackAmounts, itemCount);
    }

}
