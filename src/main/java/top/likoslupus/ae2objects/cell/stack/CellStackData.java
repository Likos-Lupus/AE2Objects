package top.likoslupus.ae2objects.cell.stack;

import appeng.api.config.FuzzyMode;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.GenericStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.registry.ModDataComponents;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Centralizes the ItemStack data-component contract for deep cells.
 *
 * <p>This is the only class that knows the legacy component ids; everything else reads a
 * {@link CellStackSnapshot} or asks this class to publish one.</p>
 */
public final class CellStackData {

    private CellStackData() {
    }

    public static CellStackSnapshot snapshot(ItemStack stack) {
        return new CellStackSnapshot(
                cellId(stack),
                storedAmount(stack),
                storedTypes(stack),
                preview(stack),
                fuzzyMode(stack)
        );
    }

    public static @Nullable UUID cellId(ItemStack stack) {
        return stack.get(ModDataComponents.CELL_ID.get());
    }

    public static long storedAmount(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_AMOUNT.get(), 0L);
    }

    public static int storedTypes(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_TYPE_COUNT.get(), 0);
    }

    public static List<GenericStack> preview(ItemStack stack) {
        return stack.getOrDefault(AEComponents.STORAGE_CELL_INV, List.of());
    }

    public static FuzzyMode fuzzyMode(ItemStack stack) {
        return stack.getOrDefault(
                ModDataComponents.FUZZY_MODE.get(),
                FuzzyMode.IGNORE_ALL
        );
    }

    public static void updatePreview(ItemStack stack, List<GenericStack> preview) {
        if (preview.isEmpty()) {
            stack.remove(AEComponents.STORAGE_CELL_INV);
        } else {
            stack.set(AEComponents.STORAGE_CELL_INV, preview);
        }
    }

    public static void clearStorageIdentity(ItemStack stack) {
        stack.remove(ModDataComponents.CELL_ID.get());
        updateSummary(stack, 0L, 0);
        stack.remove(AEComponents.STORAGE_CELL_INV);
    }

    public static void updateSummary(
            ItemStack stack,
            long storedAmount,
            int storedTypes
    ) {
        stack.set(ModDataComponents.STORED_AMOUNT.get(), storedAmount);
        stack.set(ModDataComponents.STORED_TYPE_COUNT.get(), storedTypes);
    }

    public static String registeredItemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

}
