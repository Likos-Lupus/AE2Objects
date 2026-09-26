package top.likoslupus.ae2objects.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;
import top.likoslupus.ae2objects.cell.item.DeepStorageCellItem;
import top.likoslupus.ae2objects.cell.model.CellDefinition;

import java.util.function.Supplier;

/**
 * Runtime registration handle for one deep cell, keyed by its {@link CellDefinition}.
 */
public record DeepCellRegistration(
        CellDefinition definition,
        DeferredItem<DeepStorageCellItem> item,
        Supplier<? extends ItemLike> coreItem,
        Supplier<? extends ItemLike> housingItem
) {

    public String id() {
        return definition.itemId();
    }

    public Identifier driveModel() {
        return definition.driveModelId();
    }

    public String familyTranslationKey() {
        return definition.translationKey();
    }

}
