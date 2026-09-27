package top.likoslupus.ae2objects.registry;

import appeng.core.definitions.AEItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.item.DeepDriveCellItem;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.cell.model.DeepCellCatalog;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Content registration and catalog.
 *
 * <p>Storage-cell families are registered from {@link DeepCellCatalog} definitions so tier
 * additions do not need parallel field/list/model/update edits. Optional integrations can use the
 * same registration method during mod construction while keeping their foreign API references
 * isolated.</p>
 */
public final class Ae2ObjectsItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Ae2Objects.MOD_ID);

    public static final DeferredItem<Item> DEEP_ITEM_CELL_HOUSING = ITEMS.register(
            "deep_item_cell_housing",
            key -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, key))
                    .stacksTo(64)
                    .fireResistant())
    );

    private static final List<DeepCellRegistration> STORAGE_CELLS = new ArrayList<>();
    private static final Map<CellTier, DeepCellRegistration> ITEM_STORAGE_CELLS =
            new EnumMap<>(CellTier.class);

    static {
        var components = Map.<CellTier, Supplier<? extends ItemLike>>of(
                CellTier.K1, AEItems.CELL_COMPONENT_1K::asItem,
                CellTier.K4, AEItems.CELL_COMPONENT_4K::asItem,
                CellTier.K16, AEItems.CELL_COMPONENT_16K::asItem,
                CellTier.K64, AEItems.CELL_COMPONENT_64K::asItem,
                CellTier.K256, AEItems.CELL_COMPONENT_256K::asItem
        );

        DeepCellCatalog.DRIVE_CELLS.stream()
                .filter(definition -> definition.type() == CellContentType.ITEM)
                .forEach(definition -> {
                    var registration = registerDeepStorageCell(
                            definition,
                            components.get(definition.tier()),
                            DEEP_ITEM_CELL_HOUSING
                    );
                    ITEM_STORAGE_CELLS.put(definition.tier(), registration);
                });
    }

    private Ae2ObjectsItems() {
    }

    public static DeepCellRegistration registerDeepStorageCell(
            CellDefinition definition,
            Supplier<? extends ItemLike> coreItem,
            Supplier<? extends ItemLike> housingItem
    ) {
        var item = ITEMS.register(
                definition.itemId(),
                key -> new DeepDriveCellItem(
                        ResourceKey.create(Registries.ITEM, key),
                        coreItem,
                        housingItem,
                        definition,
                        definition.translationKey()
                )
        );

        var registration = new DeepCellRegistration(
                definition,
                item,
                coreItem,
                housingItem
        );
        STORAGE_CELLS.add(registration);
        return registration;
    }

    public static List<DeepCellRegistration> storageCells() {
        return List.copyOf(STORAGE_CELLS);
    }

    public static DeepCellRegistration itemStorageCell(CellTier tier) {
        var registration = ITEM_STORAGE_CELLS.get(tier);
        if (registration == null) {
            throw new IllegalArgumentException("No item deep cell registered for tier " + tier);
        }
        return registration;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
