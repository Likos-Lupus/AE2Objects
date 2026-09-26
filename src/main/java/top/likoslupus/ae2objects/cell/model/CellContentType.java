package top.likoslupus.ae2objects.cell.model;

/**
 * The storage type of a deep cell.
 *
 * <p>This is a product-level concept and deliberately does not reference AE2 or any foreign mod
 * API. The mapping to a concrete {@code AEKeyType} is resolved at runtime through a channel
 * binding.</p>
 */
public enum CellContentType {

    ITEM("item", 1L, true),
    FLUID("fluid", 1_000L, false),
    CHEMICAL("chemical", 1_000L, false);

    private final String id;
    private final long amountPerByte;
    private final boolean supportsFuzzy;

    CellContentType(
            String id,
            long amountPerByte,
            boolean supportsFuzzy
    ) {
        this.id = id;
        this.amountPerByte = amountPerByte;
        this.supportsFuzzy = supportsFuzzy;
    }

    public String id() {
        return id;
    }

    public long amountPerByte() {
        return amountPerByte;
    }

    public boolean supportsFuzzy() {
        return supportsFuzzy;
    }

    public String familyTranslationKey() {
        return "text.ae2objects.deep_" + id + "_storage_cells";
    }

}
