package top.likoslupus.ae2objects.storage;

import appeng.api.config.IncludeExclude;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

public class DeepCellHandler implements ICellHandler {

    public static final DeepCellHandler INSTANCE = new DeepCellHandler();

    @Override
    public boolean isCell(ItemStack is) {
        return is.getItem() instanceof DeepCellItem;
    }

    @Override
    public @Nullable DeepCellInventory getCellInventory(
            ItemStack is,
            @Nullable ISaveProvider container
    ) {
        return DeepCellInventory.createInventory(
                is,
                container,
                DeepStorageAccess.getOrNull()
        );
    }

    public void addCellInformationToTooltip(ItemStack stack, List<Component> lines) {
        // Explicitly don't pass a storage manager since this only needs info stored on the item
        var handler = DeepCellInventory.createInventory(
                stack,
                null,
                null
        );

        if (handler == null) {
            return;
        }

        var uuid = handler.getCellUUID();
        if (uuid != null) {
            lines.add(
                    Component.literal("Cell UUID: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(
                                    Component.literal(uuid.toString())
                                            .withStyle(ChatFormatting.AQUA)
                            )
            );
        }

        lines.add(Tooltips.bytesUsed(
                handler.getNbtItemCount(),
                handler.getTotalBytes()
        ));
        lines.add(typesUsedInfinite(handler.getNbtItemTypes()));

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

    public static Component typesUsedInfinite(long types) {
        return Tooltips.of(
                Tooltips.ofUnformattedNumberWithRatioColor(types, 0.0, false),
                Tooltips.of(" "),
                Tooltips.of(GuiText.Of),
                Tooltips.of(" "),
                Component.literal("∞").withStyle(Tooltips.NUMBER_TEXT),
                Tooltips.of(" "),
                Tooltips.of(GuiText.Types)
        );
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack is) {
        var handler = DeepCellInventory.createInventory(
                is,
                null,
                null
        );
        if (handler == null) {
            return Optional.empty();
        }

        var upgradeStacks = new ArrayList<ItemStack>();
        if (AEConfig.instance().isTooltipShowCellUpgrades()) {
            var upgrades = handler.getUpgradesInventory();
            if (upgrades != null) {
                upgrades.forEach(upgradeStacks::add);
            }
        }

        boolean hasMoreContent;
        List<GenericStack> content;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            content = new ArrayList<>();

            var maxCountShown = AEConfig.instance().getTooltipMaxCellContentShown();

            var availableStacks = new KeyCounter();
            handler.getAvailableStacks(availableStacks);

            availableStacks.forEach(entry ->
                    content.add(new GenericStack(entry.getKey(), entry.getLongValue()))
            );

            if (content.size() < maxCountShown
                    && handler.getPartitionListMode() == IncludeExclude.WHITELIST
            ) {
                var config = handler.getConfigInventory();

                IntStream.range(0, config.size())
                        .mapToObj(config::getKey)
                        .filter(Objects::nonNull)
                        .filter(what -> availableStacks.get(what) <= 0)
                        .map(what -> new GenericStack(what, 0))
                        .forEach(content::add);
            }

            content.sort(Comparator.comparingLong(GenericStack::amount).reversed());

            hasMoreContent = handler.getNbtItemTypes() > maxCountShown
                    || content.size() > maxCountShown;

            if (content.size() > maxCountShown) {
                content.subList(maxCountShown, content.size()).clear();
            }
        } else {
            hasMoreContent = false;
            content = Collections.emptyList();
        }

        return Optional.of(new StorageCellTooltipComponent(
                upgradeStacks,
                content,
                hasMoreContent,
                true
        ));
    }

}
