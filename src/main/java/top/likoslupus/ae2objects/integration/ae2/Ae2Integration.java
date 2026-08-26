package top.likoslupus.ae2objects.integration.ae2;

import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;
import top.likoslupus.ae2objects.storage.DiskCellHandler;

import static appeng.api.client.StorageCellModels.registerModel;

public final class Ae2Integration {

    private Ae2Integration() {
    }

    public static void initCommon(FMLCommonSetupEvent event) {
        StorageCells.addCellHandler(DiskCellHandler.INSTANCE);

        event.enqueueWork(() -> {
            var disksText = "text.ae2objects.disk_drives";

            Ae2ObjectsItems.DISK_DRIVES.forEach(cell -> {
                Upgrades.add(AEItems.FUZZY_CARD, cell.get(), 1, disksText);
                Upgrades.add(AEItems.INVERTER_CARD, cell.get(), 1, disksText);
            });
        });
    }

    public static void initClient() {
        registerModel(
                Ae2ObjectsItems.DISK_DRIVE_1K.get(),
                Ae2ObjectsItems.MODEL_DISK_DRIVE_1K
        );
        registerModel(
                Ae2ObjectsItems.DISK_DRIVE_4K.get(),
                Ae2ObjectsItems.MODEL_DISK_DRIVE_4K
        );
        registerModel(
                Ae2ObjectsItems.DISK_DRIVE_16K.get(),
                Ae2ObjectsItems.MODEL_DISK_DRIVE_16K
        );
        registerModel(
                Ae2ObjectsItems.DISK_DRIVE_64K.get(),
                Ae2ObjectsItems.MODEL_DISK_DRIVE_64K
        );
        registerModel(
                Ae2ObjectsItems.DISK_DRIVE_256K.get(),
                Ae2ObjectsItems.MODEL_DISK_DRIVE_256K
        );
    }

}
