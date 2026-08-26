package top.likoslupus.ae2objects.registry;

import appeng.api.config.FuzzyMode;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.likoslupus.ae2objects.Ae2Objects;

import java.util.UUID;
import java.util.function.Supplier;

public final class Ae2ObjectsDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister
            .createDataComponents(Registries.DATA_COMPONENT_TYPE, Ae2Objects.MOD_ID);

    public static final Supplier<DataComponentType<UUID>> DISK_ID = COMPONENTS.registerComponentType(
            "disk_id",
            builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<Long>> DISK_ITEM_COUNT = COMPONENTS.registerComponentType(
            "disk_item_count",
            builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    public static final Supplier<DataComponentType<FuzzyMode>> FUZZY_MODE = COMPONENTS.registerComponentType(
            "fuzzy_mode",
            builder -> builder
                    .persistent(FuzzyMode.CODEC)
                    .networkSynchronized(FuzzyMode.STREAM_CODEC)
    );

    private Ae2ObjectsDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }

}
