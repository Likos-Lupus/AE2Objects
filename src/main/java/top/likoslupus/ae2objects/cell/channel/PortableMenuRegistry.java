package top.likoslupus.ae2objects.cell.channel;

import net.minecraft.world.inventory.MenuType;
import top.likoslupus.ae2objects.cell.model.CellContentType;

import java.util.EnumMap;
import java.util.Optional;

/**
 * Registry of the portable-cell menus available in the current runtime.
 *
 * <p>Required AE2 menus are registered during bootstrap; optional menus are registered only when
 * their mod is present. Portable registration asks this registry so no item hard-codes a menu.</p>
 */
public final class PortableMenuRegistry {

    public static final PortableMenuRegistry INSTANCE = new PortableMenuRegistry();

    private final EnumMap<CellContentType, MenuType<?>> menus =
            new EnumMap<>(CellContentType.class);

    private PortableMenuRegistry() {
    }

    public void register(PortableMenuBinding binding) {
        menus.put(binding.type(), binding.menuType());
    }

    public Optional<MenuType<?>> find(CellContentType type) {
        return Optional.ofNullable(menus.get(type));
    }

    public boolean isAvailable(CellContentType type) {
        return menus.containsKey(type);
    }

    public MenuType<?> require(CellContentType type) {
        var menu = menus.get(type);
        if (menu == null) {
            throw new IllegalStateException("No portable menu registered for " + type);
        }
        return menu;
    }

}
