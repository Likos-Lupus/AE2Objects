package top.likoslupus.ae2objects.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import top.likoslupus.ae2objects.Ae2Objects;

import java.util.List;
import java.util.function.Consumer;

/**
 * Registers the GameTest functions and instances.
 *
 * <p>Both callbacks are mod-bus events and only fire when the gametest framework is enabled
 * ({@code GameTestHooks.isGametestEnabled()}), so the release runtime is unaffected even though
 * this class is part of the dev mod.</p>
 */
@EventBusSubscriber(modid = Ae2Objects.MOD_ID)
public final class GameTestRegistrar {

    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;

    private record TestCase(String path, Consumer<GameTestHelper> function) {
    }

    private static final List<TestCase> TESTS = List.of(
            new TestCase("always_pass", GameTestFunctions::alwaysPass),
            new TestCase("drive_insert_extract", GameTestFunctions::driveInsertExtract),
            new TestCase("capacity_clamp", GameTestFunctions::capacityClamp),
            new TestCase("fluid_capacity_long", GameTestFunctions::fluidCapacityLong),
            new TestCase("uuid_lifecycle", GameTestFunctions::uuidLifecycle),
            new TestCase("persistence_roundtrip", GameTestFunctions::persistenceRoundtrip),
            new TestCase("partition_whitelist", GameTestFunctions::partitionWhitelist),
            new TestCase("partition_blacklist", GameTestFunctions::partitionBlacklist),
            new TestCase("fuzzy_item_only", GameTestFunctions::fuzzyItemOnly),
            new TestCase("fluid_rejects_fuzzy", GameTestFunctions::fluidRejectsFuzzy),
            new TestCase("nested_cell_rejected", GameTestFunctions::nestedCellRejected),
            new TestCase("me_chest_accepts_cell", GameTestFunctions::meChestAcceptsCell),
            new TestCase("me_drive_compat", GameTestFunctions::meDriveCompat),
            new TestCase("portable_power", GameTestFunctions::portablePower),
            new TestCase("portable_open", GameTestFunctions::portableOpen),
            new TestCase("clone_from_container_menu", GameTestFunctions::cloneFromContainerMenu),
            new TestCase("recovery", GameTestFunctions::recovery)
    );

    private GameTestRegistrar() {
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper ->
                TESTS.forEach(test -> helper.register(
                        GameTestFunctions.key(test.path()),
                        test.function()
                ))
        );
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                Ae2Objects.id("default"),
                new TestEnvironmentDefinition.AllOf(List.of())
        );

        TESTS.forEach(test -> event.registerTest(
                Ae2Objects.id(test.path()),
                new FunctionGameTestInstance(
                        GameTestFunctions.key(test.path()),
                        new TestData<>(
                                environment,
                                Ae2Objects.id("deep_cell_plot"),
                                MAX_TICKS,
                                SETUP_TICKS,
                                true
                        )
                )
        ));
    }

}
