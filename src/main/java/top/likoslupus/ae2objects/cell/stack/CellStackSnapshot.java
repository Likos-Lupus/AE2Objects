package top.likoslupus.ae2objects.cell.stack;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.GenericStack;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Client-safe projection of a deep cell's synchronized item metadata.
 *
 * <p>It is not the authoritative contents; the server keeps those in the repository.</p>
 */
public record CellStackSnapshot(
        @Nullable UUID id,
        long storedAmount,
        int storedTypes,
        List<GenericStack> preview,
        FuzzyMode fuzzyMode
) {

    public CellStackSnapshot {
        preview = List.copyOf(preview);
    }

}
