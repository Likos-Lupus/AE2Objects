package top.likoslupus.ae2objects.integration.ae2;

import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.likoslupus.ae2objects.cell.inventory.DeepCellHandler;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;

import static appeng.api.client.StorageCellModels.registerModel;

/** Required Applied Energistics 2 bindings. */
public final class Ae2Integration {

    private Ae2Integration() {
    }

    public static void initCommon(FMLCommonSetupEvent event) {
        StorageCells.addCellHandler(DeepCellHandler.INSTANCE);

        event.enqueueWork(() ->
                Ae2ObjectsItems.storageCells()
                        .forEach(registration -> {
                            if (registration.spec().supportsFuzzy()) {
                                Upgrades.add(
                                        AEItems.FUZZY_CARD,
                                        registration.item().get(),
                                        1,
                                        registration.familyTranslationKey()
                                );
                            }
                            Upgrades.add(
                                    AEItems.INVERTER_CARD,
                                    registration.item().get(),
                                    1,
                                    registration.familyTranslationKey()
                            );
                        })
        );
    }

    public static void initClient() {
        Ae2ObjectsItems.storageCells()
                .forEach(registration ->
                        registerModel(
                                registration.item().get(),
                                registration.driveModel()
                        )
                );
    }

}
