package top.likoslupus.ae2objects.registry;

import appeng.core.definitions.AEItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.item.DiskDriveItem;

import java.util.List;
import java.util.function.Supplier;

import static top.likoslupus.ae2objects.Ae2Objects.id;

public final class Ae2ObjectsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ae2Objects.MOD_ID);

    public static final DeferredItem<Item> DEEP_ITEM_CELL_HOUSING = ITEMS.register(
            "deep_item_cell_housing",
            key -> new Item(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, key))
                            .stacksTo(64)
                            .fireResistant()
            )
    );
    public static final DeferredItem<Item> DEEP_ITEM_STORAGE_CELL_1K = ITEMS.register(
            "deep_item_storage_cell_1k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_1K.asItem(),
                    1,
                    0.5f
            )
    );
    public static final DeferredItem<Item> DEEP_ITEM_STORAGE_CELL_4K = ITEMS.register(
            "deep_item_storage_cell_4k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_4K.asItem(),
                    4,
                    1.0f
            )
    );
    public static final DeferredItem<Item> DEEP_ITEM_STORAGE_CELL_16K = ITEMS.register(
            "deep_item_storage_cell_16k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_16K.asItem(),
                    16,
                    1.5f
            )
    );
    public static final DeferredItem<Item> DEEP_ITEM_STORAGE_CELL_64K = ITEMS.register(
            "deep_item_storage_cell_64k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_64K.asItem(),
                    64,
                    2.0f
            )
    );
    public static final DeferredItem<Item> DEEP_ITEM_STORAGE_CELL_256K = ITEMS.register(
            "deep_item_storage_cell_256k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_256K.asItem(),
                    256,
                    2.5f
            )
    );

    public static final List<Supplier<Item>> DEEP_ITEM_STORAGE_CELLS = List.of(
            DEEP_ITEM_STORAGE_CELL_1K,
            DEEP_ITEM_STORAGE_CELL_4K,
            DEEP_ITEM_STORAGE_CELL_16K,
            DEEP_ITEM_STORAGE_CELL_64K,
            DEEP_ITEM_STORAGE_CELL_256K
    );

    public static final Identifier MODEL_DEEP_STORAGE_CELL_1K = id(
            "block/drive/cells/deep_item_storage_cell_1k"
    );
    public static final Identifier MODEL_DEEP_STORAGE_CELL_4K = id(
            "block/drive/cells/deep_item_storage_cell_4k"
    );
    public static final Identifier MODEL_DEEP_STORAGE_CELL_16K = id(
            "block/drive/cells/deep_item_storage_cell_16k"
    );
    public static final Identifier MODEL_DEEP_STORAGE_CELL_64K = id(
            "block/drive/cells/deep_item_storage_cell_64k"
    );
    public static final Identifier MODEL_DEEP_STORAGE_CELL_256K = id(
            "block/drive/cells/deep_item_storage_cell_256k"
    );

    private Ae2ObjectsItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
