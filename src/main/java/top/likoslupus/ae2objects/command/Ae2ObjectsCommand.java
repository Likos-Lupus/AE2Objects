package top.likoslupus.ae2objects.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.CellTier;
import top.likoslupus.ae2objects.cell.DeepCellItem;
import top.likoslupus.ae2objects.cell.DeepCellStackData;
import top.likoslupus.ae2objects.cell.persistence.DeepCellStorage;
import top.likoslupus.ae2objects.cell.persistence.DeepCellStorageIo;
import top.likoslupus.ae2objects.cell.persistence.DeepStorageAccess;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;

import java.util.UUID;

public final class Ae2ObjectsCommand {

    private Ae2ObjectsCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal(Ae2Objects.MOD_ID)
                .executes(Ae2ObjectsCommand::help)
                .then(Commands.literal("recover")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> recover(
                                        context,
                                        context.getArgument("uuid", UUID.class)
                                ))
                        )
                )
                .then(Commands.literal("getuuid")
                        .executes(Ae2ObjectsCommand::getUuid)
                );

        event.getDispatcher().register(root);
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal("/ae2objects recover <UUID> - Recover a deep storage cell."),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("/ae2objects getuuid - Copy the held deep cell UUID."),
                false
        );
        return 1;
    }

    private static int recover(
            CommandContext<CommandSourceStack> context,
            UUID uuid
    ) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var manager = DeepStorageAccess.getOrNull();
        if (manager == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.recover_fail", uuid)
            );
            return 0;
        }

        var storage = manager.findCell(uuid).orElse(null);
        if (storage == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.recover_fail", uuid)
            );
            return 0;
        }

        var recoveredItem = resolveRecoveredItem(storage);
        var stack = new ItemStack(recoveredItem);
        var itemId = BuiltInRegistries.ITEM.getKey(recoveredItem).toString();
        var associatedStorage = storage.withCellItemIdIfMissing(itemId);
        if (associatedStorage != storage) {
            manager.updateCell(uuid, associatedStorage);
        }

        stack.set(Ae2ObjectsDataComponents.CELL_ID.get(), uuid);
        DeepCellStackData.updateSummary(
                stack,
                associatedStorage.storedAmount(),
                associatedStorage.storedTypesCount()
        );
        if (recoveredItem instanceof DeepCellItem deepCell) {
            var loaded = DeepCellStorageIo.load(
                    associatedStorage,
                    manager.registries(),
                    deepCell.cellSpec().keyType()
            );
            DeepCellStackData.updatePreview(
                    stack,
                    DeepCellStorageIo.createPreview(loaded.amounts())
            );
        }
        player.addItem(stack);

        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.ae2objects.recover_success",
                        player.getDisplayName(),
                        uuid
                ),
                true
        );
        return 1;
    }

    private static int getUuid(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        var stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (!(stack.getItem() instanceof DeepCellItem)) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.getuuid_fail_notcell")
            );
            return 0;
        }

        var cellId = DeepCellStackData.cellId(stack);
        if (cellId == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.getuuid_fail_nouuid")
            );
            return 0;
        }

        var text = copyToClipboard(cellId.toString());
        context.getSource().sendSuccess(
                () -> Component.translatable("command.ae2objects.getuuid_success", text),
                false
        );
        return 1;
    }

    private static Item resolveRecoveredItem(DeepCellStorage storage) {
        return storage.cellItemId()
                .map(Identifier::tryParse)
                .map(BuiltInRegistries.ITEM::getValue)
                .filter(item -> item instanceof DeepCellItem)
                .orElseGet(() -> Ae2ObjectsItems.itemStorageCell(CellTier.K256).item().get());
    }

    private static Component copyToClipboard(String value) {
        return Component.literal(value).withStyle(style -> style
                .withClickEvent(new ClickEvent.CopyToClipboard(value))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click")))
                .withInsertion(value)
                .withColor(ChatFormatting.GREEN));
    }

}
