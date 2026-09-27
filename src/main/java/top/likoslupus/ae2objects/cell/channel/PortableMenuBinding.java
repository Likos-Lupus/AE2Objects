package top.likoslupus.ae2objects.cell.channel;

import net.minecraft.world.inventory.MenuType;
import top.likoslupus.ae2objects.cell.model.CellContentType;

/**
 * The portable-cell menu used for a given storage type.
 *
 * <p>Menu types are vanilla/AE2 objects, so this stays free of foreign mod APIs; the concrete
 * bindings are registered by the required AE2 integration during bootstrap.</p>
 */
public record PortableMenuBinding(
        CellContentType type,
        MenuType<?> menuType
) {

}
