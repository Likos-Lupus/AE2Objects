package top.likoslupus.ae2objects.cell.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The complete product catalog of deep cells.
 *
 * <p>This is a product-level description ({@code 3 types × 10 tiers × 2 forms = 60}) and is not
 * the set of items actually registered at runtime. Runtime activation is decided separately by the
 * content registration plan.</p>
 */
public final class DeepCellCatalog {

    public static final List<CellDefinition> DRIVE_CELLS = create(CellForm.DRIVE);
    public static final List<CellDefinition> PORTABLE_CELLS = create(CellForm.PORTABLE);
    public static final List<CellDefinition> ALL_CELLS = createAll();

    private DeepCellCatalog() {
    }

    private static List<CellDefinition> create(CellForm form) {
        var definitions = new ArrayList<CellDefinition>(
                CellContentType.values().length * CellTier.all().size()
        );
        Arrays.stream(CellContentType.values())
                .forEach(type ->
                        CellTier.all().stream()
                                .map(tier -> new CellDefinition(type, tier, form))
                                .forEach(definitions::add)
                );
        return List.copyOf(definitions);
    }

    private static List<CellDefinition> createAll() {
        var definitions = new ArrayList<CellDefinition>(DRIVE_CELLS.size() + PORTABLE_CELLS.size());
        definitions.addAll(DRIVE_CELLS);
        definitions.addAll(PORTABLE_CELLS);
        return List.copyOf(definitions);
    }

}
