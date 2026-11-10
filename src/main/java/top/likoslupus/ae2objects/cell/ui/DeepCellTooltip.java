package top.likoslupus.ae2objects.cell.ui;

import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
import top.likoslupus.ae2objects.cell.item.CellWorkbenchSupport;
import top.likoslupus.ae2objects.cell.item.DeepCellDefinitionProvider;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.cell.stack.CellView;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/**
 * Client-safe tooltip projection.
 *
 * <p>Reads only the {@link top.likoslupus.ae2objects.cell.stack.CellStackSnapshot} and the
 * workbench state; it never creates a storage inventory or touches the repository.</p>
 */
public final class DeepCellTooltip {

    private DeepCellTooltip() {
    }

    /**
     * Registers the deep-cell lines at the same position as the old {@code Item#appendHoverText}
     * override: after the item's custom tooltip and before vanilla data-component tooltips.
     */
    public static void registerTooltipAppender(RegisterTooltipAppendersEvent event) {
        event.registerAppender(
                TooltipLocation.POST_CUSTOM,
                DeepCellTooltip::appendTooltip
        );
    }

    private static void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            @Nullable Player player,
            TooltipFlag tooltipFlag,
            Consumer<Component> tooltip
    ) {
        if (!(stack.getItem() instanceof DeepCellDefinitionProvider provider)) {
            return;
        }

        tooltip.accept(Component
                .translatable(provider.definition().translationKey())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );

        var lines = new ArrayList<Component>();
        addCellInformation(stack, lines);
        lines.forEach(tooltip);
    }

    public static void addCellInformation(ItemStack stack, List<Component> lines) {
        var definition = definitionOf(stack);
        if (definition == null) {
            return;
        }

        var snapshot = CellStackData.snapshot(stack);
        var capacity = CellView.capacity(definition);

        var uuid = snapshot.id();
        if (uuid != null) {
            lines.add(Component.literal("Cell UUID: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(uuid.toString()).withStyle(ChatFormatting.AQUA)));
        }

        lines.add(Tooltips.bytesUsed(
                capacity.usedBytes(snapshot.storedAmount()),
                capacity.bytes()
        ));
        lines.add(typesUsedInfinite(snapshot.storedTypes()));

        var filter = CellWorkbenchSupport.filter(stack, definition);
        if (!filter.isPreformatted()) {
            return;
        }

        var mode = (
                filter.mode() == IncludeExclude.WHITELIST
                        ? GuiText.Included
                        : GuiText.Excluded
        ).text();
        var precision = filter.isFuzzy()
                ? GuiText.Fuzzy.text()
                : GuiText.Precise.text();
        lines.add(GuiText.Partitioned.withSuffix(" - ")
                .append(mode)
                .append(" ")
                .append(precision));
    }

    private static @Nullable CellDefinition definitionOf(ItemStack stack) {
        return stack.getItem() instanceof DeepCellDefinitionProvider provider
                ? provider.definition()
                : null;
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
        var definition = definitionOf(stack);
        if (definition == null) {
            return Optional.empty();
        }

        var snapshot = CellStackData.snapshot(stack);
        var upgradeStacks = new ArrayList<ItemStack>();
        if (AEConfig.instance().isTooltipShowCellUpgrades()) {
            CellWorkbenchSupport.upgrades(stack, definition).forEach(upgradeStacks::add);
        }

        var content = new ArrayList<GenericStack>();
        var hasMoreContent = false;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            var maxShown = AEConfig.instance().getTooltipMaxCellContentShown();

            content.addAll(snapshot.preview());

            var present = new HashSet<AEKey>();
            snapshot.preview().forEach(entry -> present.add(entry.what()));

            var filter = CellWorkbenchSupport.filter(stack, definition);
            if (content.size() < maxShown
                    && filter.mode() == IncludeExclude.WHITELIST
            ) {
                var config = CellWorkbenchSupport.config(stack, definition);
                IntStream.range(0, config.size())
                        .mapToObj(config::getKey)
                        .filter(Objects::nonNull)
                        .filter(key -> !present.contains(key))
                        .map(key -> new GenericStack(key, 0))
                        .forEach(content::add);
            }

            content.sort(Comparator.comparingLong(GenericStack::amount).reversed());
            hasMoreContent = snapshot.storedTypes() > maxShown || content.size() > maxShown;
            if (content.size() > maxShown) {
                content.subList(maxShown, content.size()).clear();
            }
        }

        return Optional.of(new StorageCellTooltipComponent(
                upgradeStacks,
                content,
                hasMoreContent,
                true
        ));
    }

}
