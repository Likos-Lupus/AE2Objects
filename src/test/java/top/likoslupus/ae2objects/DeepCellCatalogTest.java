package top.likoslupus.ae2objects;

import org.junit.jupiter.api.Test;
import top.likoslupus.ae2objects.cell.model.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the product catalog ({@code 3 types × 10 tiers × 2 forms}) and the id / upgrade rules that
 * the new domain model is responsible for.
 */
class DeepCellCatalogTest {

    @Test
    void catalogContainsTheCompleteMatrix() {
        assertEquals(60, DeepCellCatalog.ALL_CELLS.size());
        assertEquals(30, DeepCellCatalog.DRIVE_CELLS.size());
        assertEquals(30, DeepCellCatalog.PORTABLE_CELLS.size());

        Arrays.stream(CellForm.values()).forEach(form ->
                Arrays.stream(CellContentType.values()).forEach(type -> {
                    var count = DeepCellCatalog.ALL_CELLS.stream()
                            .filter(definition ->
                                    definition.form() == form && definition.type() == type
                            )
                            .count();
                    assertEquals(
                            10,
                            count,
                            () -> type + "/" + form + " should have 10 tiers"
                    );
                })
        );
    }

    @Test
    void catalogIdsAreUnique() {
        var ids = new HashSet<String>();
        DeepCellCatalog.ALL_CELLS.forEach(definition -> assertTrue(
                ids.add(definition.itemId()),
                () -> "duplicate id: " + definition.itemId()
        ));
        assertEquals(60, ids.size());
    }

    @Test
    void idsFollowTheDocumentedScheme() {
        var itemDrive = new CellDefinition(CellContentType.ITEM, CellTier.K1, CellForm.DRIVE);
        assertEquals(
                "deep_item_storage_cell_1k",
                itemDrive.itemId()
        );
        assertEquals(
                "ae2objects",
                itemDrive.id().getNamespace()
        );
        assertEquals(
                "deep_item_storage_cell_1k",
                itemDrive.id().getPath()
        );
        assertEquals(
                "block/drive/cells/deep_item_storage_cell_1k",
                itemDrive.driveModelId().getPath()
        );
        assertEquals(
                "text.ae2objects.deep_item_storage_cells",
                itemDrive.translationKey()
        );

        var chemicalPortable = new CellDefinition(
                CellContentType.CHEMICAL,
                CellTier.M256,
                CellForm.PORTABLE
        );
        assertEquals(
                "deep_portable_chemical_cell_256m",
                chemicalPortable.itemId()
        );
        assertEquals(
                "text.ae2objects.deep_chemical_storage_cells",
                chemicalPortable.translationKey()
        );
    }

    @Test
    void upgradeProfilesMatchTheContentSpecification() {
        assertProfile(CellContentType.ITEM, CellForm.DRIVE, true, true, true, 0, 3);
        assertProfile(CellContentType.FLUID, CellForm.DRIVE, false, true, true, 0, 2);
        assertProfile(CellContentType.CHEMICAL, CellForm.DRIVE, false, true, true, 0, 2);
        assertProfile(CellContentType.ITEM, CellForm.PORTABLE, true, true, true, 4, 4);
        assertProfile(CellContentType.FLUID, CellForm.PORTABLE, false, true, true, 4, 3);
        assertProfile(CellContentType.CHEMICAL, CellForm.PORTABLE, false, true, true, 4, 3);
    }

    @Test
    void portableCellsStoreHalfTheTierBytesAndUseAFlatDrain() {
        var drive = new CellDefinition(CellContentType.ITEM, CellTier.K1, CellForm.DRIVE);
        assertEquals(1_000, drive.storageBytes());
        assertEquals(0.5, drive.idleDrain());

        var portable = new CellDefinition(CellContentType.ITEM, CellTier.K1, CellForm.PORTABLE);
        assertEquals(500, portable.storageBytes());
        assertEquals(1.0, portable.idleDrain());

        var portableFluidM256 = new CellDefinition(
                CellContentType.FLUID,
                CellTier.M256,
                CellForm.PORTABLE
        );
        assertEquals(128_000_000, portableFluidM256.storageBytes());
        assertEquals(1.0, portableFluidM256.idleDrain());
    }

    private static void assertProfile(
            CellContentType type,
            CellForm form,
            boolean fuzzy,
            boolean inverter,
            boolean voidOverflow,
            int energy,
            int slots
    ) {
        var definition = new CellDefinition(type, CellTier.K1, form);
        var profile = CellUpgradeProfile.forDefinition(definition);
        assertEquals(fuzzy, profile.fuzzy(), () -> type + "/" + form + " fuzzy");
        assertEquals(inverter, profile.inverter(), () -> type + "/" + form + " inverter");
        assertEquals(voidOverflow, profile.voidOverflow(), () -> type + "/" + form + " void");
        assertEquals(energy, profile.maxEnergyCards(), () -> type + "/" + form + " energy");
        assertEquals(slots, profile.totalSlots(), () -> type + "/" + form + " slots");
    }

}
