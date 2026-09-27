package top.likoslupus.ae2objects.cell.model;

import net.minecraft.resources.Identifier;

import static java.util.Objects.requireNonNull;

/**
 * Stable identity of a concrete deep cell: storage type × tier × form.
 *
 * <p>All registry ids, drive-model ids and translation keys are derived here; no other code should
 * hand-write them.</p>
 */
public record CellDefinition(
        CellContentType type,
        CellTier tier,
        CellForm form
) {

    private static final String NAMESPACE = "ae2objects";

    public CellDefinition {
        requireNonNull(type, "type");
        requireNonNull(tier, "tier");
        requireNonNull(form, "form");
    }

    public boolean isDrive() {
        return form == CellForm.DRIVE;
    }

    public Identifier id() {
        return Identifier.fromNamespaceAndPath(NAMESPACE, itemId());
    }

    /**
     * Registry path of this cell, e.g. {@code deep_item_storage_cell_1k} or
     * {@code deep_portable_fluid_cell_256m}.
     */
    public String itemId() {
        return isPortable()
                ? "deep_portable_" + type.id() + "_cell_" + tier.id()
                : "deep_" + type.id() + "_storage_cell_" + tier.id();
    }

    public boolean isPortable() {
        return form == CellForm.PORTABLE;
    }

    /**
     * Byte capacity of this cell.
     *
     * <p>Portable cells store only half of their tier's bytes, mirroring vanilla AE2 portable
     * cells.</p>
     */
    public int storageBytes() {
        return isPortable() ? tier.bytes() / 2 : tier.bytes();
    }

    /**
     * Idle power drain in AE/t.
     *
     * <p>Drive cells scale with the tier; portable cells have a flat drain.</p>
     */
    public double idleDrain() {
        return isPortable() ? 1.0 : tier.idleDrain();
    }

    /**
     * Drive-model id for the block model used inside ME Drives. Only meaningful for
     * {@link CellForm#DRIVE}.
     */
    public Identifier driveModelId() {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "block/drive/cells/" + itemId());
    }

    /**
     * In-world (Drive / ME Chest) render model id.
     *
     * <p>Portable cells render as their non-portable sibling, mirroring AE2 where portable cells
     * share the normal cell's in-drive model.</p>
     */
    public Identifier inDriveModelId() {
        var siblingId = "deep_" + type.id() + "_storage_cell_" + tier.id();
        return Identifier.fromNamespaceAndPath(NAMESPACE, "block/drive/cells/" + siblingId);
    }

    public String translationKey() {
        return type.familyTranslationKey();
    }

}
