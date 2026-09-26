package top.likoslupus.ae2objects;

import appeng.api.ids.AECreativeTabIds;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import top.likoslupus.ae2objects.command.Ae2ObjectsCommand;
import top.likoslupus.ae2objects.integration.ae2.Ae2Bootstrap;
import top.likoslupus.ae2objects.integration.ae2.Ae2Integration;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;

@Mod(Ae2Objects.MOD_ID)
public final class Ae2Objects {

    public static final String MOD_ID = "ae2objects";

    public Ae2Objects(IEventBus modEventBus) {
        Ae2Bootstrap.bootstrapRequired();

        Ae2ObjectsItems.register(modEventBus);
        Ae2ObjectsDataComponents.register(modEventBus);

        modEventBus.addListener(Ae2Integration::initCommon);
        modEventBus.addListener(this::addContentsToCreativeTab);

        NeoForge.EVENT_BUS.addListener(Ae2ObjectsCommand::register);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    private void addContentsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(AECreativeTabIds.MAIN)) {
            return;
        }

        event.accept(Ae2ObjectsItems.DEEP_ITEM_CELL_HOUSING);
        Ae2ObjectsItems.storageCells()
                .forEach(registration ->
                        event.accept(registration.item().get())
                );
    }

    private void onServerStarted(ServerStartedEvent event) {
        ServerCellContext.onServerStarted(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ServerCellContext.onServerStopped(event.getServer());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}
