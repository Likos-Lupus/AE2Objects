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
import top.likoslupus.ae2objects.storage.DiskCellItem;

@Mixin(AbstractContainerMenu.class)
public abstract class CursedInternalSlotMixin {

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
    public void cloneDisk(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo ci
    ) {
        var slot = this.slots.get(slotIndex);
        var stack = slot.getItem();
        if (stack.getItem() instanceof DiskCellItem diskCellItem) {
            var newStack = diskCellItem.clone(stack);
            this.setCarried(newStack);
            ci.cancel();
        }
    }

    @Shadow
    public abstract void setCarried(ItemStack carried);

}
