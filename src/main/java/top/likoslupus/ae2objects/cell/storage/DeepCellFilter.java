package top.likoslupus.ae2objects.cell.storage;

import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.IPartitionList;

import org.jspecify.annotations.Nullable;

/**
 * Partition/fuzzy filtering for one deep-cell inventory.
 *
 * <p>Built from the cell's workbench state; the storage engine only asks
 * {@link #accepts(AEKey)}.</p>
 */
public final class DeepCellFilter {

    private final IPartitionList partitionList;
    private final IncludeExclude mode;
    private final boolean fuzzy;

    private DeepCellFilter(
            IPartitionList partitionList,
            IncludeExclude mode,
            boolean fuzzy
    ) {
        this.partitionList = partitionList;
        this.mode = mode;
        this.fuzzy = fuzzy;
    }

    public static DeepCellFilter create(
            ConfigInventory config,
            @Nullable IUpgradeInventory upgrades,
            FuzzyMode fuzzyMode,
            boolean supportsFuzzy
    ) {
        var builder = IPartitionList.builder();
        var hasInverter = upgrades != null
                && upgrades.isInstalled(AEItems.INVERTER_CARD);
        var fuzzy = supportsFuzzy
                && upgrades != null
                && upgrades.isInstalled(AEItems.FUZZY_CARD);

        if (fuzzy) {
            builder.fuzzyMode(fuzzyMode);
        }

        builder.addAll(config.keySet());
        var partitionList = builder.build();

        return new DeepCellFilter(
                partitionList,
                hasInverter
                        ? IncludeExclude.BLACKLIST
                        : IncludeExclude.WHITELIST,
                fuzzy && partitionList instanceof FuzzyPriorityList
        );
    }

    public boolean accepts(AEKey key) {
        return partitionList.matchesFilter(key, mode);
    }

    public IncludeExclude mode() {
        return mode;
    }

    public boolean isPreformatted() {
        return !partitionList.isEmpty();
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

}
