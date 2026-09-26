package top.likoslupus.ae2objects.cell.channel;

import top.likoslupus.ae2objects.cell.model.CellContentType;

import java.util.EnumMap;
import java.util.Optional;

/**
 * Registry of the storage channels available in the current runtime.
 *
 * <p>Required AE2 channels are registered during bootstrap; optional channels are registered only
 * when their mod is present. The rest of the mod only ever asks the registry for a binding.</p>
 */
public final class StorageChannelRegistry {

    public static final StorageChannelRegistry INSTANCE = new StorageChannelRegistry();

    private final EnumMap<CellContentType, StorageChannelBinding> bindings =
            new EnumMap<>(CellContentType.class);

    private StorageChannelRegistry() {
    }

    public void register(StorageChannelBinding binding) {
        bindings.put(binding.type(), binding);
    }

    public Optional<StorageChannelBinding> find(CellContentType type) {
        return Optional.ofNullable(bindings.get(type));
    }

    public boolean isAvailable(CellContentType type) {
        return bindings.containsKey(type);
    }

    public StorageChannelBinding require(CellContentType type) {
        var binding = bindings.get(type);
        if (binding == null) {
            throw new IllegalStateException("No storage channel binding registered for " + type);
        }
        return binding;
    }

}
