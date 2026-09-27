package top.likoslupus.ae2objects.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import top.likoslupus.ae2objects.cell.model.CellContentType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The craftable housing for each content type, addressed by {@link CellContentType}.
 */
public final class RegisteredHousings {

    private static final Map<CellContentType, DeferredItem<Item>> HOUSINGS =
            new EnumMap<>(CellContentType.class);

    private RegisteredHousings() {
    }

    public static void put(
            CellContentType type,
            DeferredItem<Item> item
    ) {
        HOUSINGS.put(type, item);
    }

    public static Optional<DeferredItem<Item>> find(CellContentType type) {
        return Optional.ofNullable(HOUSINGS.get(type));
    }

    public static DeferredItem<Item> require(CellContentType type) {
        var housing = HOUSINGS.get(type);
        if (housing == null) {
            throw new IllegalArgumentException("No deep-cell housing registered for " + type);
        }
        return housing;
    }

    public static List<DeferredItem<Item>> all() {
        return List.copyOf(HOUSINGS.values());
    }

}
