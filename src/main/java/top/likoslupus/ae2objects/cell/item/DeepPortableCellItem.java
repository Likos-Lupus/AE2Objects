package top.likoslupus.ae2objects.cell.item;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.Upgrades;
import appeng.block.networking.EnergyCellBlockItem;
import appeng.core.definitions.AEBlocks;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.util.ConfigInventory;
import appeng.util.InteractionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.cell.ui.DeepCellTooltip;
import top.likoslupus.ae2objects.platform.ServerCellContext;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static appeng.api.storage.StorageCells.getCellInventory;

/**
 * Pocket form of a deep storage cell.
 *
 * <p>Reuses AE2's {@link AbstractPortableCell} for the battery, menu opening and dye colour, while
 * deep storage itself still flows through the custom cell handler. It deliberately does not
 * implement {@code IBasicCellItem}, so AE2's type/slot limits never apply.</p>
 */
public final class DeepPortableCellItem extends AbstractPortableCell
        implements DeepCellDefinitionProvider {

    private final CellDefinition definition;
    private final Supplier<? extends ItemLike> coreItem;
    private final Supplier<? extends ItemLike> housingItem;
    private final String familyTranslationKey;

    public DeepPortableCellItem(
            MenuType<?> menuType,
            Properties properties,
            int defaultColor,
            CellDefinition definition,
            Supplier<? extends ItemLike> coreItem,
            Supplier<? extends ItemLike> housingItem,
            String familyTranslationKey
    ) {
        super(menuType, properties, defaultColor);
        this.definition = definition;
        this.coreItem = coreItem;
        this.housingItem = housingItem;
        this.familyTranslationKey = familyTranslationKey;
    }

    @Override
    public CellDefinition definition() {
        return definition;
    }

    @Override
    public Identifier getRecipeId() {
        return definition.id();
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return 80d + 80d * Upgrades.getEnergyCardMultiplier(getUpgrades(stack));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getLevel() instanceof ServerLevel
                && tryDisassemble(stack, context.getPlayer())
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        if (level instanceof ServerLevel
                && InteractionUtil.isInAlternateUseMode(player)
                && tryDisassemble(player.getItemInHand(hand), player)
        ) {
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return CellWorkbenchSupport.upgrades(stack, definition, this::onUpgradesChanged);
    }

    private boolean tryDisassemble(ItemStack stack, @Nullable Player player) {
        if (player == null
                || !InteractionUtil.isInAlternateUseMode(player)
        ) {
            return false;
        }

        var playerInventory = player.getInventory();
        var inventory = getCellInventory(stack, null);
        if (inventory == null
                || playerInventory.getSelectedItem() != stack
                || !inventory.getAvailableStacks().isEmpty()
        ) {
            return false;
        }

        var cellId = CellStackData.cellId(stack);
        var context = ServerCellContext.getOrNull();
        if (cellId != null && context != null) {
            context.repository().remove(cellId);
        }

        // Preserve remaining charge by injecting it into the returned energy cell.
        var remainingEnergy = getAECurrentPower(stack);
        var energyCellStack = new ItemStack(AEBlocks.ENERGY_CELL.asItem());
        if (remainingEnergy > 0
                && energyCellStack.getItem() instanceof EnergyCellBlockItem energyCellItem
        ) {
            energyCellItem.injectAEPower(
                    energyCellStack,
                    remainingEnergy,
                    Actionable.MODULATE
            );
        }

        playerInventory.setItem(playerInventory.getSelectedSlot(), ItemStack.EMPTY);
        playerInventory.placeItemBackInInventory(new ItemStack(coreItem.get()));
        getUpgrades(stack).forEach(playerInventory::placeItemBackInInventory);
        playerInventory.placeItemBackInInventory(new ItemStack(housingItem.get()));
        playerInventory.placeItemBackInInventory(energyCellStack);
        playerInventory.placeItemBackInInventory(new ItemStack(AEBlocks.ME_CHEST.asItem()));
        return true;
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack stack) {
        return CellWorkbenchSupport.config(stack, definition);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        return CellWorkbenchSupport.fuzzyMode(stack, definition);
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode fuzzyMode) {
        CellWorkbenchSupport.setFuzzyMode(stack, definition, fuzzyMode);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, display, tooltip, tooltipFlag);

        tooltip.accept(Component
                .translatable(familyTranslationKey)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );

        var lines = new ArrayList<Component>();
        DeepCellTooltip.addCellInformation(stack, lines);
        lines.forEach(tooltip);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return DeepCellTooltip.getTooltipImage(stack);
    }

}
