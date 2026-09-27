package top.likoslupus.ae2objects.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.item.DeepDriveCellItem;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.platform.IntegrationSet;

/**
 * Content registration and catalog.
 *
 * <p>Storage-cell families are registered from
 * {@link top.likoslupus.ae2objects.cell.model.DeepCellCatalog} definitions so tier
 * additions do not need parallel field/list/model/update edits. Optional integrations can use the
 * same registration method during mod construction while keeping their foreign API references
 * isolated.</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Ae2Objects.MOD_ID);

    public static final DeferredItem<Item> DEEP_ITEM_CELL_HOUSING = ITEMS.register(
            "deep_item_cell_housing",
            key -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, key))
                    .stacksTo(64)
                    .fireResistant())
    );

    private ModItems() {
    }

    public static void defineContent(IntegrationSet integrations) {
        RegisteredHousings.put(CellContentType.ITEM, DEEP_ITEM_CELL_HOUSING);
        CellRegistrationPlan.activeDriveCells(integrations).forEach(ModItems::registerDriveCell);
    }

    private static void registerDriveCell(CellDefinition definition) {
        var component = CellComponentSources.forTier(definition.tier());
        var housing = RegisteredHousings.require(definition.type());
        var item = ITEMS.register(
                definition.itemId(),
                key -> new DeepDriveCellItem(
                        ResourceKey.create(Registries.ITEM, key),
                        () -> BuiltInRegistries.ITEM
                                .getOptional(component.itemId())
                                .orElse(Items.AIR),
                        housing,
                        definition,
                        definition.translationKey()
                )
        );
        RegisteredCells.put(definition, item);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
