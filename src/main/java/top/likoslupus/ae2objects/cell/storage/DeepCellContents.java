package top.likoslupus.ae2objects.cell.storage;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Mutable in-memory contents of one deep cell.
 *
 * <p>This is the single source of truth for the loaded amounts of a cell; callers must not keep a
 * second copy of the total or type count.</p>
 */
public final class DeepCellContents {

    private final Object2LongOpenHashMap<AEKey> amounts;

    public DeepCellContents() {
        this.amounts = new Object2LongOpenHashMap<>();
    }

    public DeepCellContents(Object2LongMap<AEKey> source) {
        this.amounts = new Object2LongOpenHashMap<>(Math.max(1, source.size()));
        source.object2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() > 0) {
                this.amounts.put(entry.getKey(), entry.getLongValue());
            }
        });
    }

    public static DeepCellContents fromPreview(ItemStack stack, AEKeyType keyType) {
        var contents = new DeepCellContents();
        List<GenericStack> preview = stack.getOrDefault(
                AEComponents.STORAGE_CELL_INV,
                List.of()
        );
        preview.stream()
                .filter(entry ->
                        entry.amount() > 0 && keyType.contains(entry.what())
                )
                .forEach(entry ->
                        contents.amounts.put(entry.what(), entry.amount())
                );
        return contents;
    }

    public long amount(AEKey key) {
        return amounts.getLong(key);
    }

    public long totalAmount() {
        return amounts.values().longStream().sum();
    }

    public int typeCount() {
        return amounts.size();
    }

    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    public void insert(AEKey key, long amount) {
        if (amount > 0) {
            amounts.mergeLong(key, amount, Long::sum);
        }
    }

    public long extract(AEKey key, long amount) {
        if (amount <= 0) {
            return 0L;
        }

        var current = amounts.getLong(key);
        if (current <= 0) {
            return 0L;
        }

        var extracted = Math.min(amount, current);
        if (extracted == current) {
            amounts.removeLong(key);
        } else {
            amounts.put(key, current - extracted);
        }
        return extracted;
    }

    /** Live map view; intended for read-only use by the persistence codec and the inventory. */
    public Object2LongMap<AEKey> asMap() {
        return amounts;
    }

}
