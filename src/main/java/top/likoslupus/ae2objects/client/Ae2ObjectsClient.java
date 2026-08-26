package top.likoslupus.ae2objects.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.integration.ae2.Ae2Integration;

@Mod(
        value = Ae2Objects.MOD_ID,
        dist = Dist.CLIENT
)
public class Ae2ObjectsClient {

    public Ae2ObjectsClient(IEventBus eventBus) {
        eventBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        Ae2Integration.initClient();
    }

}
