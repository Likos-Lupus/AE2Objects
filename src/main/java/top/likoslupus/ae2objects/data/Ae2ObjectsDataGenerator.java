package top.likoslupus.ae2objects.data;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
//? if 26.3
//import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import top.likoslupus.ae2objects.Ae2Objects;

@EventBusSubscriber(modid = Ae2Objects.MOD_ID)
public class Ae2ObjectsDataGenerator {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        //? if <26.3
        event.createProvider(CraftingRecipeProvider.Runner::new);
        // TODO: MEGA Cells 26.x 可用后恢复。
        // event.createProvider(MegaRecipeProvider::new);
        event.createProvider(GameTestStructureProvider::new);
    }

    //? if 26.3 {
    /*@SubscribeEvent
    public static void onGatherDataRegistryEntries(GatherDataRegistryEntriesEvent event) {
        event.recipe(CraftingRecipeProvider::new);
    }
    *///?}

}
