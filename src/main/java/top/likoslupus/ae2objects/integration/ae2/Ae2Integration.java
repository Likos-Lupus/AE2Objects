package top.likoslupus.ae2objects.integration.ae2;

import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
                            if (profile.voidOverflow()) {
                                Upgrades.add(
                                        AEItems.VOID_CARD,
                                        item,
                                        1,
                                        definition.translationKey()
                                );
                            }
                            if (profile.maxEnergyCards() > 0) {
                                Upgrades.add(
                                        AEItems.ENERGY_CARD,
                                        item,
                                        profile.maxEnergyCards(),
                                        definition.translationKey()
                                );
                            }
                        })
        );
    }

    public static void initClient() {
        RegisteredCells.entries()
                .forEach(entry -> registerModel(
                        entry.getValue().get(),
                        entry.getKey().inDriveModelId()
                ));
    }

    /**
     * Exposes deep portable cells to other mods' energy chargers through the NeoForge energy
     * capability, mirroring AE2's {@code InitCapabilityProviders.initPoweredItem}.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        RegisteredCells.entries().forEach(entry -> {
            var item = entry.getValue().get();
            if (item instanceof IAEItemPowerStorage powerStorage) {
                event.registerItem(
                        Capabilities.Energy.ITEM,
                        (stack, itemAccess) -> new PoweredItemCapabilities(
                                itemAccess,
                                item,
                                powerStorage
                        ),
                        item
                );
            }
        });
    }

}
