package top.likoslupus.ae2objects.integration.ae2;

import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;

/**
 * Registers the runtime storage channels provided by the required AE2 dependency.
 *
 * <p>Runs during mod construction, before any content is registered or any inventory can be
 * created.</p>
 */
public final class Ae2Bootstrap {

    private Ae2Bootstrap() {
    }

    public static void bootstrapRequired() {
        var registry = StorageChannelRegistry.INSTANCE;
        registry.register(new ItemChannelBinding());
        registry.register(new FluidChannelBinding());
    }

}
