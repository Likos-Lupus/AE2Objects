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
            var cellsText = "text.ae2objects.deep_item_storage_cells";

            Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELLS.forEach(cell -> {
                Upgrades.add(AEItems.FUZZY_CARD, cell.get(), 1, cellsText);
                Upgrades.add(AEItems.INVERTER_CARD, cell.get(), 1, cellsText);
            });
        });
    }

    public static void initClient() {
        registerModel(
                Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_1K.get(),
                Ae2ObjectsItems.MODEL_DEEP_STORAGE_CELL_1K
        );
        registerModel(
                Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_4K.get(),
                Ae2ObjectsItems.MODEL_DEEP_STORAGE_CELL_4K
        );
        registerModel(
                Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_16K.get(),
                Ae2ObjectsItems.MODEL_DEEP_STORAGE_CELL_16K
        );
        registerModel(
                Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_64K.get(),
                Ae2ObjectsItems.MODEL_DEEP_STORAGE_CELL_64K
        );
        registerModel(
                Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_256K.get(),
                Ae2ObjectsItems.MODEL_DEEP_STORAGE_CELL_256K
        );
    }

}
