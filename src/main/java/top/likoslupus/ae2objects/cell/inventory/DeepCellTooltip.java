package top.likoslupus.ae2objects.cell.inventory;

import appeng.api.config.IncludeExclude;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
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

/** Client-safe tooltip projection based only on synchronized ItemStack metadata. */
public final class DeepCellTooltip {

    private DeepCellTooltip() {
    }

    public static void addCellInformation(ItemStack stack, List<Component> lines) {
        var inventory = DeepCellInventory.createInventory(stack, null, null);
        if (inventory == null) {
            return;
        }

        var uuid = inventory.getCellUUID();
        if (uuid != null) {
            lines.add(Component.literal("Cell UUID: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(uuid.toString()).withStyle(ChatFormatting.AQUA)));
        }

        lines.add(Tooltips.bytesUsed(inventory.getCachedUsedBytes(), inventory.getTotalBytes()));
        lines.add(typesUsedInfinite(inventory.getCachedStoredTypes()));

        if (!inventory.isPreformatted()) {
            return;
        }

        var mode = (
                inventory.getPartitionListMode() == IncludeExclude.WHITELIST
                        ? GuiText.Included
                        : GuiText.Excluded
        ).text();

        var precision = inventory.isFuzzy()
                ? GuiText.Fuzzy.text()
                : GuiText.Precise.text();
        lines.add(GuiText.Partitioned.withSuffix(" - ")
                .append(mode)
                .append(" ")
                .append(precision));
    }

    private static Component typesUsedInfinite(long types) {
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

    public static Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var inventory = DeepCellInventory.createInventory(
                stack,
                null,
                null
        );
        if (inventory == null) {
            return Optional.empty();
        }

        var upgradeStacks = new ArrayList<ItemStack>();
        if (AEConfig.instance().isTooltipShowCellUpgrades()) {
            var upgrades = inventory.getUpgradesInventory();
            if (upgrades != null) {
                upgrades.forEach(upgradeStacks::add);
            }
        }

        var content = new ArrayList<GenericStack>();
        var hasMoreContent = false;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            var maxShown = AEConfig.instance().getTooltipMaxCellContentShown();
            var availableStacks = new KeyCounter();
            inventory.getAvailableStacks(availableStacks);
            availableStacks.forEach(entry ->
                    content.add(new GenericStack(entry.getKey(), entry.getLongValue()))
            );

            if (content.size() < maxShown
                    && inventory.getPartitionListMode() == IncludeExclude.WHITELIST
            ) {
                var config = inventory.getConfigInventory();
                IntStream.range(0, config.size())
                        .mapToObj(config::getKey)
                        .filter(Objects::nonNull)
                        .filter(key -> availableStacks.get(key) <= 0)
                        .map(key -> new GenericStack(key, 0))
                        .forEach(content::add);
            }

            content.sort(Comparator.comparingLong(GenericStack::amount).reversed());
            hasMoreContent =
                    inventory.getCachedStoredTypes() > maxShown || content.size() > maxShown;
            if (content.size() > maxShown) {
                content.subList(maxShown, content.size()).clear();
            }
        }

        return Optional.of(new StorageCellTooltipComponent(
                upgradeStacks,
                AEConfig.instance().isTooltipShowCellContent()
                        ? content
                        : Collections.emptyList(),
                hasMoreContent,
                true
        ));
    }

}
