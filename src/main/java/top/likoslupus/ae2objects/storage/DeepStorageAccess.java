package top.likoslupus.ae2objects.storage;

import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

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

    public static Optional<DeepStorageManager> get() {
        return Optional.ofNullable(currentManager);
    }

    @Nullable
    public static DeepStorageManager getOrNull() {
        return currentManager;
    }

}
