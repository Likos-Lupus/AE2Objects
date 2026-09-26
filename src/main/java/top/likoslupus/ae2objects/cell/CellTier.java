package top.likoslupus.ae2objects.cell;

import java.util.List;

/**
 * Capacity and idle-drain values for every deep-cell tier described by the content specification.
 *
 * <p>The enum contains the planned MEGA tiers as well as the currently shipped AE2 tiers. Keeping
 * the numeric model independent from item registration lets integrations opt into new tiers without
 * changing the storage implementation.</p>
 */
public enum CellTier {

    K1("1k", 1_000, 0.5),
    K4("4k", 4_000, 1.0),
    K16("16k", 16_000, 1.5),
    K64("64k", 64_000, 2.0),
    K256("256k", 256_000, 2.5),
    M1("1m", 1_000_000, 3.0),
    M4("4m", 4_000_000, 3.5),
    M16("16m", 16_000_000, 4.0),
    M64("64m", 64_000_000, 4.5),
    M256("256m", 256_000_000, 5.0);

    private static final List<CellTier> AE2_TIERS = List.of(K1, K4, K16, K64, K256);
    private static final List<CellTier> MEGA_TIERS = List.of(M1, M4, M16, M64, M256);

    private final String id;
    private final int bytes;
    private final double idleDrain;

    CellTier(
            String id,
            int bytes,
            double idleDrain
    ) {
        this.id = id;
        this.bytes = bytes;
        this.idleDrain = idleDrain;
    }

    public static List<CellTier> ae2Tiers() {
        return AE2_TIERS;
    }

    public static List<CellTier> megaTiers() {
        return MEGA_TIERS;
    }

    public String id() {
        return id;
    }

    public int bytes() {
        return bytes;
    }

    public double idleDrain() {
        return idleDrain;
    }

}
