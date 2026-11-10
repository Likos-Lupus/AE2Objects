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
 *
 * <p>The current thread is checked explicitly. In a single-player game the client and the
 * integrated server share the same JVM, so an unguarded static context would allow the client
 * render/UI thread to read or write the server's {@code SavedData}.</p>
 */
public record ServerCellContext(
        CellRepository repository,
        HolderLookup.Provider registries
) {

    private static volatile @Nullable ServerCellContext current;
    private static volatile @Nullable MinecraftServer currentServer;
    private static volatile @Nullable Thread serverThread;

    public static void onServerAboutToStart(MinecraftServer server) {
        serverThread = server.getRunningThread();
    }

    public static void onServerStarted(MinecraftServer server) {
        serverThread = server.getRunningThread();
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

    /**
     * Returns the context only on the server thread. Client threads always get {@code null}, even
     * when an integrated server is running in the same JVM.
     */
    public static @Nullable ServerCellContext getForCurrentThread() {
        var context = current;
        return context != null && isServerThread()
                ? context
                : null;
    }

    public static boolean isServerThread() {
        var thread = serverThread;
        return thread != null && Thread.currentThread() == thread;
    }

    /** Raw access for server-only lifecycle code and tests. */
    public static @Nullable ServerCellContext getOrNull() {
        return current;
    }

}
