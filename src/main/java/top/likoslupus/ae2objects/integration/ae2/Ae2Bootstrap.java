package top.likoslupus.ae2objects.integration.ae2;

import appeng.menu.me.common.MEStorageMenu;
import top.likoslupus.ae2objects.cell.channel.PortableMenuBinding;
import top.likoslupus.ae2objects.cell.channel.PortableMenuRegistry;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.model.CellContentType;

/**
 * Registers the runtime storage channels and portable menus provided by the required AE2
 * dependency.
 *
 * <p>Runs during mod construction, before any content is registered or any inventory/menu can be
 * created.</p>
 */
public final class Ae2Bootstrap {

    private Ae2Bootstrap() {
    }

    public static void bootstrapRequired() {
        var channels = StorageChannelRegistry.INSTANCE;
        channels.register(new ItemChannelBinding());
        channels.register(new FluidChannelBinding());

        var menus = PortableMenuRegistry.INSTANCE;
        menus.register(new PortableMenuBinding(
                CellContentType.ITEM,
                MEStorageMenu.PORTABLE_ITEM_CELL_TYPE
        ));
        menus.register(new PortableMenuBinding(
                CellContentType.FLUID,
                MEStorageMenu.PORTABLE_FLUID_CELL_TYPE
        ));
    }

}
