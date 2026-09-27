package top.likoslupus.ae2objects.cell.item;

import appeng.api.config.FuzzyMode;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.ItemUpgradesChanged;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.CellUpgradeProfile;
import top.likoslupus.ae2objects.cell.storage.DeepCellFilter;
import top.likoslupus.ae2objects.registry.ModDataComponents;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Shared workbench bridge for drive and portable deep cells.
 *
 * <p>Resolves the runtime channel and upgrade profile from the definition so that no item class
 * needs its own copy of those rules.</p>
 */
public final class CellWorkbenchSupport {

    private CellWorkbenchSupport() {
    }

    public static void setFuzzyMode(
            ItemStack stack,
            CellDefinition definition,
            FuzzyMode mode
    ) {
        if (definition.type().supportsFuzzy()) {
            stack.set(ModDataComponents.FUZZY_MODE.get(), mode);
        }
    }

    public static DeepCellFilter filter(
            ItemStack stack,
            CellDefinition definition
    ) {
        return DeepCellFilter.create(
                config(stack, definition),
                upgrades(stack, definition),
                fuzzyMode(stack, definition),
                definition.type().supportsFuzzy()
        );
    }

    public static ConfigInventory config(ItemStack stack, CellDefinition definition) {
        var keyType = StorageChannelRegistry.INSTANCE.require(definition.type()).keyType();
        return CellConfig.create(Set.of(keyType), stack);
    }

    public static IUpgradeInventory upgrades(ItemStack stack, CellDefinition definition) {
        return upgrades(stack, definition, null);
    }

    public static FuzzyMode fuzzyMode(ItemStack stack, CellDefinition definition) {
        return definition.type().supportsFuzzy()
                ?
                stack.getOrDefault(
                        ModDataComponents.FUZZY_MODE.get(),
                        FuzzyMode.IGNORE_ALL
                )
                : FuzzyMode.IGNORE_ALL;
    }

    public static IUpgradeInventory upgrades(
            ItemStack stack,
            CellDefinition definition,
            @Nullable ItemUpgradesChanged onChanged
    ) {
        var slots = CellUpgradeProfile.forDefinition(definition).totalSlots();
        return onChanged == null
                ? UpgradeInventories.forItem(stack, slots)
                : UpgradeInventories.forItem(stack, slots, onChanged);
    }

}
