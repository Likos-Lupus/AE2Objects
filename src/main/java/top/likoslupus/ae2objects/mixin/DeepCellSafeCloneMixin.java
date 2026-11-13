//? if >=26.2 {
/*package top.likoslupus.ae2objects.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.ae2objects.cell.item.CellCloneService;
import top.likoslupus.ae2objects.cell.item.DeepCellDefinitionProvider;

/^* 26.2 moved creative-menu cloning to {@link Slot#safeClone(Player)}. ^/
@Mixin(Slot.class)
public abstract class DeepCellSafeCloneMixin {

    @Inject(
            method = "safeClone",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ae2objects$cloneDeepCellIndependently(
            Player player,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        var stack = getItem();
        if (stack.getItem() instanceof DeepCellDefinitionProvider) {
            callback.setReturnValue(CellCloneService.copyIndependent(stack));
        }
    }

    @Shadow
    public abstract ItemStack getItem();

}
*///?}
