package top.likoslupus.ae2objects.registry;

import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.DeepCellCatalog;
import top.likoslupus.ae2objects.platform.IntegrationSet;

import java.util.List;

/**
 * Decides which catalog definitions are active in the current environment.
 *
 * <p>The catalog always describes the full product model; this plan selects the subset to
 * register. Item and fluid cells (including MEGA tiers) are always registered; only the MEGA
 * recipes are gated on the MEGA mod. Chemical content is activated by the Applied Mekanistics
 * integration.</p>
 */
public final class CellRegistrationPlan {

    private CellRegistrationPlan() {
    }

    public static List<CellDefinition> activeDriveCells(IntegrationSet integrations) {
        return DeepCellCatalog.DRIVE_CELLS.stream()
                .filter(CellRegistrationPlan::isActive)
                .toList();
    }

    private static boolean isActive(CellDefinition definition) {
        // Chemical cells exist only once the Applied Mekanistics chemical channel is registered.
        return definition.type() != CellContentType.CHEMICAL
                || StorageChannelRegistry.INSTANCE.isAvailable(CellContentType.CHEMICAL);
    }

    public static List<CellDefinition> activePortableCells(IntegrationSet integrations) {
        return DeepCellCatalog.PORTABLE_CELLS.stream()
                .filter(CellRegistrationPlan::isActive)
                .toList();
    }

}
