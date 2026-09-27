package top.likoslupus.ae2objects.integration.ae2;

import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.likoslupus.ae2objects.cell.inventory.DeepCellHandler;
import top.likoslupus.ae2objects.cell.model.CellUpgradeProfile;
import top.likoslupus.ae2objects.registry.RegisteredCells;

import static appeng.api.client.StorageCellModels.registerModel;

/** Required Applied Energistics 2 bindings. */
public final class Ae2Integration {

    private Ae2Integration() {
    }

    public static void initCommon(FMLCommonSetupEvent event) {
        StorageCells.addCellHandler(DeepCellHandler.INSTANCE);

        event.enqueueWork(() ->
                RegisteredCells.entries()
                        .forEach(entry -> {
                            var definition = entry.getKey();
                            var item = entry.getValue().get();
                            var profile = CellUpgradeProfile.forDefinition(definition);

                            if (profile.fuzzy()) {
                                Upgrades.add(
                                        AEItems.FUZZY_CARD,
                                        item,
                                        1,
                                        definition.translationKey()
                                );
                            }
                            if (profile.inverter()) {
                                Upgrades.add(
                                        AEItems.INVERTER_CARD,
                                        item,
                                        1,
                                        definition.translationKey()
                                );
                            }
                        })
        );
    }

    public static void initClient() {
        RegisteredCells.entries()
                .forEach(entry -> {
                    var definition = entry.getKey();
                    if (definition.isDrive()) {
                        registerModel(
                                entry.getValue().get(),
                                definition.driveModelId()
                        );
                    }
                });
    }

}
