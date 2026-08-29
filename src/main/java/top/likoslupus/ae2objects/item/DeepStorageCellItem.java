package top.likoslupus.ae2objects.item;

import appeng.api.config.FuzzyMode;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKeyType;
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
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;
import top.likoslupus.ae2objects.storage.DeepCellInventory;
import top.likoslupus.ae2objects.storage.DeepCellItem;
import top.likoslupus.ae2objects.storage.DeepStorageAccess;

import java.util.*;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

import static appeng.api.storage.StorageCells.getCellInventory;

public class DeepStorageCellItem extends Item implements DeepCellItem, AEToolItem {

    private final int bytes;
    private final double idleDrain;
    private final ItemLike coreItem;
    private final ItemLike housingItem;
    private final AEKeyType keyType;

    public DeepStorageCellItem(
            ResourceKey<Item> id,
            ItemLike coreItem,
            ItemLike housingItem,
            int kilobytes,
            double idleDrain,
            AEKeyType keyType
    ) {
        super(
                new Properties().setId(id).stacksTo(1).fireResistant()
                        .component(Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get(), 0L)
                        .component(Ae2ObjectsDataComponents.CELL_TYPE_COUNT.get(), 0)
                        .component(Ae2ObjectsDataComponents.FUZZY_MODE.get(), FuzzyMode.IGNORE_ALL)
        );
        this.bytes = kilobytes * 1000;
        this.coreItem = coreItem;
        this.housingItem = housingItem;
        this.idleDrain = idleDrain;
        this.keyType = keyType;
    }

    public static int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 1) {
            var cellInv = DeepCellInventory.createInventory(
                    stack,
                    null,
                    null
            );
            var cellStatus = cellInv != null
                    ? cellInv.getClientStatus()
                    : CellState.EMPTY;
            return 0xFF000000 | cellStatus.getStateColor();
        } else {
            return 0xFFFFFFFF;
        }
    }

    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    @Override
    public FuzzyMode getFuzzyMode(final ItemStack is) {
        return is.getOrDefault(Ae2ObjectsDataComponents.FUZZY_MODE.get(), FuzzyMode.IGNORE_ALL);
    }

    @Override
    public void setFuzzyMode(final ItemStack is, final FuzzyMode fzMode) {
        is.set(Ae2ObjectsDataComponents.FUZZY_MODE.get(), fzMode);
    }

    @Override
    public AEKeyType getKeyType() {
        return this.keyType;
    }

    @Override
    public InteractionResult use(
            final Level level,
            final Player player,
            final InteractionHand hand
    ) {
        if (level instanceof ServerLevel serverLevel) {
            this.disassembleCell(player.getItemInHand(hand), serverLevel, player);
        }

        return InteractionResult.SUCCESS;
    }

    private boolean disassembleCell(
            final ItemStack stack,
            final ServerLevel level,
            final @Nullable Player player
    ) {
        if (player != null && InteractionUtil.isInAlternateUseMode(player)) {
            final var playerInventory = player.getInventory();
            var inv = getCellInventory(stack, null);
            if (inv != null && playerInventory.getSelectedItem() == stack) {
                var list = inv.getAvailableStacks();
                if (list.isEmpty()) {
                    playerInventory.setItem(playerInventory.getSelectedSlot(), ItemStack.EMPTY);

                    // drop core
                    playerInventory.placeItemBackInInventory(new ItemStack(coreItem));

                    // drop upgrades
                    var upgrades = this.getUpgrades(stack);
                    if (upgrades != null) {
                        upgrades.forEach(playerInventory::placeItemBackInInventory);
                    }

                    // drop empty storage cell housing
                    playerInventory.placeItemBackInInventory(
                            new ItemStack(housingItem)
                    );

                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, 2);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        tooltip.accept(
                Component.literal("Deep Item Storage Cell - Storage without type limits")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
        List<Component> lines = new ArrayList<>();
        addCellInformationToTooltip(stack, lines);
        lines.forEach(tooltip);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return getCellTooltipImage(stack);
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return bytes;
    }

    @Override
    public InteractionResult onItemUseFirst(
            ItemStack stack,
            UseOnContext context
    ) {
        if (context.getLevel() instanceof ServerLevel serverLevel) {
            return this.disassembleCell(stack, serverLevel, context.getPlayer())
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public double getIdleDrain() {
        return idleDrain;
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack is) {
        return CellConfig.create(Set.of(getKeyType()), is);
    }

    @Override
    public ItemStack clone(ItemStack item) {
        var cellId = item.get(Ae2ObjectsDataComponents.CELL_ID.get());
        if (cellId != null) {
            var id = UUID.randomUUID();
            var newStack = item.copy();
            newStack.set(Ae2ObjectsDataComponents.CELL_ID.get(), id);
            newStack.setCount(newStack.getMaxStackSize());

            // Deep clone the cell storage if storage manager is available
            var storageManager = DeepStorageAccess.getOrNull();
            if (storageManager != null) {
                var originalStorage = storageManager.getOrCreateCell(cellId);
                var clonedStorage = originalStorage.copy();
                newStack.set(
                        Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get(),
                        clonedStorage.getItemCount()
                );
                newStack.set(
                        Ae2ObjectsDataComponents.CELL_TYPE_COUNT.get(),
                        clonedStorage.getStoredTypesCount()
                );
                storageManager.updateCell(id, clonedStorage);
            } else {
                newStack.remove(Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get());
                newStack.remove(Ae2ObjectsDataComponents.CELL_TYPE_COUNT.get());
                newStack.remove(AEComponents.STORAGE_CELL_INV);
            }

            return newStack;
        } else {
            return item.copy();
        }
    }

}
