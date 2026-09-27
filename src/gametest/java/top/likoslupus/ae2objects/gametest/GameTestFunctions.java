package top.likoslupus.ae2objects.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.item.CellWorkbenchSupport;
import top.likoslupus.ae2objects.cell.item.DeepCellDefinitionProvider;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellForm;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.ModDataComponents;

import java.util.Objects;
import java.util.function.Consumer;

import static top.likoslupus.ae2objects.gametest.DeepCellTestSupport.*;

/**
 * GameTest bodies. Each method is a {@link Consumer} of {@link GameTestHelper} registered by
 * {@link GameTestRegistrar}.
 *
 * <p>AE keys are created lazily (never in static initializers) because this class is referenced
 * during the {@code RegisterEvent} dispatch, before the registries are fully bound.</p>
 */
public final class GameTestFunctions {

    private GameTestFunctions() {
    }

    public static ResourceKey<Consumer<GameTestHelper>> key(String path) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Ae2Objects.id(path));
    }

    public static void alwaysPass(GameTestHelper helper) {
        helper.succeed();
    }

    public static void driveInsertExtract(GameTestHelper helper) {
        var stone = stone();
        var cell = inventory(drive(CellContentType.ITEM, CellTier.K1));

        var accepted = cell.insert(stone, 100, Actionable.MODULATE, IActionSource.empty());
        assertTrue(helper, accepted == 100, "insert should accept 100 items");
        assertAmount(helper, cell, stone, 100, "stored amount after insert");

        var extracted = cell.extract(stone, 40, Actionable.MODULATE, IActionSource.empty());
        assertTrue(helper, extracted == 40, "extract should return 40 items");
        assertAmount(helper, cell, stone, 60, "stored amount after extract");

        helper.succeed();
    }

    private static AEItemKey stone() {
        return AEItemKey.of(Items.STONE);
    }

    public static void capacityClamp(GameTestHelper helper) {
        var stone = stone();
        var cell = inventory(drive(CellContentType.ITEM, CellTier.K1));

        // 1k item cell: 1_000 bytes * 1 amount/byte = 1_000 items.
        var accepted = cell.insert(stone, 5_000, Actionable.MODULATE, IActionSource.empty());
        assertTrue(helper, accepted == 1_000, "1k item cell must clamp to 1_000, got " + accepted);
        assertAmount(helper, cell, stone, 1_000, "stored amount after clamp");

        helper.succeed();
    }

    // ---------------------------------------------------------------- storage engine

    public static void fluidCapacityLong(GameTestHelper helper) {
        var water = water();
        var cell = inventory(drive(CellContentType.FLUID, CellTier.M256));

        // 256m fluid cell: 256_000_000 bytes * 1_000 mB/byte = 256_000_000_000 mB.
        var accepted = cell.insert(
                water,
                300_000_000_000L,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        assertTrue(
                helper,
                accepted == 256_000_000_000L,
                "256m fluid cell must clamp to 256_000_000_000 mB, got " + accepted
        );
        assertAmount(helper, cell, water, 256_000_000_000L, "stored mB after clamp");

        helper.succeed();
    }

    private static AEFluidKey water() {
        return AEFluidKey.of(Fluids.WATER);
    }

    public static void uuidLifecycle(GameTestHelper helper) {
        var stone = stone();
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        var cell = inventory(stack);
        var context = ServerCellContext.getOrNull();
        assertTrue(helper, context != null, "server cell context should be available");

        // SIMULATE must not allocate an identity or mutate the stack.
        cell.insert(stone, 5, Actionable.SIMULATE, IActionSource.empty());
        assertTrue(
                helper,
                CellStackData.cellId(stack) == null,
                "SIMULATE must not allocate a UUID"
        );

        // First MODULATE insertion allocates the UUID and a repository record.
        cell.insert(stone, 5, Actionable.MODULATE, IActionSource.empty());
        var id = CellStackData.cellId(stack);
        assertTrue(helper, id != null, "MODULATE must allocate a UUID");
        assertTrue(helper, context.repository().contains(id), "repository must contain the record");

        // Emptying the cell clears the identity and removes the record.
        cell.extract(stone, 5, Actionable.MODULATE, IActionSource.empty());
        assertTrue(helper, CellStackData.cellId(stack) == null, "empty cell must clear its UUID");
        assertTrue(helper, !context.repository().contains(id), "empty cell must remove its record");

        helper.succeed();
    }

    public static void persistenceRoundtrip(GameTestHelper helper) {
        var stone = stone();
        var stack = drive(CellContentType.ITEM, CellTier.K4);
        inventory(stack).insert(stone, 7, Actionable.MODULATE, IActionSource.empty());

        // A second inventory over the same stack reloads from the repository.
        assertAmount(helper, inventory(stack), stone, 7, "reloaded stored amount");

        helper.succeed();
    }

    public static void partitionWhitelist(GameTestHelper helper) {
        var stone = stone();
        var diamond = diamond();
        var definition = definition(CellContentType.ITEM, CellTier.K1, CellForm.DRIVE);
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        CellWorkbenchSupport.config(stack, definition)
                .setStack(0, new GenericStack(stone, 1));

        var cell = inventory(stack);
        assertTrue(
                helper,
                cell.insert(diamond, 1, Actionable.MODULATE, IActionSource.empty()) == 0,
                "whitelisted cell must reject unlisted item"
        );
        assertTrue(
                helper,
                cell.insert(stone, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                "whitelisted cell must accept listed item"
        );

        helper.succeed();
    }

    private static AEItemKey diamond() {
        return AEItemKey.of(Items.DIAMOND);
    }

    public static void partitionBlacklist(GameTestHelper helper) {
        var stone = stone();
        var diamond = diamond();
        var definition = definition(CellContentType.ITEM, CellTier.K1, CellForm.DRIVE);
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        CellWorkbenchSupport.upgrades(stack, definition).addItems(AEItems.INVERTER_CARD.stack());
        CellWorkbenchSupport.config(stack, definition)
                .setStack(0, new GenericStack(stone, 1));

        var cell = inventory(stack);
        assertTrue(
                helper,
                cell.insert(stone, 1, Actionable.MODULATE, IActionSource.empty()) == 0,
                "blacklisted cell must reject listed item"
        );
        assertTrue(
                helper,
                cell.insert(diamond, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                "blacklisted cell must accept unlisted item"
        );

        helper.succeed();
    }

    public static void fuzzyItemOnly(GameTestHelper helper) {
        var definition = definition(CellContentType.ITEM, CellTier.K1, CellForm.DRIVE);
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        CellWorkbenchSupport.setFuzzyMode(stack, definition, FuzzyMode.IGNORE_ALL);

        assertTrue(
                helper,
                CellContentType.ITEM.supportsFuzzy(),
                "item content type must support fuzzy"
        );
        assertTrue(
                helper,
                CellWorkbenchSupport.fuzzyMode(stack, definition) == FuzzyMode.IGNORE_ALL,
                "item cell must store its fuzzy mode"
        );

        helper.succeed();
    }

    public static void fluidRejectsFuzzy(GameTestHelper helper) {
        var definition = definition(CellContentType.FLUID, CellTier.K1, CellForm.DRIVE);
        var stack = drive(CellContentType.FLUID, CellTier.K1);
        var upgrades = CellWorkbenchSupport.upgrades(stack, definition);
        upgrades.addItems(AEItems.FUZZY_CARD.stack());

        CellWorkbenchSupport.setFuzzyMode(stack, definition, FuzzyMode.IGNORE_ALL);
        assertTrue(
                helper,
                !CellContentType.FLUID.supportsFuzzy(),
                "fluid content type must not support fuzzy"
        );
        assertTrue(
                helper,
                CellWorkbenchSupport.fuzzyMode(stack, definition) == FuzzyMode.IGNORE_ALL,
                "fluid cell must ignore even an installed fuzzy card"
        );
        assertTrue(
                helper,
                !CellWorkbenchSupport.filter(stack, definition).isFuzzy(),
                "fluid cell filter must not report fuzzy"
        );

        helper.succeed();
    }

    public static void nestedCellRejected(GameTestHelper helper) {
        var stone = stone();
        var populated = drive(CellContentType.ITEM, CellTier.K1);
        inventory(populated).insert(stone, 5, Actionable.MODULATE, IActionSource.empty());

        var host = inventory(drive(CellContentType.ITEM, CellTier.K1));
        assertTrue(
                helper,
                host.insert(AEItemKey.of(populated), 1, Actionable.MODULATE, IActionSource.empty())
                        == 0,
                "a deep cell must not be stored inside another deep cell"
        );

        helper.succeed();
    }

    // ---------------------------------------------------------------- blocks

    public static void meChestAcceptsCell(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, AEBlocks.ME_CHEST.block());
        var chest = helper.getBlockEntity(pos, MEChestBlockEntity.class);
        var cell = drive(CellContentType.ITEM, CellTier.K1);

        helper.succeedWhen(() -> {
            chest.getInternalInventory().setItemDirect(1, cell);
            assertTrue(
                    helper,
                    chest.getCellInventory(0) != null,
                    "ME Chest must expose the deep cell as a storage cell"
            );
        });
    }

    public static void meDriveCompat(GameTestHelper helper) {
        var pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, AEBlocks.DRIVE.block());
        var drive = helper.getBlockEntity(pos, DriveBlockEntity.class);
        var cell = drive(CellContentType.ITEM, CellTier.K1);

        helper.succeedWhen(() -> {
            drive.getInternalInventory().setItemDirect(0, cell);
            assertTrue(
                    helper,
                    drive.getInternalInventory().getStackInSlot(0).getItem()
                            instanceof DeepCellDefinitionProvider,
                    "ME Drive must hold the deep cell"
            );
        });
    }

    // ---------------------------------------------------------------- portable / interaction

    public static void portablePower(GameTestHelper helper) {
        var stack = portable(CellContentType.ITEM, CellTier.K1);
        var power = (IAEItemPowerStorage) stack.getItem();

        assertTrue(helper, power.getAEMaxPower(stack) > 0, "portable cell must have a battery");
        // injectAEPower returns the overflow that could not be stored.
        var overflow = power.injectAEPower(stack, 1_000, Actionable.MODULATE);
        assertTrue(helper, 1_000 - overflow > 0, "portable cell must accept energy");
        assertTrue(helper, power.getAECurrentPower(stack) > 0, "portable cell must store energy");

        helper.succeed();
    }

    public static void portableOpen(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var stack = portable(CellContentType.ITEM, CellTier.K1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // The mock connection cannot receive screens, so verify the menu host that backs the
        // portable menu can be constructed for this item (the actual open path delegates to it).
        var item = (AbstractPortableCell) stack.getItem();
        var host = item.getMenuHost(
                player,
                MenuLocators.forHand(player, InteractionHand.MAIN_HAND),
                null
        );
        assertTrue(helper, host != null, "portable deep cell must provide a menu host");

        helper.succeed();
    }

    public static void cloneFromContainerMenu(GameTestHelper helper) {
        var stone = stone();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;

        var original = drive(CellContentType.ITEM, CellTier.K1);
        inventory(original).insert(stone, 3, Actionable.MODULATE, IActionSource.empty());
        var originalId = CellStackData.cellId(original);
        assertTrue(helper, originalId != null, "original cell must have a UUID");

        var container = new SimpleContainer(9);
        container.setItem(0, original);
        var menu = new ChestMenu(MenuType.GENERIC_9x1, 0, player.getInventory(), container, 1);

        menu.clicked(0, 2, ContainerInput.CLONE, player);

        var cloned = menu.getCarried();
        assertTrue(
                helper,
                cloned.getItem() instanceof DeepCellDefinitionProvider,
                "clone must be a deep cell"
        );
        var clonedId = CellStackData.cellId(cloned);
        assertTrue(helper, clonedId != null, "clone must have a UUID");
        assertTrue(helper, !clonedId.equals(originalId), "clone must get an independent UUID");
        assertAmount(helper, inventory(cloned), stone, 3, "clone must copy the contents");

        helper.succeed();
    }

    public static void recovery(GameTestHelper helper) {
        var stone = stone();
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        inventory(stack).insert(stone, 9, Actionable.MODULATE, IActionSource.empty());

        var id = CellStackData.cellId(stack);
        assertTrue(helper, id != null, "source cell must have a UUID");

        var context = ServerCellContext.getOrNull();
        assertTrue(helper, context != null, "server cell context should be available");
        var record = context.repository().find(id).orElse(null);
        assertTrue(helper, record != null, "repository must contain the record");

        // Mirrors /ae2objects recover: rebuild an item from the persisted cell_item id.
        var itemId = record.cellItemId()
                .map(Identifier::tryParse);
        assertTrue(helper, itemId.isPresent(), "record must persist the source cell id");

        var recovered = new ItemStack(BuiltInRegistries.ITEM.getValue(itemId.get()));
        assertTrue(
                helper,
                recovered.getItem() instanceof DeepCellDefinitionProvider,
                "recovered item must be a deep cell"
        );
        recovered.set(ModDataComponents.CELL_ID.get(), id);
        CellStackData.updateSummary(recovered, record.storedAmount(), record.storedTypesCount());

        assertAmount(helper, inventory(recovered), stone, 9, "recovered cell must reload contents");

        helper.succeed();
    }

}
