package top.likoslupus.ae2objects.cell.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.hooks.AEToolItem;
import appeng.util.ConfigInventory;
import appeng.util.InteractionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.CellUpgradeProfile;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.cell.stack.CellView;
import top.likoslupus.ae2objects.cell.ui.DeepCellTooltip;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static appeng.api.storage.StorageCells.getCellInventory;

/** Standard drive/chest form of a deep storage cell. */
public final class DeepDriveCellItem extends Item
        implements DeepCellDefinitionProvider, ICellWorkbenchItem, AEToolItem {

    private final CellDefinition definition;
    private final Supplier<? extends ItemLike> coreItem;
    private final Supplier<? extends ItemLike> housingItem;
    private final String familyTranslationKey;

    public DeepDriveCellItem(
            ResourceKey<Item> id,
            Supplier<? extends ItemLike> coreItem,
            Supplier<? extends ItemLike> housingItem,
            CellDefinition definition,
            String familyTranslationKey
    ) {
        super(properties(id, definition));
        this.coreItem = coreItem;
        this.housingItem = housingItem;
        this.definition = definition;
        this.familyTranslationKey = familyTranslationKey;
    }

    private static Properties properties(ResourceKey<Item> id, CellDefinition definition) {
        var properties = new Properties()
                .setId(id)
                .stacksTo(1)
                .fireResistant()
                .component(Ae2ObjectsDataComponents.STORED_AMOUNT.get(), 0L)
                .component(Ae2ObjectsDataComponents.STORED_TYPE_COUNT.get(), 0);

        if (definition.type().supportsFuzzy()) {
            properties.component(
                    Ae2ObjectsDataComponents.FUZZY_MODE.get(),
                    FuzzyMode.IGNORE_ALL
            );
        }
        return properties;
    }

    public static int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return 0xFFFFFFFF;
        }

        if (!(stack.getItem() instanceof DeepCellDefinitionProvider provider)) {
            return 0xFF000000 | CellState.EMPTY.getStateColor();
        }

        var status = CellView.status(
                provider.definition(),
                CellStackData.snapshot(stack)
        );
        return 0xFF000000 | status.getStateColor();
    }

    @Override
    public CellDefinition definition() {
        return definition;
    }

    @Override
    public boolean isEditable(ItemStack stack) {
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
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        if (!InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            tryDisassemble(player.getItemInHand(hand), player);
        }
        return InteractionResult.SUCCESS;
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

        playerInventory.setItem(playerInventory.getSelectedSlot(), ItemStack.EMPTY);
        playerInventory.placeItemBackInInventory(new ItemStack(coreItem.get()));

        var upgrades = getUpgrades(stack);
        upgrades.forEach(playerInventory::placeItemBackInInventory);

        playerInventory.placeItemBackInInventory(new ItemStack(housingItem.get()));
        return true;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return CellWorkbenchSupport.upgrades(stack, definition);
    }

    // FIXME: Overrides deprecated method in 'net.minecraft.world.item.Item'
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
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

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getLevel() instanceof ServerLevel
                && tryDisassemble(stack, context.getPlayer())
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    /** Upgrade slots for this cell, derived from the definition. */
    public int upgradeSlots() {
        return CellUpgradeProfile.forDefinition(definition).totalSlots();
    }

}
