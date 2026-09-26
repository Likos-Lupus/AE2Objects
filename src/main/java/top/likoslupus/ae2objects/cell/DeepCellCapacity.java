package top.likoslupus.ae2objects.cell;

/**
 * Converts between a cell's byte capacity and the native amount used by an AE key type.
 */
public record DeepCellCapacity(
        int totalBytes,
        long amountPerByte
) {

    public DeepCellCapacity {
        if (totalBytes <= 0) {
            throw new IllegalArgumentException("totalBytes must be positive");
        }
        if (amountPerByte <= 0) {
            throw new IllegalArgumentException("amountPerByte must be positive");
        }
    }

    public long freeBytes(long storedAmount) {
        return Math.max(0L, (long) totalBytes - usedBytes(storedAmount));
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
        return Math.multiplyExact(totalBytes, amountPerByte);
    }

}
