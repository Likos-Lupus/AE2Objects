package top.likoslupus.ae2objects;

import org.junit.jupiter.api.Test;
import top.likoslupus.ae2objects.cell.DeepCellCapacity;

import static org.junit.jupiter.api.Assertions.*;

class DeepCellCapacityTest {

    @Test
    void itemCapacityUsesOneNativeAmountPerByte() {
        var capacity = new DeepCellCapacity(1_000, 1);

        assertEquals(1_000L, capacity.totalAmount());
        assertEquals(250L, capacity.usedBytes(250));
        assertEquals(750L, capacity.remainingAmount(250));
        assertFalse(capacity.isFull(999));
        assertTrue(capacity.isFull(1_000));
    }

    @Test
    void fluidLikeCapacityUsesExplicitAmountPerByte() {
        var capacity = new DeepCellCapacity(1_000, 1_000);

        assertEquals(1_000_000L, capacity.totalAmount());
        assertEquals(1L, capacity.usedBytes(1_999));
        assertEquals(998_001L, capacity.remainingAmount(1_999));
        assertTrue(capacity.isFull(1_000_000));
    }

    @Test
    void largestDocumentedTierStaysWellInsideLong() {
        var capacity = new DeepCellCapacity(256_000_000, 1_000);
        assertEquals(256_000_000_000L, capacity.totalAmount());
    }

}
