package top.likoslupus.ae2objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import top.likoslupus.ae2objects.cell.persistence.CellRecord;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for the persisted deep-cell record.
 *
 * <p>They lock the legacy NBT payload shape ({@code keys}/{@code amts}/{@code item_count}) and the
 * optional {@code cell_item} metadata.</p>
 */
class CellRecordTest {

    @Test
    void storageDefensivelyCopiesMutableNbtAndArrays() {
        var keys = new ListTag();
        var key = new CompoundTag();
        key.putString("id", "minecraft:iron_ingot");
        keys.add(key);
        var amounts = new long[]{100L};

        var storage = new CellRecord(
                keys,
                amounts,
                100L,
                Optional.of("ae2objects:deep_item_storage_cell_1k")
        );

        keys.clear();
        amounts[0] = 999L;

        assertEquals(1, storage.stackKeys().size());
        assertArrayEquals(new long[]{100L}, storage.stackAmounts());

        var returnedKeys = storage.stackKeys();
        var returnedAmounts = storage.stackAmounts();
        returnedKeys.clear();
        returnedAmounts[0] = 500L;

        assertEquals(1, storage.stackKeys().size());
        assertArrayEquals(new long[]{100L}, storage.stackAmounts());
    }

    @Test
    void nbtSerializationKeepsLegacyPayloadShape() {
        var keys = new ListTag();
        var itemTag = new CompoundTag();
        itemTag.putString("id", "minecraft:diamond");
        keys.add(itemTag);

        var storage = new CellRecord(
                keys,
                new long[]{42L},
                42L,
                Optional.of("ae2objects:deep_item_storage_cell_4k")
        );
        var deserialized = CellRecord.fromNbt(storage.toNbt());

        assertEquals(storage.storedAmount(), deserialized.storedAmount());
        assertArrayEquals(storage.stackAmounts(), deserialized.stackAmounts());
        assertEquals(storage.stackKeys().size(), deserialized.stackKeys().size());
        assertEquals(Optional.empty(), deserialized.cellItemId());
    }

    @Test
    void roundTripPreservesMultipleKeysAndAmounts() {
        var keys = new ListTag();
        var iron = new CompoundTag();
        iron.putString("id", "minecraft:iron_ingot");
        keys.add(iron);
        var gold = new CompoundTag();
        gold.putString("id", "minecraft:gold_ingot");
        keys.add(gold);

        var storage = new CellRecord(
                keys,
                new long[]{10L, 20L},
                30L,
                Optional.of("ae2objects:deep_item_storage_cell_16k")
        );
        var round = CellRecord.fromNbt(storage.toNbt());

        assertEquals(2, round.storedTypesCount());
        assertArrayEquals(new long[]{10L, 20L}, round.stackAmounts());
        assertEquals(30L, round.storedAmount());
        assertEquals(2, round.stackKeys().size());
    }

    @Test
    void emptyFactoriesProduceEmptyStorage() {
        var plain = CellRecord.empty();
        assertEquals(0, plain.storedTypesCount());
        assertEquals(0L, plain.storedAmount());
        assertEquals(Optional.empty(), plain.cellItemId());

        var identified = CellRecord.empty("ae2objects:deep_item_storage_cell_1k");
        assertEquals(
                0,
                identified.storedTypesCount()
        );
        assertEquals(
                Optional.of("ae2objects:deep_item_storage_cell_1k"),
                identified.cellItemId()
        );
    }

    @Test
    void addingCellItemMetadataDoesNotMutateExistingRecord() {
        var storage = CellRecord.empty();
        var associated = storage.withCellItemIdIfMissing("ae2objects:deep_item_storage_cell_256k");

        assertNotSame(storage, associated);
        assertEquals(
                Optional.empty(),
                storage.cellItemId()
        );
        assertEquals(
                Optional.of("ae2objects:deep_item_storage_cell_256k"),
                associated.cellItemId()
        );
    }

    @Test
    void withCellItemIdIsNoOpWhenAlreadyPresent() {
        var storage = CellRecord.empty("ae2objects:deep_item_storage_cell_1k");
        var same = storage.withCellItemIdIfMissing("ae2objects:deep_item_storage_cell_4k");

        assertSame(storage, same);
        assertEquals(
                Optional.of("ae2objects:deep_item_storage_cell_1k"),
                same.cellItemId()
        );
    }

}
