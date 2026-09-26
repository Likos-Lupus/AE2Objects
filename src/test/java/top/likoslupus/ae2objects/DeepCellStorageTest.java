package top.likoslupus.ae2objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import top.likoslupus.ae2objects.cell.persistence.DeepCellStorage;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeepCellStorageTest {

    @Test
    void storageDefensivelyCopiesMutableNbtAndArrays() {
        var keys = new ListTag();
        var key = new CompoundTag();
        key.putString("id", "minecraft:iron_ingot");
        keys.add(key);
        var amounts = new long[]{100L};

        var storage = new DeepCellStorage(
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

        var storage = new DeepCellStorage(
                keys,
                new long[]{42L},
                42L,
                Optional.of("ae2objects:deep_item_storage_cell_4k")
        );
        var deserialized = DeepCellStorage.fromNbt(storage.toNbt());

        assertEquals(storage.storedAmount(), deserialized.storedAmount());
        assertArrayEquals(storage.stackAmounts(), deserialized.stackAmounts());
        assertEquals(storage.stackKeys().size(), deserialized.stackKeys().size());
        assertEquals(Optional.empty(), deserialized.cellItemId());
    }

    @Test
    void addingCellItemMetadataDoesNotMutateExistingRecord() {
        var storage = DeepCellStorage.empty();
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

}
