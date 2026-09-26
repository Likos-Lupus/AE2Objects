package top.likoslupus.ae2objects.cell.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.CellState;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.hooks.AEToolItem;
import appeng.items.contents.CellConfig;
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
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.DeepCellStackData;
import top.likoslupus.ae2objects.cell.inventory.DeepCellInventory;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static appeng.api.storage.StorageCells.getCellInventory;

/** Standard drive/chest form of a deep storage cell. */
public final class DeepStorageCellItem extends Item implements DeepCellItem, AEToolItem {

    private final CellDefinition definition;
    private final Supplier<? extends ItemLike> coreItem;
    private final Supplier<? extends ItemLike> housingItem;
    private final int upgradeSlots;
    private final String familyTranslationKey;

    public DeepStorageCellItem(
            ResourceKey<Item> id,
            Supplier<? extends ItemLike> coreItem,
            Supplier<? extends ItemLike> housingItem,
            CellDefinition definition,
            int upgradeSlots,
            String familyTranslationKey
    ) {
        super(properties(id, definition));
        if (upgradeSlots < 0) {
            throw new IllegalArgumentException("upgradeSlots must not be negative");
        }
        this.coreItem = coreItem;
        this.housingItem = housingItem;
        this.definition = definition;
        this.upgradeSlots = upgradeSlots;
        this.familyTranslationKey = familyTranslationKey;
    }

    private static Properties properties(
            ResourceKey<Item> id,
            CellDefinition definition
    ) {
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

        var inventory = DeepCellInventory.createInventory(
                stack,
                null,
                null
        );
        var status = inventory != null
                ? inventory.getClientStatus()
                : CellState.EMPTY;
        return 0xFF000000 | status.getStateColor();
    }

    @Override
    public CellDefinition definition() {
        return definition;
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack stack) {
        return CellConfig.create(Set.of(getKeyType()), stack);
    }

    @Override
    public boolean isEditable(ItemStack stack) {
        return true;
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        return supportsFuzzy()
                ?
                stack.getOrDefault(
                        Ae2ObjectsDataComponents.FUZZY_MODE.get(),
                        FuzzyMode.IGNORE_ALL
                )
                : FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode fuzzyMode) {
        if (supportsFuzzy()) {
            stack.set(Ae2ObjectsDataComponents.FUZZY_MODE.get(), fuzzyMode);
        }
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
        if (player == null || !InteractionUtil.isInAlternateUseMode(player)) {
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

        var cellId = DeepCellStackData.cellId(stack);
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
        return UpgradeInventories.forItem(stack, upgradeSlots);
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
        addCellInformationToTooltip(stack, lines);
        lines.forEach(tooltip);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return getCellTooltipImage(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel
                && tryDisassemble(stack, context.getPlayer())
        ) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
