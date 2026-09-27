package top.likoslupus.ae2objects.registry;

import net.minecraft.resources.Identifier;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.platform.IntegrationId;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Component item behind each tier.
 *
 * <p>AE2 tiers use {@code ae2:cell_component_*}; MEGA tiers use
 * {@code megacells:cell_component_*}. No MEGA Java API is referenced.</p>
 */
public final class CellComponentSources {

    private static final Map<CellTier, CellComponentSource> SOURCES = create();

    private CellComponentSources() {
    }

    public static CellComponentSource forTier(CellTier tier) {
        var source = SOURCES.get(tier);
        if (source == null) {
            throw new IllegalArgumentException("No component source registered for tier " + tier);
        }
        return source;
    }

    private static Map<CellTier, CellComponentSource> create() {
        var sources = new EnumMap<CellTier, CellComponentSource>(CellTier.class);

        sources.put(CellTier.K1, ae2("cell_component_1k"));
        sources.put(CellTier.K4, ae2("cell_component_4k"));
        sources.put(CellTier.K16, ae2("cell_component_16k"));
        sources.put(CellTier.K64, ae2("cell_component_64k"));
        sources.put(CellTier.K256, ae2("cell_component_256k"));

        sources.put(CellTier.M1, mega("cell_component_1m"));
        sources.put(CellTier.M4, mega("cell_component_4m"));
        sources.put(CellTier.M16, mega("cell_component_16m"));
        sources.put(CellTier.M64, mega("cell_component_64m"));
        sources.put(CellTier.M256, mega("cell_component_256m"));

        return Map.copyOf(sources);
    }

    private static CellComponentSource ae2(String path) {
        return new CellComponentSource(
                Identifier.fromNamespaceAndPath("ae2", path),
                Optional.empty()
        );
    }

    private static CellComponentSource mega(String path) {
        return new CellComponentSource(
                Identifier.fromNamespaceAndPath("megacells", path),
                Optional.of(IntegrationId.MEGA_CELLS)
        );
    }

}
