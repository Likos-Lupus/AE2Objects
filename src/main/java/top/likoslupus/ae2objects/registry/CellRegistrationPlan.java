package top.likoslupus.ae2objects.registry;

import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.cell.model.DeepCellCatalog;
import top.likoslupus.ae2objects.platform.IntegrationSet;

import java.util.List;

/**
 * Decides which catalog definitions are active in the current environment.
 *
 * <p>The catalog always describes the full product model; this plan selects the subset to
 * register. Fluid and chemical content is added by later phases.</p>
 */
public final class CellRegistrationPlan {

    private CellRegistrationPlan() {
    }

    public static List<CellDefinition> activeDriveCells(IntegrationSet integrations) {
        return DeepCellCatalog.DRIVE_CELLS.stream()
                .filter(definition -> isActive(definition, integrations))
                .toList();
    }

    private static boolean isActive(
            CellDefinition definition,
            IntegrationSet integrations
    ) {
        return definition.type() == CellContentType.ITEM
                && (CellTier.ae2Tiers().contains(definition.tier()) || integrations.megaCells());
    }

}
