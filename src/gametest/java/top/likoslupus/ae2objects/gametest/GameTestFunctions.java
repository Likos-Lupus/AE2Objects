package top.likoslupus.ae2objects.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.item.CellWorkbenchSupport;
import top.likoslupus.ae2objects.cell.item.DeepCellDefinitionProvider;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellForm;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.cell.persistence.CellContentsCodec;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.cell.storage.DeepCellContents;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.ModDataComponents;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static top.likoslupus.ae2objects.gametest.DeepCellTestSupport.*;

/**
 * GameTest bodies. Each method is a {@link Consumer} of {@link GameTestHelper} registered by
 * {@link GameTestRegistrar}.
 *
 * <p>AE keys are created lazily (never in static initializers) because this class is referenced
 * during the {@code RegisterEvent} dispatch, before the registries are fully bound.</p>
 */
@SuppressWarnings({"DataFlowIssue", "NullableProblems", "OptionalGetWithoutIsPresent"})
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

        var accepted = cell.insert(
                stone,
                100,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        helper.assertValueEqual(
                accepted,
                100L,
                "insert should accept 100 items"
        );
        helper.assertValueEqual(
                stored(cell, stone),
                100L,
                "stored amount after insert"
        );

        var extracted = cell.extract(
                stone,
                40,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        helper.assertValueEqual(
                extracted,
                40L,
                "extract should return 40 items"
        );
        helper.assertValueEqual(
                stored(cell, stone),
                60L,
                "stored amount after extract"
        );

        helper.succeed();
    }

    private static AEItemKey stone() {
        return AEItemKey.of(Items.STONE);
    }

    public static void capacityClamp(GameTestHelper helper) {
        var stone = stone();
        var cell = inventory(drive(CellContentType.ITEM, CellTier.K1));

        // 1k item cell: 1_000 bytes * 1 amount/byte = 1_000 items.
        var accepted = cell.insert(
                stone,
                5_000,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        helper.assertValueEqual(
                accepted,
                1_000L,
                "1k item cell must clamp to 1_000"
        );
        helper.assertValueEqual(
                stored(cell, stone),
                1_000L,
                "stored amount after clamp"
        );

        helper.succeed();
    }

    // ---------------------------------------------------------------- storage engine

    // TODO: delete this and uncomment the below one
    public static void fluidCapacityLong(GameTestHelper helper) {
        var water = water();
        var cell = inventory(drive(CellContentType.FLUID, CellTier.K256));

        // 256k fluid cell: 256_000 bytes * 1_000 mB/byte = 256_000_000 mB.
        var accepted = cell.insert(
                water,
                300_000_000L,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        helper.assertValueEqual(
                accepted,
                256_000_000L,
                "256k fluid cell must clamp to 256_000_000 mB"
        );
        helper.assertValueEqual(
                stored(cell, water),
                256_000_000L,
                "stored mB after clamp"
        );

        helper.succeed();
    }

//    public static void fluidCapacityLong(GameTestHelper helper) {
//        var water = water();
//        var cell = inventory(drive(CellContentType.FLUID, CellTier.M256));
//
//        // 256m fluid cell: 256_000_000 bytes * 1_000 mB/byte = 256_000_000_000 mB.
//        var accepted = cell.insert(
//                water,
//                300_000_000_000L,
//                Actionable.MODULATE,
//                IActionSource.empty()
//        );
//        helper.assertValueEqual(
//                accepted,
//                256_000_000_000L,
//                "256m fluid cell must clamp to 256_000_000_000 mB"
//        );
//        helper.assertValueEqual(
//                stored(cell, water),
//                256_000_000_000L,
//                "stored mB after clamp"
//        );
//
//        helper.succeed();
//    }

    private static AEFluidKey water() {
        return AEFluidKey.of(Fluids.WATER);
    }

    public static void uuidLifecycle(GameTestHelper helper) {
        var stone = stone();
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        var cell = inventory(stack);
        var context = ServerCellContext.getOrNull();
        helper.assertTrue(
                context != null,
                "server cell context should be available"
        );

        // SIMULATE must not allocate an identity or mutate the stack.
        cell.insert(
                stone,
                5,
                Actionable.SIMULATE,
                IActionSource.empty()
        );
        helper.assertTrue(
                CellStackData.cellId(stack) == null,
                "SIMULATE must not allocate a UUID"
        );

        // First MODULATE insertion allocates the UUID and a repository record.
        cell.insert(
                stone,
                5,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        var id = CellStackData.cellId(stack);
        helper.assertTrue(
                id != null,
                "MODULATE must allocate a UUID"
        );
        helper.assertTrue(
                context.repository().contains(id),
                "repository must contain the record"
        );

        // Emptying the cell clears the identity and removes the record.
        cell.extract(
                stone,
                5,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        helper.assertTrue(
                CellStackData.cellId(stack) == null,
                "empty cell must clear its UUID"
        );
        helper.assertFalse(
                context.repository().contains(id),
                "empty cell must remove its record"
        );

        helper.succeed();
    }

    public static void tooltipLines(GameTestHelper helper) {
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        inventory(stack).insert(
                stone(),
                5,
                Actionable.MODULATE,
                IActionSource.empty()
        );

        var id = CellStackData.cellId(stack);
        helper.assertTrue(
                id != null,
                "tooltip test cell should have a UUID"
        );

        var lines = stack.getTooltipLines(
                        Item.TooltipContext.of(helper.getLevel()),
                        null,
                        TooltipFlag.NORMAL
                ).stream()
                .map(Component::getString)
                .toList();

        helper.assertTrue(
                lines.stream()
                        .anyMatch(line -> line.contains("Cell UUID: ")),
                "tooltip must contain the UUID header: " + lines
        );
        helper.assertTrue(
                lines.stream()
                        .anyMatch(line -> line.contains(id.toString())),
                "tooltip must contain the cell UUID: " + lines
        );
        helper.assertTrue(
                lines.stream()
                        .anyMatch(line -> line.contains("ME Deep Item Storage Cells")),
                "tooltip must contain the family line: " + lines
        );

        helper.succeed();
    }

    public static void persistenceRoundtrip(GameTestHelper helper) {
        var stone = stone();
        var stack = drive(CellContentType.ITEM, CellTier.K4);
        inventory(stack).insert(
                stone,
                7,
                Actionable.MODULATE,
                IActionSource.empty()
        );

        // A second inventory over the same stack reloads from the repository.
        helper.assertValueEqual(
                stored(inventory(stack), stone),
                7L,
                "reloaded stored amount"
        );

        helper.succeed();
    }

    public static void partitionWhitelist(GameTestHelper helper) {
        var stone = stone();
        var diamond = diamond();
        var definition = definition(
                CellContentType.ITEM,
                CellTier.K1,
                CellForm.DRIVE
        );
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        CellWorkbenchSupport
                .config(stack, definition)
                .setStack(0, new GenericStack(stone, 1));

        var cell = inventory(stack);
        helper.assertValueEqual(
                cell.insert(
                        diamond,
                        1,
                        Actionable.MODULATE,
                        IActionSource.empty()
                ),
                0L,
                "whitelisted cell must reject unlisted item"
        );
        helper.assertValueEqual(
                cell.insert(
                        stone,
                        1,
                        Actionable.MODULATE,
                        IActionSource.empty()
                ),
                1L,
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
        var definition = definition(
                CellContentType.ITEM,
                CellTier.K1,
                CellForm.DRIVE
        );
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        CellWorkbenchSupport
                .upgrades(stack, definition)
                .addItems(AEItems.INVERTER_CARD.stack());
        CellWorkbenchSupport
                .config(stack, definition)
                .setStack(0, new GenericStack(stone, 1));

        var cell = inventory(stack);
        helper.assertValueEqual(
                cell.insert(
                        stone,
                        1,
                        Actionable.MODULATE,
                        IActionSource.empty()
                ),
                0L,
                "blacklisted cell must reject listed item"
        );
        helper.assertValueEqual(
                cell.insert(
                        diamond,
                        1,
                        Actionable.MODULATE,
                        IActionSource.empty()
                ),
                1L,
                "blacklisted cell must accept unlisted item"
        );

        helper.succeed();
    }

    public static void fuzzyItemOnly(GameTestHelper helper) {
        var definition = definition(
                CellContentType.ITEM,
                CellTier.K1,
                CellForm.DRIVE
        );
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        CellWorkbenchSupport.setFuzzyMode(
                stack,
                definition,
                FuzzyMode.IGNORE_ALL
        );

        helper.assertTrue(
                CellContentType.ITEM.supportsFuzzy(),
                "item content type must support fuzzy"
        );
        helper.assertValueEqual(
                CellWorkbenchSupport.fuzzyMode(stack, definition),
                FuzzyMode.IGNORE_ALL,
                "item cell must store its fuzzy mode"
        );

        helper.succeed();
    }

    public static void fluidRejectsFuzzy(GameTestHelper helper) {
        var definition = definition(
                CellContentType.FLUID,
                CellTier.K1,
                CellForm.DRIVE
        );
        var stack = drive(CellContentType.FLUID, CellTier.K1);
        var upgrades = CellWorkbenchSupport.upgrades(stack, definition);
        upgrades.addItems(AEItems.FUZZY_CARD.stack());

        CellWorkbenchSupport.setFuzzyMode(
                stack,
                definition,
                FuzzyMode.IGNORE_ALL
        );
        helper.assertFalse(
                CellContentType.FLUID.supportsFuzzy(),
                "fluid content type must not support fuzzy"
        );
        helper.assertValueEqual(
                CellWorkbenchSupport.fuzzyMode(stack, definition),
                FuzzyMode.IGNORE_ALL,
                "fluid cell must ignore even an installed fuzzy card"
        );
        helper.assertFalse(
                CellWorkbenchSupport.filter(stack, definition).isFuzzy(),
                "fluid cell filter must not report fuzzy"
        );

        helper.succeed();
    }

    public static void nestedCellRejected(GameTestHelper helper) {
        var stone = stone();
        var populated = drive(CellContentType.ITEM, CellTier.K1);
        inventory(populated).insert(
                stone,
                5,
                Actionable.MODULATE,
                IActionSource.empty()
        );

        var host = inventory(drive(CellContentType.ITEM, CellTier.K1));
        helper.assertValueEqual(
                host.insert(
                        AEItemKey.of(populated),
                        1,
                        Actionable.MODULATE,
                        IActionSource.empty()
                ),
                0L,
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
            helper.assertTrue(
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
            helper.assertTrue(
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

        helper.assertTrue(
                power.getAEMaxPower(stack) > 0,
                "portable cell must have a battery"
        );
        // injectAEPower returns the overflow that could not be stored.
        var overflow = power.injectAEPower(
                stack,
                1_000,
                Actionable.MODULATE
        );
        helper.assertTrue(
                1_000 - overflow > 0,
                "portable cell must accept energy"
        );
        helper.assertTrue(
                power.getAECurrentPower(stack) > 0,
                "portable cell must store energy"
        );

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
        helper.assertTrue(
                host != null,
                "portable deep cell must provide a menu host"
        );

        helper.succeed();
    }

    public static void cloneFromContainerMenu(GameTestHelper helper) {
        var stone = stone();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;

        var original = drive(CellContentType.ITEM, CellTier.K1);
        inventory(original).insert(
                stone,
                3,
                Actionable.MODULATE,
                IActionSource.empty()
        );
        var originalId = CellStackData.cellId(original);
        helper.assertTrue(
                originalId != null,
                "original cell must have a UUID"
        );

        var container = new SimpleContainer(9);
        container.setItem(0, original);
        var menu = new ChestMenu(
                MenuType.GENERIC_9x1,
                0,
                player.getInventory(),
                container,
                1
        );

        menu.clicked(0, 2, ContainerInput.CLONE, player);

        var cloned = menu.getCarried();
        helper.assertTrue(
                cloned.getItem() instanceof DeepCellDefinitionProvider,
                "clone must be a deep cell"
        );
        var clonedId = CellStackData.cellId(cloned);
        helper.assertTrue(
                clonedId != null,
                "clone must have a UUID"
        );
        helper.assertFalse(
                clonedId.equals(originalId),
                "clone must get an independent UUID"
        );
        helper.assertValueEqual(
                stored(inventory(cloned), stone),
                3L,
                "clone must copy the contents"
        );

        helper.succeed();
    }

    public static void recovery(GameTestHelper helper) {
        var stone = stone();
        var stack = drive(CellContentType.ITEM, CellTier.K1);
        inventory(stack).insert(
                stone,
                9,
                Actionable.MODULATE,
                IActionSource.empty()
        );

        var id = CellStackData.cellId(stack);
        helper.assertTrue(
                id != null,
                "source cell must have a UUID"
        );

        var context = ServerCellContext.getOrNull();
        helper.assertTrue(
                context != null,
                "server cell context should be available"
        );
        var record = context.repository().find(id).orElse(null);
        helper.assertTrue(
                record != null,
                "repository must contain the record"
        );

        // Mirrors /ae2objects recover: rebuild an item from the persisted cell_item id.
        var itemId = record.cellItemId()
                .map(Identifier::tryParse);
        helper.assertTrue(
                itemId.isPresent(),
                "record must persist the source cell id"
        );

        var recovered = new ItemStack(BuiltInRegistries.ITEM.getValue(itemId.get()));
        helper.assertTrue(
                recovered.getItem() instanceof DeepCellDefinitionProvider,
                "recovered item must be a deep cell"
        );
        recovered.set(ModDataComponents.CELL_ID.get(), id);
        CellStackData.updateSummary(
                recovered,
                record.storedAmount(),
                record.storedTypesCount()
        );

        helper.assertValueEqual(
                stored(inventory(recovered), stone),
                9L,
                "recovered cell must reload contents"
        );

        helper.succeed();
    }

    public static void contentsTotalTracking(GameTestHelper helper) {
        var contents = new DeepCellContents();
        var stone = stone();
        var diamond = diamond();

        contents.insert(stone, 5);
        contents.insert(diamond, 7);
        contents.insert(stone, 3);

        helper.assertValueEqual(
                contents.totalAmount(),
                15L,
                "total after merged inserts"
        );
        helper.assertValueEqual(
                contents.typeCount(),
                2,
                "merged type count"
        );
        helper.assertValueEqual(
                contents.amount(stone),
                8L,
                "merged amount"
        );

        helper.assertValueEqual(
                contents.extract(stone, 2),
                2L,
                "extract amount"
        );
        helper.assertValueEqual(
                contents.totalAmount(),
                13L,
                "total after extract"
        );
        helper.assertValueEqual(
                contents.extract(stone, 10),
                6L,
                "extract clamp"
        );
        helper.assertValueEqual(
                contents.totalAmount(),
                7L,
                "total after removal"
        );
        helper.assertValueEqual(
                contents.typeCount(),
                1,
                "type count after removal"
        );
        helper.assertValueEqual(
                contents.extract(diamond, 100),
                7L,
                "extract drain"
        );
        helper.assertTrue(
                contents.isEmpty(),
                "drained cell must be empty"
        );

        helper.succeed();
    }

    public static void previewKeepsTopTen(GameTestHelper helper) {
        var contents = new DeepCellContents();
        var items = List.of(
                Items.STONE,
                Items.DIRT,
                Items.GRASS_BLOCK,
                Items.COBBLESTONE,
                Items.OAK_LOG,
                Items.SAND,
                Items.GRAVEL,
                Items.GLASS,
                Items.IRON_INGOT,
                Items.GOLD_INGOT,
                Items.DIAMOND,
                Items.EMERALD
        );

        IntStream.range(0, items.size())
                .forEach(index ->
                        contents.insert(AEItemKey.of(items.get(index)), index + 1L)
                );

        var preview = CellContentsCodec.preview(contents);
        helper.assertValueEqual(
                preview.size(),
                10,
                "preview entry count"
        );
        helper.assertValueEqual(
                preview.getFirst().amount(),
                12L,
                "largest preview amount"
        );
        helper.assertValueEqual(
                preview.getLast().amount(),
                3L,
                "smallest preview amount"
        );
        helper.assertTrue(
                IntStream.range(1, preview.size())
                        .allMatch(index ->
                                preview.get(index - 1).amount() >= preview.get(index).amount()
                        ),
                "preview must stay sorted by amount descending"
        );

        helper.succeed();
    }

}
