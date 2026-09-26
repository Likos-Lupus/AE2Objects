package top.likoslupus.ae2objects.integration.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import top.likoslupus.ae2objects.cell.channel.StorageChannelBinding;
import top.likoslupus.ae2objects.cell.model.CellContentType;

/** Binds the product-level fluid type to {@link AEKeyType#fluids()}. */
public final class FluidChannelBinding implements StorageChannelBinding {

    @Override
    public CellContentType type() {
        return CellContentType.FLUID;
    }

    @Override
    public AEKeyType keyType() {
        return AEKeyType.fluids();
    }

    @Override
    public boolean accepts(AEKey key) {
        return keyType().contains(key);
    }

}
