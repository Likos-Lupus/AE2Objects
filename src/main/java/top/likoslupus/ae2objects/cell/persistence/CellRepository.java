package top.likoslupus.ae2objects.cell.persistence;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence store for deep-cell records, addressed by cell UUID.
 *
 * <p>Deliberately small: it must not depend on the server, registries, item stacks or AE keys.</p>
 */
public interface CellRepository {

    Optional<CellRecord> find(UUID id);

    boolean contains(UUID id);

    void put(UUID id, CellRecord record);

    void remove(UUID id);

}
