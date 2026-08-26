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

    public static final DeferredItem<Item> DISK_HOUSING = ITEMS.register(
            "disk_housing",
            key -> new Item(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, key))
                            .stacksTo(64)
                            .fireResistant()
            )
    );
    public static final DeferredItem<Item> DISK_DRIVE_1K = ITEMS.register(
            "disk_drive_1k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_1K.asItem(),
                    1,
                    0.5f
            )
    );
    public static final DeferredItem<Item> DISK_DRIVE_4K = ITEMS.register(
            "disk_drive_4k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_4K.asItem(),
                    4,
                    1.0f
            )
    );
    public static final DeferredItem<Item> DISK_DRIVE_16K = ITEMS.register(
            "disk_drive_16k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_16K.asItem(),
                    16,
                    1.5f
            )
    );
    public static final DeferredItem<Item> DISK_DRIVE_64K = ITEMS.register(
            "disk_drive_64k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_64K.asItem(),
                    64,
                    2.0f
            )
    );
    public static final DeferredItem<Item> DISK_DRIVE_256K = ITEMS.register(
            "disk_drive_256k",
            key -> new DiskDriveItem(
                    ResourceKey.create(Registries.ITEM, key),
                    AEItems.CELL_COMPONENT_256K.asItem(),
                    256,
                    2.5f
            )
    );

    public static final List<Supplier<Item>> DISK_DRIVES = List.of(
            DISK_DRIVE_1K,
            DISK_DRIVE_4K,
            DISK_DRIVE_16K,
            DISK_DRIVE_64K,
            DISK_DRIVE_256K
    );

    public static final Identifier MODEL_DISK_DRIVE_1K = id("model/drive/cells/disk_1k");
    public static final Identifier MODEL_DISK_DRIVE_4K = id("model/drive/cells/disk_4k");
    public static final Identifier MODEL_DISK_DRIVE_16K = id("model/drive/cells/disk_16k");
    public static final Identifier MODEL_DISK_DRIVE_64K = id("model/drive/cells/disk_64k");
    public static final Identifier MODEL_DISK_DRIVE_256K = id("model/drive/cells/disk_256k");

    private Ae2ObjectsItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
