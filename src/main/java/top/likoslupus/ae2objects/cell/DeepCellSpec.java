package top.likoslupus.ae2objects.cell;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Runtime storage semantics shared by normal and portable deep cells.
 *
 * <p>The amount-per-byte ratio is deliberately part of the deep-cell specification instead of
 * inheriting AE2's basic-cell ratio. Deep cells use their own flat capacity model: one item per
 * byte, or 1000 native units per byte for fluids/chemicals. Optional integrations can construct a
 * spec in their isolated package and pass only AE2 abstractions plus a validator into the shared
 * implementation. This keeps foreign classes out of the core package.</p>
 */
public record DeepCellSpec(
        CellTier tier,
        AEKeyType keyType,
        long amountPerByte,
        boolean supportsFuzzy,
        Predicate<AEKey> contentValidator
) {

    private static final Predicate<AEKey> ACCEPT_ALL = _ -> true;
    private static final long FLUID_AMOUNT_PER_BYTE = 1_000L;

    public DeepCellSpec {
        requireNonNull(tier, "tier");
        requireNonNull(keyType, "keyType");
        requireNonNull(contentValidator, "contentValidator");
        if (amountPerByte <= 0) {
            throw new IllegalArgumentException("amountPerByte must be positive");
        }
    }

    public static DeepCellSpec items(CellTier tier) {
        return new DeepCellSpec(tier, AEKeyType.items(), 1L, true, ACCEPT_ALL);
    }

    public static DeepCellSpec fluids(CellTier tier) {
        return new DeepCellSpec(tier, AEKeyType.fluids(), FLUID_AMOUNT_PER_BYTE, false, ACCEPT_ALL);
    }

    public DeepCellCapacity capacity() {
        return new DeepCellCapacity(tier.bytes(), amountPerByte);
    }

    public boolean accepts(AEKey key) {
        return keyType.contains(key) && contentValidator.test(key);
    }

}
