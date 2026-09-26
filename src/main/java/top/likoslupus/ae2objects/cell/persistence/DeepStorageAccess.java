package top.likoslupus.ae2objects.cell.persistence;

import net.minecraft.server.MinecraftServer;

import org.jspecify.annotations.Nullable;

/**
 * Narrow bridge from AE2's level-less cell-handler API to the current server's SavedData.
 */
public final class DeepStorageAccess {

    private static @Nullable DeepStorageManager currentManager;
    private static @Nullable MinecraftServer currentServer;

    private DeepStorageAccess() {
    }

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
        currentManager = DeepStorageManager.getInstance(server);
    }

    public static void onServerStopped(MinecraftServer server) {
        if (currentServer == server) {
            currentServer = null;
            currentManager = null;
        }
    }

    public static @Nullable DeepStorageManager getOrNull() {
        return currentManager;
    }

}
