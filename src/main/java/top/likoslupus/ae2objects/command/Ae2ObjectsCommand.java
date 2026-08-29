package top.likoslupus.ae2objects.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;
import top.likoslupus.ae2objects.storage.DeepCellItem;
import top.likoslupus.ae2objects.storage.DeepStorageAccess;

import java.util.UUID;

public class Ae2ObjectsCommand {

    public static void register(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        var root = Commands.literal(Ae2Objects.MOD_ID)
                .executes(Ae2ObjectsCommand::help)
                .then(Commands.literal("recover")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> spawnCell(
                                        context,
                                        context.getArgument("uuid", UUID.class)
                                ))
                        )
                )
                .then(Commands.literal("getuuid")
                        .executes(Ae2ObjectsCommand::getUUID)
                );

        dispatcher.register(root);
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal("Available Argument(s): "),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "/ae2objects recover <UUID> - Spawns a storage cell with the given UUID, if it doesn't exist, does not spawn any item."
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "/ae2objects getuuid - Gets the UUID of the storage cell in the player's hand if it has a UUID."
                ),
                false
        );
        return 0;
    }

    private static int spawnCell(
            CommandContext<CommandSourceStack> context,
            UUID uuid
    ) throws CommandSyntaxException {
        var player = (Player) context.getSource().getPlayerOrException();
        var storageManager = DeepStorageAccess.getOrNull();

        if (storageManager != null && storageManager.hasUUID(uuid)) {
            var cellStorage = storageManager.getOrCreateCell(uuid);
            var stack = new ItemStack(Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_256K.get());
            stack.set(
                    Ae2ObjectsDataComponents.CELL_ID.get(),
                    uuid
            );
            stack.set(
                    Ae2ObjectsDataComponents.CELL_ITEM_COUNT.get(),
                    cellStorage.getItemCount()
            );
            stack.set(
                    Ae2ObjectsDataComponents.CELL_TYPE_COUNT.get(),
                    cellStorage.getStoredTypesCount()
            );

            player.addItem(stack);
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            "command.ae2objects.recover_success",
                            player.getDisplayName(),
                            uuid
                    ),
                    true
            );
            return 0;
        } else {
            context.getSource()
                    .sendFailure(Component.translatable("command.ae2objects.recover_fail", uuid));
            return 1;
        }
    }

    private static int getUUID(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        var player = (Player) context.getSource().getPlayerOrException();
        var mainStack = player.getMainHandItem();
        if (mainStack.getItem() instanceof DeepCellItem) {
            var cellId = mainStack.get(Ae2ObjectsDataComponents.CELL_ID.get());
            if (cellId != null) {
                var text = copyToClipboard(cellId.toString());
                context.getSource().sendSuccess(
                        () -> Component.translatable("command.ae2objects.getuuid_success", text),
                        false
                );
                return 0;
            } else {
                context.getSource()
                        .sendFailure(Component.translatable("command.ae2objects.getuuid_fail_nouuid"));
                return 1;
            }
        }
        context.getSource()
                .sendFailure(Component.translatable("command.ae2objects.getuuid_fail_notcell"));
        return 1;
    }

    private static Component copyToClipboard(String string) {
        return Component.literal(string).withStyle(style -> style
                .withClickEvent(new ClickEvent.CopyToClipboard(string))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click")))
                .withInsertion(string)
                .withColor(ChatFormatting.GREEN));
    }

}
