package top.likoslupus.ae2objects.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.ae2objects.cell.DeepCellItem;

@Mixin(AbstractContainerMenu.class)
public abstract class DeepCellCopyMixin {

    @Final
    @Shadow
    public NonNullList<Slot> slots;

    @Inject(
            method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/inventory/Slot;hasItem()Z",
                            ordinal = 1
                    )
            ),
            cancellable = true
    )
    private void ae2objects$copyDeepCellIndependently(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo callback
    ) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return;
        }

        var stack = slots.get(slotIndex).getItem();
        if (stack.getItem() instanceof DeepCellItem deepCell) {
            setCarried(deepCell.copyWithIndependentStorage(stack));
            callback.cancel();
        }
    }

    @Shadow
    public abstract void setCarried(ItemStack carried);

}
