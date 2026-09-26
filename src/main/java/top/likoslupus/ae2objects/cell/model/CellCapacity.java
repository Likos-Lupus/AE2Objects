package top.likoslupus.ae2objects.cell.model;

/**
 * Converts between a cell's byte capacity and the native amount used by a key type.
 */
public record CellCapacity(
        long bytes,
        long amountPerByte
) {

    public CellCapacity {
        if (bytes <= 0) {
            throw new IllegalArgumentException("bytes must be positive");
        }
        if (amountPerByte <= 0) {
            throw new IllegalArgumentException("amountPerByte must be positive");
        }
    }

    public long freeBytes(long storedAmount) {
        return Math.max(0L, bytes - usedBytes(storedAmount));
    }

    public long usedBytes(long storedAmount) {
        return Math.max(0L, storedAmount) / amountPerByte;
    }

    public boolean isFull(long storedAmount) {
        return remainingAmount(storedAmount) == 0;
    }

    public long remainingAmount(long storedAmount) {
        return Math.max(0L, totalAmount() - Math.max(0L, storedAmount));
    }

    public long totalAmount() {
        return Math.multiplyExact(bytes, amountPerByte);
    }

}
