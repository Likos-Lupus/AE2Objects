package top.likoslupus.ae2objects.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import top.likoslupus.ae2objects.cell.persistence.CellRepository;
import top.likoslupus.ae2objects.cell.persistence.SavedDataCellRepository;

import org.jspecify.annotations.Nullable;

/**
 * Server-scoped access to the deep-cell repository and registries.
 *
 * <p>The repository stays a pure persistence store; the registry lookup is only attached to the
 * context so that AE-key codecs can be resolved where needed.</p>
 */
public record ServerCellContext(
        CellRepository repository,
        HolderLookup.Provider registries
) {

    private static @Nullable ServerCellContext current;
    private static @Nullable MinecraftServer currentServer;

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
        current = new ServerCellContext(
                SavedDataCellRepository.getInstance(server),
                server.registryAccess()
        );
    }

    public static void onServerStopped(MinecraftServer server) {
        if (currentServer == server) {
            currentServer = null;
            current = null;
        }
    }

    public static @Nullable ServerCellContext getOrNull() {
        return current;
    }

}
