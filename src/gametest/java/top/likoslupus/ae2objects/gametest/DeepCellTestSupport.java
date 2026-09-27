package top.likoslupus.ae2objects.gametest;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.CellForm;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.registry.RegisteredCells;

/**
 * Shared helpers for the deep-cell GameTests.
 */
final class DeepCellTestSupport {

    private DeepCellTestSupport() {
    }

    static CellDefinition definition(
            CellContentType type,
            CellTier tier,
            CellForm form
    ) {
        return new CellDefinition(type, tier, form);
    }

    static ItemStack drive(CellContentType type, CellTier tier) {
        return new ItemStack(RegisteredCells.require(
                definition(type, tier, CellForm.DRIVE)
        ).get());
    }

    static ItemStack portable(CellContentType type, CellTier tier) {
        return new ItemStack(RegisteredCells.require(
                definition(type, tier, CellForm.PORTABLE)
        ).get());
    }

    static StorageCell inventory(ItemStack stack) {
        var inventory = StorageCells.getCellInventory(stack, null);
        if (inventory == null) {
            throw new IllegalStateException("No cell inventory for " + stack);
        }
        return inventory;
    }

    static long stored(StorageCell inventory, AEKey key) {
        var counter = new KeyCounter();
        inventory.getAvailableStacks(counter);
        return counter.get(key);
    }

    static void assertTrue(
            GameTestHelper helper,
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }

    static void assertAmount(
            GameTestHelper helper,
            StorageCell inventory,
            AEKey key,
            long expected,
            String message
    ) {
        var actual = stored(inventory, key);
        assertTrue(
                helper,
                actual == expected,
                message + " (expected " + expected + ", got " + actual + ")"
        );
    }

}
