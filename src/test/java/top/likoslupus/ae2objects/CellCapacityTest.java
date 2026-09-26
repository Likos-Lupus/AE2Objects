package top.likoslupus.ae2objects;

import org.junit.jupiter.api.Test;
import top.likoslupus.ae2objects.cell.model.CellCapacity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for the deep-cell byte/native-amount math.
 *
 * <p>These lock the capacity rules documented in {@code docs/values.md}. They intentionally cover
 * the item (1 amount/byte) and fluid/chemical (1000 amount/byte) cases plus long-range limits.</p>
 */
class CellCapacityTest {

    @Test
    void itemCapacityUsesOneNativeAmountPerByte() {
        var capacity = new CellCapacity(1_000, 1);

        assertEquals(1_000L, capacity.totalAmount());
        assertEquals(0L, capacity.usedBytes(0));
        assertEquals(250L, capacity.usedBytes(250));
        assertEquals(750L, capacity.freeBytes(250));
        assertEquals(750L, capacity.remainingAmount(250));
        assertFalse(capacity.isFull(0));
        assertFalse(capacity.isFull(999));
        assertTrue(capacity.isFull(1_000));
    }

    @Test
    void fluidLikeCapacityUsesExplicitAmountPerByte() {
        var capacity = new CellCapacity(1_000, 1_000);

        assertEquals(1_000_000L, capacity.totalAmount());
        assertEquals(0L, capacity.usedBytes(999));
        assertEquals(1L, capacity.usedBytes(1_000));
        assertEquals(1L, capacity.usedBytes(1_999));
        assertEquals(2L, capacity.usedBytes(2_000));
        assertEquals(999L, capacity.freeBytes(1_999));
        assertEquals(998_001L, capacity.remainingAmount(1_999));
        assertFalse(capacity.isFull(999_999));
        assertTrue(capacity.isFull(1_000_000));
    }

    @Test
    void negativeAmountsClampToAnEmptyCell() {
        var capacity = new CellCapacity(4_000, 1_000);

        assertEquals(0L, capacity.usedBytes(-1));
        assertEquals(4_000L, capacity.freeBytes(-1));
        assertEquals(4_000_000L, capacity.remainingAmount(-1));
        assertFalse(capacity.isFull(-1));
    }

    @Test
    void constructorRejectsNonPositiveValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CellCapacity(0, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new CellCapacity(1, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new CellCapacity(1, -1)
        );
    }

    @Test
    void largestDocumentedTierStaysWellInsideLong() {
        var fluid = new CellCapacity(256_000_000, 1_000);
        assertEquals(256_000_000_000L, fluid.totalAmount());
        assertEquals(256_000_000L, fluid.freeBytes(0));

        var chemical = new CellCapacity(256_000_000, 1_000);
        assertEquals(256_000_000_000L, chemical.totalAmount());
    }

}
