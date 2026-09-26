package top.likoslupus.ae2objects.cell.model;

/**
 * The physical form of a deep cell.
 *
 * <p>Form only decides how the cell is used (drive/chest vs. pocket ME chest). Capacity, stored
 * contents, storage type and UUID are not part of the form.</p>
 */
public enum CellForm {

    DRIVE,
    PORTABLE;

    public boolean isPortable() {
        return this == PORTABLE;
    }

}
