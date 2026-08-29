package top.likoslupus.ae2objects.storage;

import appeng.api.config.IncludeExclude;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import top.likoslupus.ae2objects.item.DiskDriveItem;

import java.util.List;

public class DiskCellHandler implements ICellHandler {

    public static final DiskCellHandler INSTANCE = new DiskCellHandler();

    @Override
    public boolean isCell(ItemStack is) {
        return is.getItem() instanceof DiskDriveItem;
    }

    @Override
    public @Nullable DiskCellInventory getCellInventory(ItemStack is, @Nullable ISaveProvider container) {
        return DiskCellInventory.createInventory(is, container, DiskStorageAccess.getOrNull());
    }

    public void addCellInformationToTooltip(ItemStack stack, List<Component> lines) {
        // Explicitly don't pass a storage manager since this only needs info stored on the item
        var handler = DiskCellInventory.createInventory(
                stack,
                null,
                null
        );

        if (handler == null) {
            return;
        }

        var uuid = handler.getDiskUUID();
        if (uuid != null) {
            lines.add(
                    Component.literal("Disk UUID: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(
                                    Component.literal(uuid.toString())
                                            .withStyle(ChatFormatting.AQUA)
                            )
            );
            lines.add(Tooltips.bytesUsed(
                    handler.getNbtItemCount(),
                    handler.getTotalBytes()
            ));
        }

        if (handler.isPreformatted()) {
            var list = (
                    handler.getPartitionListMode() == IncludeExclude.WHITELIST
                            ? GuiText.Included
                            : GuiText.Excluded
            ).text();

            if (handler.isFuzzy()) {
                lines.add(
                        GuiText.Partitioned.withSuffix(" - ")
                                .append(list)
                                .append(" ")
                                .append(GuiText.Fuzzy.text())
                );
            } else {
                lines.add(
                        GuiText.Partitioned.withSuffix(" - ")
                                .append(list)
                                .append(" ")
                                .append(GuiText.Precise.text())
                );
            }
        }
    }

}
