package top.likoslupus.ae2objects.platform;

import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The set of optional integrations present in the current runtime.
 *
 * <p>Centralizes {@code ModList} lookups so detection is not scattered across items, inventory,
 * tooltips and datagen.</p>
 */
public record IntegrationSet(Set<IntegrationId> present) {

    public IntegrationSet {
        present = Set.copyOf(present);
    }

    public static IntegrationSet detect() {
        return new IntegrationSet(
                Arrays.stream(IntegrationId.values())
                        .filter(IntegrationSet::isLoaded)
                        .collect(Collectors.toCollection(() ->
                                EnumSet.noneOf(IntegrationId.class)
                        ))
        );
    }

    private static boolean isLoaded(IntegrationId integration) {
        var modList = ModList.get();
        return modList != null
                && modList.isLoaded(integration.modId());
    }

    public boolean megaCells() {
        return has(IntegrationId.MEGA_CELLS);
    }

    public boolean has(IntegrationId integration) {
        return present.contains(integration);
    }

    public boolean appliedMekanistics() {
        return has(IntegrationId.APPLIED_MEKANISTICS);
    }

}
