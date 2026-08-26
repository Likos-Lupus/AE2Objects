package top.likoslupus.ae2objects.data;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import top.likoslupus.ae2objects.Ae2Objects;

@EventBusSubscriber(modid = Ae2Objects.MOD_ID)
public class Ae2ObjectsDataGenerator {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        event.createProvider(CraftingRecipeProvider.Runner::new);
    }

}
