package top.likoslupus.ae2objects.cell.stack;

import appeng.api.storage.cells.CellState;
import top.likoslupus.ae2objects.cell.model.CellCapacity;
import top.likoslupus.ae2objects.cell.model.CellDefinition;

/**
 * Pure projections from a definition plus a client snapshot. No repository, no inventory.
 */
public final class CellView {

    private CellView() {
    }

    public static CellState status(
            CellDefinition definition,
            CellStackSnapshot snapshot
    ) {
        var amount = snapshot.storedAmount();
        return amount <= 0
                ? CellState.EMPTY
                : capacity(definition).isFull(amount)
                        ? CellState.FULL
                        : CellState.NOT_EMPTY;
    }

    public static CellCapacity capacity(CellDefinition definition) {
        return new CellCapacity(
                definition.tier().bytes(),
                definition.type().amountPerByte()
        );
    }

}
