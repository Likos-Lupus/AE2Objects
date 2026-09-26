package top.likoslupus.ae2objects.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;
import top.likoslupus.ae2objects.cell.DeepCellSpec;
import top.likoslupus.ae2objects.cell.item.DeepStorageCellItem;

import java.util.function.Supplier;

/** Metadata shared by item registration, AE2 integration, creative tabs and data generation. */
public record DeepCellRegistration(
        String id,
        String family,
        DeepCellSpec spec,
        DeferredItem<DeepStorageCellItem> item,
        Supplier<? extends ItemLike> coreItem,
        Supplier<? extends ItemLike> housingItem,
        Identifier driveModel,
        String familyTranslationKey
) {

}
