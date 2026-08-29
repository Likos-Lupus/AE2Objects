package top.likoslupus.ae2objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import top.likoslupus.ae2objects.storage.DeepCellStorage;

import static org.junit.jupiter.api.Assertions.*;

class DeepCellStorageTest {

    @Test
    void testCopyIsDeepAndIndependent() {
        var keys = new ListTag();
        var key1 = new CompoundTag();
        key1.putString(
                "id",
                "minecraft:iron_ingot"
        );
        keys.add(key1);

        var amounts = new long[]{100L};
        var itemCount = 100L;

        var original = new DeepCellStorage(
                keys,
                amounts,
                itemCount
        );
        var copy = original.copy();

        assertNotSame(
                original,
                copy
        );
        assertEquals(
                original.getItemCount(),
                copy.getItemCount()
        );
        assertArrayEquals(
                original.getStackAmounts(),
                copy.getStackAmounts()
        );
        assertEquals(
                original.getStackKeys().size(),
                copy.getStackKeys().size()
        );

        // Mutate original - copy must remain unaffected
        var newKeys = new ListTag();
        var key2 = new CompoundTag();
        key2.putString(
                "id",
                "minecraft:gold_ingot"
        );
        newKeys.add(key2);

        original.update(
                newKeys,
                new long[]{500L},
                500L
        );

        assertEquals(500L, original.getItemCount());
        assertEquals(100L, copy.getItemCount());
        assertEquals(500L, original.getStackAmounts()[0]);
        assertEquals(100L, copy.getStackAmounts()[0]);
    }

    @Test
    void testNbtSerializationRoundTrip() {
        var keys = new ListTag();
        var itemTag = new CompoundTag();
        itemTag.putString("id", "minecraft:diamond");
        keys.add(itemTag);

        var amounts = new long[]{42L};
        var itemCount = 42L;

        var storage = new DeepCellStorage(keys, amounts, itemCount);
        var nbt = storage.toNbt();

        var deserialized = DeepCellStorage.fromNbt(nbt);

        assertEquals(
                storage.getItemCount(),
                deserialized.getItemCount()
        );
        assertArrayEquals(
                storage.getStackAmounts(),
                deserialized.getStackAmounts()
        );
        assertEquals(
                storage.getStackKeys().size(),
                deserialized.getStackKeys().size()
        );
    }

}
