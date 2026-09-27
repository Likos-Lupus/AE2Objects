package top.likoslupus.ae2objects.cell.item;

import top.likoslupus.ae2objects.cell.model.CellDefinition;

/**
 * The only capability every deep-cell item must expose: which cell it is.
 *
 * <p>All other behaviour is provided by composition (workbench support, tooltip, services) so that
 * drive and portable items do not need to inherit a large shared interface.</p>
 */
public interface DeepCellDefinitionProvider {

    CellDefinition definition();

}
