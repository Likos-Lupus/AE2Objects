package top.likoslupus.ae2objects.cell.item;

import net.minecraft.server.level.ServerPlayer;
//? if 26.3
//import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Version bridge for inventory/drop APIs that gained a client/server prediction argument. */
public final class CellItemOutput {

    private CellItemOutput() {
    }

    public static void giveBack(Inventory inventory, ItemStack stack) {
        //? if 26.3 {
        /*inventory.placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
         *///?} else {
        inventory.placeItemBackInInventory(stack);
        //?}
    }

    public static void drop(ServerPlayer player, ItemStack stack) {
        //? if 26.3 {
        /*player.drop(stack, false, Prediction.SERVER_ONLY);
         *///?} else {
        player.drop(stack, false);
        //?}
    }

}
