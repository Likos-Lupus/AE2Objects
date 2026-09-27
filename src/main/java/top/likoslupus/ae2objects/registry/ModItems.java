package top.likoslupus.ae2objects.registry;

import appeng.api.config.FuzzyMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.channel.PortableMenuRegistry;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.item.DeepDriveCellItem;
import top.likoslupus.ae2objects.cell.item.DeepPortableCellItem;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.platform.IntegrationSet;

/**
 * Content registration and catalog.
 *
 * <p>Storage-cell families are registered from
 * {@link top.likoslupus.ae2objects.cell.model.DeepCellCatalog} definitions so tier additions do not
 * need parallel field/list/model/update edits. Optional integrations can use the same registration
 * method during mod construction while keeping their foreign API references isolated.</p>
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
    public static final DeferredItem<Item> DEEP_FLUID_CELL_HOUSING = ITEMS.register(
            "deep_fluid_cell_housing",
            key -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, key))
                    .stacksTo(64)
                    .fireResistant())
    );

    /**
     * Chemical housing. Always registered as an item so its model is valid, but only exposed (tab,
     * recipes) once the Applied Mekanistics chemical channel exists.
     */
    public static final DeferredItem<Item> DEEP_CHEMICAL_CELL_HOUSING = ITEMS.register(
            "deep_chemical_cell_housing",
            key -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, key))
                    .stacksTo(64)
                    .fireResistant())
    );

    /** Default dye tint of portable cells (matches AE2's portable-cell colour). */
    private static final int PORTABLE_DEFAULT_COLOR = 0x80CAFF;

    private ModItems() {
    }

    public static void defineContent(IntegrationSet integrations) {
        RegisteredHousings.put(CellContentType.ITEM, DEEP_ITEM_CELL_HOUSING);
        RegisteredHousings.put(CellContentType.FLUID, DEEP_FLUID_CELL_HOUSING);
        if (StorageChannelRegistry.INSTANCE.isAvailable(CellContentType.CHEMICAL)) {
            RegisteredHousings.put(CellContentType.CHEMICAL, DEEP_CHEMICAL_CELL_HOUSING);
        }
        CellRegistrationPlan.activeDriveCells(integrations).forEach(ModItems::registerDriveCell);
        CellRegistrationPlan.activePortableCells(integrations)
                .forEach(ModItems::registerPortableCell);
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

    private static void registerPortableCell(CellDefinition definition) {
        var menu = PortableMenuRegistry.INSTANCE.require(definition.type());
        var component = CellComponentSources.forTier(definition.tier());
        var housing = RegisteredHousings.require(definition.type());
        var item = ITEMS.register(
                definition.itemId(),
                key -> new DeepPortableCellItem(
                        menu,
                        portableProperties(ResourceKey.create(Registries.ITEM, key), definition),
                        PORTABLE_DEFAULT_COLOR,
                        definition,
                        () -> BuiltInRegistries.ITEM
                                .getOptional(component.itemId())
                                .orElse(Items.AIR),
                        housing,
                        definition.translationKey()
                )
        );
        RegisteredCells.put(definition, item);
    }

    private static Item.Properties portableProperties(
            ResourceKey<Item> id,
            CellDefinition definition
    ) {
        var properties = new Item.Properties()
                .setId(id)
                .stacksTo(1)
                .fireResistant()
                .component(ModDataComponents.STORED_AMOUNT.get(), 0L)
                .component(ModDataComponents.STORED_TYPE_COUNT.get(), 0);
        if (definition.type().supportsFuzzy()) {
            properties.component(ModDataComponents.FUZZY_MODE.get(), FuzzyMode.IGNORE_ALL);
        }
        return properties;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
