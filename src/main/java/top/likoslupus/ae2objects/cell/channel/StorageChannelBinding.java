package top.likoslupus.ae2objects.cell.channel;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import top.likoslupus.ae2objects.cell.model.CellContentType;

/**
 * Runtime mapping from a product-level {@link CellContentType} to a concrete AE2 storage channel.
 *
 * <p>There is exactly one binding per content type. Foreign implementations (for example chemicals
 * backed by Applied Mekanistics) live in their integration package and are never referenced by the
 * core.</p>
 */
public interface StorageChannelBinding {

    CellContentType type();

    AEKeyType keyType();

    boolean accepts(AEKey key);

}
