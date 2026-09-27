package top.likoslupus.ae2objects.cell.model;

/**
 * Upgrade capability of a deep cell, derived from {@code type × form}.
 *
 * <p>This is the single source of truth for both the item's upgrade inventory and the AE2 upgrade
 * registration, so the two can never disagree.</p>
 */
public record CellUpgradeProfile(
        boolean fuzzy,
        boolean inverter,
        boolean voidOverflow,
        int maxEnergyCards,
        int totalSlots
) {

    public CellUpgradeProfile {
        if (maxEnergyCards < 0) {
            throw new IllegalArgumentException("maxEnergyCards must not be negative");
        }
        if (totalSlots < 0) {
            throw new IllegalArgumentException("totalSlots must not be negative");
        }
    }

    public static CellUpgradeProfile forDefinition(CellDefinition definition) {
        var type = definition.type();
        var fuzzy = type.supportsFuzzy();

        if (definition.isPortable()) {
            var slots = type == CellContentType.ITEM
                    ? 4
                    : 3;
            return new CellUpgradeProfile(fuzzy, true, true, 4, slots);
        }

        var slots = type == CellContentType.ITEM
                ? 3
                : 2;
        return new CellUpgradeProfile(fuzzy, true, true, 0, slots);
    }

}
