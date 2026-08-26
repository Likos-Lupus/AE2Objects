package top.likoslupus.ae2objects.storage;

import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class DiskStorageAccess {

    private static @Nullable DiskStorageManager currentManager;
    private static @Nullable MinecraftServer currentServer;

    private DiskStorageAccess() {
    }

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
        currentManager = DiskStorageManager.getInstance(server);
    }

    public static void onServerStopped(MinecraftServer server) {
        if (currentServer == server) {
            currentServer = null;
            currentManager = null;
        }
    }

    public static Optional<DiskStorageManager> get() {
        return Optional.ofNullable(currentManager);
    }

    @Nullable
    public static DiskStorageManager getOrNull() {
        return currentManager;
    }

}
