package top.likoslupus.ae2objects.integration.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import top.likoslupus.ae2objects.cell.channel.StorageChannelBinding;
import top.likoslupus.ae2objects.cell.model.CellContentType;

/** Binds the product-level item type to {@link AEKeyType#items()}. */
public final class ItemChannelBinding implements StorageChannelBinding {

    @Override
    public CellContentType type() {
        return CellContentType.ITEM;
    }

    @Override
    public AEKeyType keyType() {
        return AEKeyType.items();
    }

    @Override
    public boolean accepts(AEKey key) {
        return keyType().contains(key);
    }

}
