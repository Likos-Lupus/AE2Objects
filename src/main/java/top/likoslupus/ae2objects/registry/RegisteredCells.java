package top.likoslupus.ae2objects.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import top.likoslupus.ae2objects.cell.model.CellDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The deep cells actually registered at runtime, addressed by their {@link CellDefinition}.
 *
 * <p>Keeps only the NeoForge registry handle; ids, models, translations and upgrades all derive
 * from the definition.</p>
 */
public final class RegisteredCells {

    private static final Map<CellDefinition, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();

    private RegisteredCells() {
    }

    public static void put(
            CellDefinition definition,
            DeferredItem<? extends Item> item
    ) {
        ITEMS.put(definition, item);
    }

    public static Optional<DeferredItem<? extends Item>> find(CellDefinition definition) {
        return Optional.ofNullable(ITEMS.get(definition));
    }

    public static DeferredItem<? extends Item> require(CellDefinition definition) {
        var item = ITEMS.get(definition);
        if (item == null) {
            throw new IllegalArgumentException(
                    "No deep cell registered for " + definition.itemId()
            );
        }
        return item;
    }

    public static List<CellDefinition> definitions() {
        return List.copyOf(ITEMS.keySet());
    }

    public static List<Map.Entry<CellDefinition, DeferredItem<? extends Item>>> entries() {
        return List.copyOf(ITEMS.entrySet());
    }

}
